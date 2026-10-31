package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.RecordRevisionManager
import com.example.workermanager.data.remote.sync.SyncCursorManager
import com.example.workermanager.data.remote.sync.dto.SiteDTO
import com.example.workermanager.data.remote.sync.mapper.SiteMapper
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

object SiteSyncEngine {
    private const val TAG = "SiteSyncEngine"

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

    suspend fun syncUserSites(siteDao: SiteDao): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = FirestoreProvider.requireAuthenticatedUid()
            val sitesRef = FirestoreProvider.getUserDocumentRef().collection("sites")
            val functions = Firebase.functions

            // 0. Ensure Quota Ledger Initialization (Idempotent server-side check for legacy users)
            try {
                functions.getHttpsCallable("initializeQuotaLedger").call().await()
            } catch (e: Exception) {
                logW(TAG, "Quota ledger initialization check in sync engine: ${e.message}")
            }

            // 1. Paginated Incremental Pull (Cloud -> Local)
            val pullResult = FirestoreSyncHelper.pullIncremental(
                collectionRef = sitesRef,
                userId = uid,
                collectionName = "sites"
            ) { doc ->
                SiteMapper.fromSnapshot(doc)
            }
            val cloudSites = pullResult.items

            // Process Cloud -> Local Pull with Inbound Sync Shield
            processInboundSites(cloudSites, siteDao)

            // 2. Local -> Cloud Outbound Push: Process all pending records
            val eligibleLocalSites = siteDao.getAllSitesRaw().filter { it.isPendingSync }
            val cloudSitesMap = cloudSites.associateBy { it.syncId }

            // Candidate cache populated on-demand if legacy records exist with blank syncId
            var fullCloudSitesCache: List<SiteDTO>? = null

