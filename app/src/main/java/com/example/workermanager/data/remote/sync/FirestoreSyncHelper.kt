package com.example.workermanager.data.remote.sync

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Reusable helper providing paginated, cursor-based incremental reads from Cloud Firestore.
 * Eliminates redundant full collection downloads by querying only documents modified since
 * the high-water mark timestamp, with a safety buffer to compensate for clock skew or
 * simultaneous in-flight writes.
 */
object FirestoreSyncHelper {
    private const val TAG = "FirestoreSyncHelper"
    const val DEFAULT_PAGE_SIZE = 100L
    const val DEFAULT_CLOCK_SKEW_BUFFER_MS = 5000L

    data class IncrementalPullResult<T>(
        val items: List<T>,
        val highestUpdatedAt: Long,
        val totalPagesRead: Int,
        val isInitialSync: Boolean
    )

    /**
     * Parses a timestamp value into epoch milliseconds.
     * Supports Number (Long, Int, Double), Firestore Timestamp, Java Date, and numeric String.
     * Returns null if missing or invalid.
     */
    fun parseTimestampValue(raw: Any?): Long? {
        return when (raw) {
            is Number -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            is java.util.Date -> raw.time
            is String -> raw.trim().toLongOrNull()
            else -> null
        }
    }

    /**
     * Extracts an epoch-millisecond timestamp from a DocumentSnapshot field.
     * Handles both Long numbers and Firestore Timestamp objects, preserving compatibility
     * with existing documents across Cloud Functions and Android write paths.
     * Returns null if the field is absent or invalid (never silently advances the cursor).
     */
    fun extractTimestampMillis(doc: DocumentSnapshot, field: String = "updatedAt"): Long? {
        if (!doc.contains(field)) return null
        val raw = doc.get(field) ?: return null
        return parseTimestampValue(raw)
    }

    /**
     * Executes a paginated incremental pull against the given Firestore collection.
     *
     * @param collectionRef The Firestore collection reference to query.
     * @param userId The authenticated user ID owning the data.
     * @param collectionName The entity collection name (e.g. "sites", "workers").
     * @param pageSize Maximum documents to fetch per page.
     * @param clockSkewBufferMs Safety overlap in ms applied to the cursor to prevent missing records.
     * @param transform Function converting a DocumentSnapshot into the typed DTO.
     */
    suspend fun <T> pullIncremental(
        collectionRef: CollectionReference,
        userId: String,
        collectionName: String,
        pageSize: Long = DEFAULT_PAGE_SIZE,
        clockSkewBufferMs: Long = DEFAULT_CLOCK_SKEW_BUFFER_MS,
        transform: (DocumentSnapshot) -> T?
    ): IncrementalPullResult<T> {
        val lastSyncTimestamp = SyncCursorManager.getLastSyncTimestamp(userId, collectionName)
        val isInitialSync = lastSyncTimestamp <= 0L
        val queryTimestamp = if (!isInitialSync) maxOf(0L, lastSyncTimestamp - clockSkewBufferMs) else 0L

        val results = mutableListOf<T>()
        var highestUpdatedAt = lastSyncTimestamp
        var lastSnapshot: DocumentSnapshot? = null
        var pageCount = 0

        Log.d(TAG, "Starting ${if (isInitialSync) "initial" else "incremental"} pull for '$collectionName' (user=$userId, lastSync=$lastSyncTimestamp, queryFrom=$queryTimestamp)")

        while (true) {
            var query: Query = if (!isInitialSync) {
                collectionRef.whereGreaterThan("updatedAt", queryTimestamp)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .limit(pageSize)
            } else {
                // Initial sync: fetch in pages using document ordering
                collectionRef.limit(pageSize)
            }

            if (lastSnapshot != null) {
                query = query.startAfter(lastSnapshot)
            }

            val pageSnapshot = query.get().await()
            pageCount++

            if (pageSnapshot.isEmpty) {
                break
            }

            for (doc in pageSnapshot.documents) {
                val item = transform(doc)
                if (item != null) {
                    results.add(item)
                }
                val docUpdatedAt = extractTimestampMillis(doc, "updatedAt")
                if (docUpdatedAt != null && docUpdatedAt > highestUpdatedAt) {
                    highestUpdatedAt = docUpdatedAt
                }
            }

            if (pageSnapshot.size() < pageSize) {
                break
            }
            lastSnapshot = pageSnapshot.documents.last()
        }

        // Ensure a valid non-zero cursor is established only after an empty initial sync
        if (isInitialSync && results.isEmpty() && highestUpdatedAt <= 0L) {
            highestUpdatedAt = System.currentTimeMillis()
        }

        Log.d(TAG, "Completed pull for '$collectionName': retrieved ${results.size} items across $pageCount page(s). High-water mark=$highestUpdatedAt")

        return IncrementalPullResult(
            items = results,
            highestUpdatedAt = highestUpdatedAt,
            totalPagesRead = pageCount,
            isInitialSync = isInitialSync
        )
    }
}

