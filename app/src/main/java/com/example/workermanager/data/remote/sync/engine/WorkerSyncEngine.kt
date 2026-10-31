package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.dao.WorkerDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.RecordRevisionManager
import com.example.workermanager.data.remote.sync.SyncCursorManager
import com.example.workermanager.data.remote.sync.dto.WorkerDTO
import com.example.workermanager.data.remote.sync.mapper.WorkerMapper
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object WorkerSyncEngine {
    private const val TAG = "WorkerSyncEngine"

    enum class PushAction {
        PERMANENT_DELETE,
        SOFT_DELETE,
        RESTORE,
        CREATE_NEW,
        UPDATE_METADATA,
        SKIP
    }

    private fun logD(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
            // Handled for JVM unit tests where android.util.Log is unmocked
        }
    }

    private fun logW(tag: String, message: String) {
        try {
            Log.w(tag, message)
        } catch (_: Throwable) {
        }
    }

    private fun logE(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        } catch (_: Throwable) {
        }
    }

    suspend fun syncUserWorkers(workerDao: WorkerDao, siteDao: SiteDao): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = FirestoreProvider.requireAuthenticatedUid()
            val workersRef = FirestoreProvider.getUserDocumentRef().collection("workers")
            val functions = Firebase.functions

            // 1. Paginated Incremental Pull (Cloud -> Local)
            val pullResult = FirestoreSyncHelper.pullIncremental(
                collectionRef = workersRef,
                userId = uid,
                collectionName = "workers"
            ) { doc ->
                WorkerMapper.fromSnapshot(doc)
            }
            val cloudWorkers = pullResult.items

            // Process Cloud -> Local Pull with Inbound Sync Shield
            processInboundWorkers(cloudWorkers, workerDao, siteDao)

            // 2. Outbound Push (Local -> Cloud): Process all pending records regardless of syncOrigin
            val eligibleWorkers = workerDao.getAllWorkersRaw().filter { it.isPendingSync }

            val cloudWorkersMap = cloudWorkers.associateBy { it.syncId }

            // Candidate cache populated on-demand if legacy records exist with blank syncId
            var fullCloudWorkersCache: List<WorkerDTO>? = null

            for (workerItem in eligibleWorkers) {
                var localWorker = workerItem
                var cloudDto: WorkerDTO? = null

                if (localWorker.syncId.isBlank() || localWorker.syncOrigin != SyncOrigin.CLOUD_SYNCED) {
                    // CRITICAL: Check cloud identity before generating new remote record.
                    // If syncId is blank OR if it was created locally with a new UUID, attempt conservative identity match
                    // against cloud candidates to reuse existing cloud record rather than creating a duplicate!
                    var establishedCloudWorker = resolveLegacyWorkerIdentity(localWorker, cloudWorkers)

                    if (establishedCloudWorker == null) {
                        val candidates = fullCloudWorkersCache ?: run {
                            val fetched = try {
                                fetchAllWorkerCandidatesPaginated(workersRef)
                            } catch (e: Exception) {
                                logW(TAG, "Failed to fetch paginated cloud worker candidates: ${e.message}")
                                emptyList()
                            }
                            fullCloudWorkersCache = fetched
                            fetched
                        }
                        establishedCloudWorker = resolveLegacyWorkerIdentity(localWorker, candidates)
                    }

                    if (establishedCloudWorker != null) {
                        val establishedSyncId = establishedCloudWorker.syncId
                        localWorker = localWorker.copy(
                            syncId = establishedSyncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED
                        )
                        workerDao.updateWorker(localWorker)
                        RecordRevisionManager.recordLocalEdit(establishedSyncId)
                        cloudDto = establishedCloudWorker
                        logD(TAG, "Reused stable cloud identity for worker '${localWorker.name}': $establishedSyncId")
                    } else if (localWorker.syncId.isBlank()) {
                        logE(TAG, "Recoverable sync error: Cannot establish stable cloud identity for legacy worker '${localWorker.name}' (id=${localWorker.id}) with blank syncId. Preserving local record in Room with isPendingSync = true.")
                        continue
                    }
                }

                if (cloudDto == null && localWorker.syncId.isNotBlank()) {
                    cloudDto = cloudWorkersMap[localWorker.syncId]
                    if (cloudDto == null && localWorker.syncOrigin != SyncOrigin.NEW_LOCAL) {
                        try {
                            val docSnap = workersRef.document(localWorker.syncId).get().await()
                            if (docSnap.exists()) {
                                cloudDto = WorkerMapper.fromSnapshot(docSnap)
                            }
                        } catch (e: Exception) {
                            logW(TAG, "Could not fetch single worker snapshot for ${localWorker.syncId}: ${e.message}")
                        }
                    }
                }

                try {
                    // Capture exact revision being uploaded (content hash + local sequence)
                    val snapshotRevision = RecordRevisionManager.computeWorkerRevision(localWorker)
                    when (determinePushAction(localWorker, cloudDto)) {
                        PushAction.PERMANENT_DELETE -> {
                            val payload = mapOf(
                                "entityType" to "worker",
                                "syncId" to localWorker.syncId
                            )
                            functions.getHttpsCallable("permanentDeleteEntityWithQuota").call(payload).await()
                            acknowledgeWorkerPermanentDelete(workerDao, localWorker.syncId, snapshotRevision)
                            logD(TAG, "Permanently purged worker tombstone after backend deletion: ${localWorker.syncId}")
                        }
                        PushAction.SOFT_DELETE -> {
                            val payload = mapOf(
                                "entityType" to "worker",
                                "syncId" to localWorker.syncId
                            )
                            functions.getHttpsCallable("softDeleteEntityWithQuota").call(payload).await()
                            acknowledgeWorkerSync(workerDao, localWorker.syncId, snapshotRevision) {
                                it.copy(isPendingSync = false)
                            }
                            logD(TAG, "Soft-deleted worker via backend quota service: ${localWorker.name} (${localWorker.syncId})")
                        }
                        PushAction.RESTORE -> {
                            val payload = mapOf(
                                "entityType" to "worker",
                                "syncId" to localWorker.syncId
                            )
                            functions.getHttpsCallable("restoreEntityWithQuota").call(payload).await()
                            acknowledgeWorkerSync(workerDao, localWorker.syncId, snapshotRevision) {
                                it.copy(isPendingSync = false)
                            }
                            logD(TAG, "Restored worker via backend quota service: ${localWorker.name} (${localWorker.syncId})")
                        }
                        PushAction.CREATE_NEW -> {
                            val siteSyncId = if (localWorker.currentSiteId != null) {
                                siteDao.getAllSitesRaw().find { it.id == localWorker.currentSiteId }?.syncId
                            } else null

                            val workerData = mapOf(
                                "syncId" to localWorker.syncId,
                                "name" to localWorker.name,
                                "phone" to localWorker.phone,
                                "workerType" to localWorker.workerType.name,
                                "dailyRate" to localWorker.dailyRate,
                                "overtimeRate" to localWorker.overtimeRate,
                                "joiningDate" to localWorker.joiningDate.toString(),
                                "siteSyncId" to siteSyncId,
                                "isActive" to localWorker.isActive,
                                "notes" to localWorker.notes,
                                "photoPath" to localWorker.photoPath
                            )
                            val payload = mapOf("workerData" to workerData)
                            functions.getHttpsCallable("createWorkerWithQuota").call(payload).await()
                            acknowledgeWorkerSync(workerDao, localWorker.syncId, snapshotRevision) {
                                it.copy(syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
                            }
                            logD(TAG, "Created new worker via backend quota service: ${localWorker.name} (${localWorker.syncId})")
                        }
                        PushAction.UPDATE_METADATA -> {
                            val siteSyncId = if (localWorker.currentSiteId != null) {
                                siteDao.getAllSitesRaw().find { it.id == localWorker.currentSiteId }?.syncId
                            } else null

                            val dto = WorkerMapper.toDTO(localWorker, siteSyncId = siteSyncId)
                            workersRef.document(localWorker.syncId).set(dto, SetOptions.merge()).await()
                            acknowledgeWorkerSync(workerDao, localWorker.syncId, snapshotRevision) {
                                it.copy(syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
                            }
                            logD(TAG, "Updated existing worker metadata: ${localWorker.name} (${localWorker.syncId})")
                        }
                        PushAction.SKIP -> {
                            // Ineligible or not pending
                        }
                    }
                } catch (e: Exception) {
                    if (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED) {
                        logW(TAG, "Worker sync quota exceeded on backend for ${localWorker.name} (${localWorker.syncId}): ${e.message}")
                        // Leave isPendingSync = true so record remains safe locally and syncs when upgraded or freed
                    } else {
                        logE(TAG, "Error pushing worker ${localWorker.syncId} to cloud", e)
                        throw e
                    }
                }
            }

            // 3. Save sync cursor high-water mark
            SyncCursorManager.setLastSyncTimestamp(uid, "workers", pullResult.highestUpdatedAt)

            Result.success(Unit)
        } catch (e: Exception) {
            logE(TAG, "Error syncing user workers", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves an existing stable cloud identity for a legacy worker with blank syncId.
     * Normalized phone number may be used as a matching attribute only when it identifies exactly one cloud candidate.
     * Worker type alone must NEVER establish identity. If phone number is missing or multiple candidates match, do not guess.
     * Returns matching cloud worker only when exactly one unambiguous candidate exists.
     */
    internal fun resolveLegacyWorkerIdentity(
        localWorker: WorkerEntity,
        cloudCandidates: List<WorkerDTO>
    ): WorkerDTO? {
        if (localWorker.syncId.isNotBlank()) return null
        val localPhone = LegacySyncBootstrapper.normalizePhone(localWorker.phone)
        if (localPhone.isBlank()) {
            return null
        }
        val matches = cloudCandidates.filter {
            it.syncId.isNotBlank() && LegacySyncBootstrapper.isWorkerMatch(localWorker, it)
        }
        return if (matches.size == 1) matches.first() else null
    }

    const val DEFAULT_CANDIDATE_PAGE_SIZE = 50L

    /**
     * Paginated retrieval of cloud worker candidates using limit() and startAfter().
     * Eliminates unbounded reads by retrieving candidates in pages of size [pageSize].
     * Pages through the entire collection until exhausted.
     */
    internal suspend fun fetchAllWorkerCandidatesPaginated(
        workersRef: CollectionReference,
        pageSize: Long = DEFAULT_CANDIDATE_PAGE_SIZE
    ): List<WorkerDTO> {
        val candidates = mutableListOf<WorkerDTO>()
        var lastSnapshot: DocumentSnapshot? = null
        while (true) {
            var query: Query = workersRef.limit(pageSize)
            if (lastSnapshot != null) {
                query = query.startAfter(lastSnapshot)
            }
            val pageSnapshot = query.get().await()
            if (pageSnapshot.isEmpty) {
                break
            }
            for (doc in pageSnapshot.documents) {
                val dto = WorkerMapper.fromSnapshot(doc)
                if (dto != null) {
                    candidates.add(dto)
                }
            }
            if (pageSnapshot.size() < pageSize) {
                break
            }
            lastSnapshot = pageSnapshot.documents.last()
        }
        return candidates
    }

    /**
     * Resolves an existing stable cloud identity for a legacy worker across paginated candidate pages.
     * Searches all pages from the candidate page provider.
     * CRITICAL: Continues pagination across all pages even after finding a match,
     * ensuring duplicate candidates across pages are detected and rejected.
     */
    internal inline fun resolveLegacyWorkerIdentityFromPages(
        localWorker: WorkerEntity,
        pageProvider: () -> Sequence<List<WorkerDTO>>
    ): WorkerDTO? {
        if (localWorker.syncId.isNotBlank()) return null
        val localPhone = LegacySyncBootstrapper.normalizePhone(localWorker.phone)
        val localName = LegacySyncBootstrapper.normalizeName(localWorker.name)
        if (localPhone.isBlank() || localName.isBlank()) {
            return null
        }

        var singleMatch: WorkerDTO? = null
        var matchCount = 0

        for (page in pageProvider()) {
            for (candidate in page) {
                if (candidate.syncId.isNotBlank() && LegacySyncBootstrapper.isWorkerMatch(localWorker, candidate)) {
                    matchCount++
                    if (matchCount == 1) {
                        singleMatch = candidate
                    }
                    // NEVER stop or break early upon finding a match; continue pagination to detect duplicates
                }
            }
        }

        return if (matchCount == 1) singleMatch else null
    }

    /**
     * Queries cloud for legacy worker identity using paginated Firestore cursor reads.
     * Pages through the collection using limit() and startAfter().
     * If cachedCandidates is provided, it searches the cache directly without network round-trips.
     */
    internal suspend fun queryCloudForLegacyWorkerIdentity(
        workersRef: CollectionReference,
        localWorker: WorkerEntity,
        pageSize: Long = DEFAULT_CANDIDATE_PAGE_SIZE,
        cachedCandidates: List<WorkerDTO>? = null
    ): WorkerDTO? {
        val localPhone = LegacySyncBootstrapper.normalizePhone(localWorker.phone)
        val localName = LegacySyncBootstrapper.normalizeName(localWorker.name)
        if (localPhone.isBlank() || localName.isBlank()) {
            return null
        }
        return try {
            val candidates = cachedCandidates ?: fetchAllWorkerCandidatesPaginated(workersRef, pageSize)
            resolveLegacyWorkerIdentity(localWorker, candidates)
        } catch (e: Exception) {
            logW(TAG, "Failed to query cloud workers for legacy identity resolution of '${localWorker.name}': ${e.message}")
            null
        }
    }

    /**
     * Determines which outbound operation to perform for an eligible worker.
     * Guaranteed: An unidentified record with blank syncId returns SKIP so it is never pushed as CREATE_NEW.
     * Guaranteed: Any valid pending record is evaluated without restriction by syncOrigin.
     * Guaranteed: Records not yet in the cloud (cloudDto == null) route to CREATE_NEW to enforce quota.
     */
    internal fun determinePushAction(localWorker: WorkerEntity, cloudDto: WorkerDTO?): PushAction {
        if (!localWorker.isPendingSync || localWorker.syncId.isBlank()) {
            return PushAction.SKIP
        }
        return when {
            localWorker.deletedAt == -1L -> PushAction.PERMANENT_DELETE
            localWorker.isDeleted -> PushAction.SOFT_DELETE
            cloudDto != null && cloudDto.isDeleted && !localWorker.isDeleted -> PushAction.RESTORE
            cloudDto == null -> PushAction.CREATE_NEW
            else -> PushAction.UPDATE_METADATA
        }
    }

    /**
     * Inbound sync pull processing:
     * - Inserts new records from cloud if not present locally.
     * - Shields local pending edits: if existingLocal.isPendingSync == true, preserves local fields and does NOT overwrite.
     * - If not pending and cloudDto.updatedAt > existingLocal.updatedAt, updates local Room.
     */
    internal suspend fun processInboundWorkers(
        cloudWorkers: List<WorkerDTO>,
        workerDao: WorkerDao,
        siteDao: SiteDao
    ) {
        val allLocalWorkers = workerDao.getAllWorkersRaw()
        val localWorkersMap = allLocalWorkers.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }.toMutableMap()

        for (cloudDto in cloudWorkers) {
            if (cloudDto.syncId.isBlank()) continue
            var existingLocal = localWorkersMap[cloudDto.syncId] ?: workerDao.getWorkerBySyncId(cloudDto.syncId)

            val resolvedSiteId = if (!cloudDto.siteSyncId.isNullOrBlank()) {
                siteDao.getSiteBySyncId(cloudDto.siteSyncId!!)?.id
            } else null

            // Reinstall / Pre-existing un-synced local worker matching guard:
            // If not found by syncId, check if an un-synced local worker matches conservative identity (name + phone)
            if (existingLocal == null) {
                val matchedLocal = allLocalWorkers.find {
                    (it.syncOrigin != SyncOrigin.CLOUD_SYNCED || it.syncId.isBlank()) &&
                    LegacySyncBootstrapper.isWorkerMatch(it, cloudDto)
                }
                if (matchedLocal != null) {
                    val updated = matchedLocal.copy(
                        syncId = cloudDto.syncId,
                        syncOrigin = SyncOrigin.CLOUD_SYNCED,
                        isPendingSync = false
                    )
                    workerDao.updateWorker(updated)
                    localWorkersMap[cloudDto.syncId] = updated
                    logD(TAG, "Merged un-synced local worker '${matchedLocal.name}' with incoming cloud worker: ${cloudDto.syncId}")
                    existingLocal = updated
                }
            }

            if (existingLocal == null) {
                if (cloudDto.isDeleted) continue
                val entity = WorkerMapper.toEntity(cloudDto, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                val id = workerDao.upsertWorkerBySyncId(entity)
                localWorkersMap[cloudDto.syncId] = entity.copy(id = id)
                logD(TAG, "Pulled new worker from cloud: ${cloudDto.name} (${cloudDto.syncId})")
            } else if (existingLocal.isPendingSync) {
                // INBOUND SHIELD: Local user has un-synced offline edits.
                // Preserve local user-modified fields, local ID, and isPendingSync = true.
                logD(TAG, "Inbound sync skipped for pending local worker: ${existingLocal.syncId}")
            } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                val updatedEntity = WorkerMapper.toEntity(cloudDto, localId = existingLocal.id, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                workerDao.updateWorker(updatedEntity)
                localWorkersMap[cloudDto.syncId] = updatedEntity
                logD(TAG, "Updated local worker from cloud: ${cloudDto.name} (${cloudDto.syncId})")
            }
        }
    }

    /**
     * Safely acknowledges a successful upload only if no concurrent local edit occurred in Room
     * while the upload network operation was in-flight, verified via deterministic revision hash and sequence.
     */
    internal suspend fun acknowledgeWorkerSync(
        workerDao: WorkerDao,
        syncId: String,
        expectedRevision: String,
        transform: (WorkerEntity) -> WorkerEntity
    ): Boolean {
        val current = workerDao.getWorkerBySyncId(syncId) ?: return false
        val currentRevision = RecordRevisionManager.computeWorkerRevision(current)
        return if (currentRevision == expectedRevision) {
            workerDao.updateWorker(transform(current))
            true
        } else {
            logD(TAG, "Concurrent local edit detected during upload for worker $syncId (revision mismatch); retaining isPendingSync = true")
            false
        }
    }

    internal suspend fun acknowledgeWorkerPermanentDelete(
        workerDao: WorkerDao,
        syncId: String,
        expectedRevision: String
    ): Boolean {
        val current = workerDao.getWorkerBySyncId(syncId) ?: return false
        val currentRevision = RecordRevisionManager.computeWorkerRevision(current)
        return if (currentRevision == expectedRevision) {
            workerDao.deleteBySyncId(syncId)
            true
        } else {
            logD(TAG, "Concurrent local edit detected during tombstone purge for worker $syncId (revision mismatch); retaining record")
            false
        }
    }
}