            for (siteItem in eligibleLocalSites) {
                var localSite = siteItem
                var cloudDto: SiteDTO? = null

                if (localSite.syncId.isBlank() || localSite.syncOrigin != SyncOrigin.CLOUD_SYNCED) {
                    // CRITICAL: Check cloud identity before generating new remote record.
                    // If syncId is blank OR if it was created locally with a new UUID, attempt conservative identity match
                    // against cloud candidates to reuse existing cloud record rather than creating a duplicate!
                    var establishedCloudSite = resolveLegacySiteIdentity(localSite, cloudSites)

                    if (establishedCloudSite == null) {
                        val candidates = fullCloudSitesCache ?: run {
                            val fetched = try {
                                fetchAllSiteCandidatesPaginated(sitesRef)
                            } catch (e: Exception) {
                                logW(TAG, "Failed to fetch paginated cloud site candidates: ${e.message}")
                                emptyList()
                            }
                            fullCloudSitesCache = fetched
                            fetched
                        }
                        establishedCloudSite = resolveLegacySiteIdentity(localSite, candidates)
                    }

                    if (establishedCloudSite != null) {
                        val establishedSyncId = establishedCloudSite.syncId
                        localSite = localSite.copy(
                            syncId = establishedSyncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED
                        )
                        siteDao.updateSite(localSite)
                        RecordRevisionManager.recordLocalEdit(establishedSyncId)
                        cloudDto = establishedCloudSite
                        logD(TAG, "Reused stable cloud identity for site '${localSite.name}': $establishedSyncId")
                    } else if (localSite.syncId.isBlank()) {
                        logE(TAG, "Recoverable sync error: Cannot establish stable cloud identity for legacy site '${localSite.name}' (id=${localSite.id}) with blank syncId. Preserving local record in Room with isPendingSync = true.")
                        continue
                    }
                }

                if (cloudDto == null && localSite.syncId.isNotBlank()) {
                    cloudDto = cloudSitesMap[localSite.syncId]
                    if (cloudDto == null && localSite.syncOrigin != SyncOrigin.NEW_LOCAL) {
                        try {
                            val docSnap = sitesRef.document(localSite.syncId).get().await()
                            if (docSnap.exists()) {
                                cloudDto = SiteMapper.fromSnapshot(docSnap)
                            }
                        } catch (e: Exception) {
                            logW(TAG, "Could not fetch single site snapshot for ${localSite.syncId}: ${e.message}")
                        }
                    }
                }

                try {
                    val snapshotRevision = RecordRevisionManager.computeSiteRevision(localSite)
                    when (determinePushAction(localSite, cloudDto)) {
                        PushAction.PERMANENT_DELETE -> {
                            val payload = mapOf(
                                "entityType" to "site",
                                "syncId" to localSite.syncId
                            )
                            functions.getHttpsCallable("permanentDeleteEntityWithQuota").call(payload).await()
                            acknowledgeSitePermanentDelete(siteDao, localSite.syncId, snapshotRevision)
                            logD(TAG, "Permanently purged site tombstone after backend deletion: ${localSite.syncId}")
                        }
                        PushAction.SOFT_DELETE -> {
                            val payload = mapOf(
                                "entityType" to "site",
                                "syncId" to localSite.syncId
                            )
                            functions.getHttpsCallable("softDeleteEntityWithQuota").call(payload).await()
                            acknowledgeSiteSync(siteDao, localSite.syncId, snapshotRevision) {
                                it.copy(isPendingSync = false)
                            }
                            logD(TAG, "Soft-deleted site via backend quota service: ${localSite.name} (${localSite.syncId})")
                        }
                        PushAction.RESTORE -> {
                            val payload = mapOf(
                                "entityType" to "site",
                                "syncId" to localSite.syncId
                            )
                            functions.getHttpsCallable("restoreEntityWithQuota").call(payload).await()
                            acknowledgeSiteSync(siteDao, localSite.syncId, snapshotRevision) {
                                it.copy(isPendingSync = false)
                            }
                            logD(TAG, "Restored site via backend quota service: ${localSite.name} (${localSite.syncId})")
                        }
                        PushAction.CREATE_NEW -> {
                            val siteData = mapOf(
                                "syncId" to localSite.syncId,
                                "name" to localSite.name,
                                "address" to localSite.address,
                                "manager" to localSite.manager,
                                "startDate" to localSite.startDate.toString(),
                                "status" to localSite.status.name
                            )
                            val payload = mapOf("siteData" to siteData)
                            functions.getHttpsCallable("createSiteWithQuota").call(payload).await()
                            acknowledgeSiteSync(siteDao, localSite.syncId, snapshotRevision) {
                                it.copy(syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
                            }
                            logD(TAG, "Created new site via backend quota service: ${localSite.name} (${localSite.syncId})")
                        }
                        PushAction.UPDATE_METADATA -> {
                            val dto = SiteMapper.toDTO(localSite)
                            sitesRef.document(localSite.syncId).set(dto, SetOptions.merge()).await()
                            acknowledgeSiteSync(siteDao, localSite.syncId, snapshotRevision) {
                                it.copy(syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
                            }
                            logD(TAG, "Updated existing site metadata: ${localSite.name} (${localSite.syncId})")
                        }
                        PushAction.SKIP -> {
                            // Ineligible or not pending
                        }
                    }
                } catch (e: Exception) {
                    if (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED) {
                        logW(TAG, "Site sync quota exceeded on backend for ${localSite.name} (${localSite.syncId}): ${e.message}")
                        // Leave isPendingSync = true so record remains safe locally and syncs when upgraded or freed
                    } else {
                        logE(TAG, "Error pushing site ${localSite.syncId} to cloud", e)
                        throw e
                    }
                }
            }

            // 3. Save sync cursor high-water mark
            SyncCursorManager.setLastSyncTimestamp(uid, "sites", pullResult.highestUpdatedAt)

            Result.success(Unit)
        } catch (e: Exception) {
            logE(TAG, "SiteSyncEngine failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves an existing stable cloud identity for a legacy site with blank syncId.
     * Strengthened: Requires an unambiguous match using sufficiently strong attributes
     * (normalized name plus matching address, manager, or start date).
     * Name-only matching must NOT automatically establish identity.
     * Returns matching cloud site only when exactly one unambiguous candidate exists.
     */
    internal fun resolveLegacySiteIdentity(
        localSite: SiteEntity,
        cloudCandidates: List<SiteDTO>
    ): SiteDTO? {
        if (localSite.syncId.isNotBlank()) return null
        val matches = cloudCandidates.filter {
            it.syncId.isNotBlank() && LegacySyncBootstrapper.isSiteMatch(localSite, it)
        }
        return if (matches.size == 1) matches.first() else null
    }

    const val DEFAULT_CANDIDATE_PAGE_SIZE = 50L

    /**
     * Paginated retrieval of cloud site candidates using limit() and startAfter().
     * Eliminates unbounded reads by retrieving candidates in pages of size [pageSize].
     * Pages through the entire collection until exhausted.
     */
    internal suspend fun fetchAllSiteCandidatesPaginated(
        sitesRef: CollectionReference,
        pageSize: Long = DEFAULT_CANDIDATE_PAGE_SIZE
    ): List<SiteDTO> {
        val candidates = mutableListOf<SiteDTO>()
        var lastSnapshot: DocumentSnapshot? = null
        while (true) {
            var query: Query = sitesRef.limit(pageSize)
            if (lastSnapshot != null) {
                query = query.startAfter(lastSnapshot)
            }
            val pageSnapshot = query.get().await()
            if (pageSnapshot.isEmpty) {
                break
            }
            for (doc in pageSnapshot.documents) {
                val dto = SiteMapper.fromSnapshot(doc)
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
     * Resolves an existing stable cloud identity for a legacy site across paginated candidate pages.
     * Searches all pages from the candidate page provider.
     * CRITICAL: Continues pagination across all pages even after finding a match,
     * ensuring duplicate candidates across pages are detected and rejected.
     */
    internal inline fun resolveLegacySiteIdentityFromPages(
        localSite: SiteEntity,
        pageProvider: () -> Sequence<List<SiteDTO>>
    ): SiteDTO? {
        if (localSite.syncId.isNotBlank()) return null
        val localName = LegacySyncBootstrapper.normalizeName(localSite.name)
        if (localName.isBlank()) return null

        var singleMatch: SiteDTO? = null
        var matchCount = 0

        for (page in pageProvider()) {
            for (candidate in page) {
                if (candidate.syncId.isNotBlank() && LegacySyncBootstrapper.isSiteMatch(localSite, candidate)) {
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
     * Queries cloud for legacy site identity using paginated Firestore cursor reads.
     * Pages through the collection using limit() and startAfter().
     * If cachedCandidates is provided, it searches the cache directly without network round-trips.
     */
    internal suspend fun queryCloudForLegacySiteIdentity(
        sitesRef: CollectionReference,
        localSite: SiteEntity,
        pageSize: Long = DEFAULT_CANDIDATE_PAGE_SIZE,
        cachedCandidates: List<SiteDTO>? = null
    ): SiteDTO? {
        val localName = LegacySyncBootstrapper.normalizeName(localSite.name)
        if (localName.isBlank()) {
            return null
        }
        return try {
            val candidates = cachedCandidates ?: fetchAllSiteCandidatesPaginated(sitesRef, pageSize)
            resolveLegacySiteIdentity(localSite, candidates)
        } catch (e: Exception) {
            logW(TAG, "Failed to query cloud sites for legacy identity resolution of '${localSite.name}': ${e.message}")
            null
        }
    }

    /**
     * Determines which outbound operation to perform for an eligible site.
     * Guaranteed: An unidentified record with blank syncId returns SKIP so it is never pushed as CREATE_NEW.
     * Guaranteed: Records not yet in the cloud (cloudDto == null) route to CREATE_NEW to enforce quota.
     */
    internal fun determinePushAction(localSite: SiteEntity, cloudDto: SiteDTO?): PushAction {
        if (!localSite.isPendingSync || localSite.syncId.isBlank()) {
            return PushAction.SKIP
        }
        return when {
            localSite.deletedAt == -1L -> PushAction.PERMANENT_DELETE
            localSite.isDeleted -> PushAction.SOFT_DELETE
            cloudDto != null && cloudDto.isDeleted && !localSite.isDeleted -> PushAction.RESTORE
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
    internal suspend fun processInboundSites(
        cloudSites: List<SiteDTO>,
        siteDao: SiteDao
    ) {
        val allLocalSites = siteDao.getAllSitesRaw()
        val localSitesMap = allLocalSites.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }.toMutableMap()

        for (cloudDto in cloudSites) {
            if (cloudDto.syncId.isBlank()) continue
            var existingLocal = localSitesMap[cloudDto.syncId] ?: siteDao.getSiteBySyncId(cloudDto.syncId)

            // Reinstall / Pre-existing un-synced local site matching guard:
            // If not found by syncId, check if an un-synced local record matches conservative identity
            if (existingLocal == null) {
                val matchedLocal = allLocalSites.find {
                    (it.syncOrigin != SyncOrigin.CLOUD_SYNCED || it.syncId.isBlank()) &&
                    LegacySyncBootstrapper.isSiteMatch(it, cloudDto)
                }
                if (matchedLocal != null) {
                    val updated = matchedLocal.copy(
                        syncId = cloudDto.syncId,
                        syncOrigin = SyncOrigin.CLOUD_SYNCED,
                        isPendingSync = false
                    )
                    siteDao.updateSite(updated)
                    localSitesMap[cloudDto.syncId] = updated
                    logD(TAG, "Merged un-synced local site '${matchedLocal.name}' with incoming cloud site: ${cloudDto.syncId}")
                    existingLocal = updated
                }
            }

            if (existingLocal == null) {
                if (cloudDto.isDeleted) continue
                val entity = SiteMapper.toEntity(cloudDto).copy(isPendingSync = false)
                val id = siteDao.upsertSiteBySyncId(entity)
                localSitesMap[cloudDto.syncId] = entity.copy(id = id)
                logD(TAG, "Pulled new site from cloud: ${cloudDto.name} (${cloudDto.syncId})")
            } else if (existingLocal.isPendingSync) {
                // INBOUND SHIELD: Local user has un-synced offline edits.
                // Preserve local user-modified fields, local ID, and isPendingSync = true.
                logD(TAG, "Inbound sync skipped for pending local site: ${existingLocal.syncId}")
            } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                // Cloud record is newer -> Update local Room
                val updatedEntity = SiteMapper.toEntity(cloudDto, localId = existingLocal.id).copy(isPendingSync = false)
                siteDao.updateSite(updatedEntity)
                localSitesMap[cloudDto.syncId] = updatedEntity
                logD(TAG, "Updated local site from cloud: ${cloudDto.name} (${cloudDto.syncId})")
            }
        }
    }

    /**
     * Safely acknowledges a successful upload only if no concurrent local edit occurred in Room
     * while the upload network operation was in-flight, verified via deterministic revision hash and sequence.
     */
    internal suspend fun acknowledgeSiteSync(
        siteDao: SiteDao,
        syncId: String,
        expectedRevision: String,
        transform: (SiteEntity) -> SiteEntity
    ): Boolean {
        val current = siteDao.getSiteBySyncId(syncId) ?: return false
        val currentRevision = RecordRevisionManager.computeSiteRevision(current)
        return if (currentRevision == expectedRevision) {
            siteDao.updateSite(transform(current))
            true
        } else {
            logD(TAG, "Concurrent local edit detected during upload for site $syncId (revision mismatch); retaining isPendingSync = true")
            false
        }
    }

    internal suspend fun acknowledgeSitePermanentDelete(
        siteDao: SiteDao,
        syncId: String,
        expectedRevision: String
    ): Boolean {
        val current = siteDao.getSiteBySyncId(syncId) ?: return false
        val currentRevision = RecordRevisionManager.computeSiteRevision(current)
        return if (currentRevision == expectedRevision) {
            siteDao.deleteBySyncId(syncId)
            true
        } else {
            logD(TAG, "Concurrent local edit detected during tombstone purge for site $syncId (revision mismatch); retaining record")
            false
        }
    }
}
