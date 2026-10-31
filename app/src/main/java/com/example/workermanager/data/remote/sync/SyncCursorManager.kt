package com.example.workermanager.data.remote.sync

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages persistent synchronization cursors and bootstrap states per user and entity collection.
 * Uses Android SharedPreferences when running within the Android framework, with a thread-safe
 * concurrent memory fallback for unit testing in JVM environments.
 */
object SyncCursorManager {
    private const val PREFS_NAME = "workcraft_sync_cursors"
    private const val KEY_BOOTSTRAP_PREFIX = "bootstrapped_legacy_"
    const val DEFAULT_CLOCK_SKEW_BUFFER_MS = 5000L

    @Volatile
    private var sharedPrefs: SharedPreferences? = null

    // Fallback store for unit test environments where Android Context is not attached
    private val memoryFallback = ConcurrentHashMap<String, Long>()
    private val memoryBootstrap = ConcurrentHashMap<String, Boolean>()

    fun init(context: Context) {
        if (sharedPrefs == null) {
            synchronized(this) {
                if (sharedPrefs == null) {
                    sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    /**
     * Retrieves the last successful sync timestamp (high-water mark) for the given user and collection.
     * Returns 0L if no sync has occurred yet (triggering initial full paginated sync).
     */
    fun getLastSyncTimestamp(userId: String, collectionName: String): Long {
        if (userId.isBlank() || collectionName.isBlank()) return 0L
        val key = "${userId}_${collectionName}_cursor"
        val prefs = sharedPrefs
        return if (prefs != null) {
            prefs.getLong(key, 0L)
        } else {
            memoryFallback[key] ?: 0L
        }
    }

    /**
     * Updates the last sync timestamp for the given user and collection.
     */
    fun setLastSyncTimestamp(userId: String, collectionName: String, timestamp: Long) {
        if (userId.isBlank() || collectionName.isBlank()) return
        val key = "${userId}_${collectionName}_cursor"
        val prefs = sharedPrefs
        if (prefs != null) {
            prefs.edit().putLong(key, timestamp).apply()
        }
        memoryFallback[key] = timestamp
    }

    /**
     * Atomically updates the cursor only if the new timestamp is higher than the existing cursor.
     */
    fun updateCursorIfHigher(userId: String, collectionName: String, timestamp: Long) {
        if (userId.isBlank() || collectionName.isBlank() || timestamp <= 0L) return
        val current = getLastSyncTimestamp(userId, collectionName)
        if (timestamp > current) {
            setLastSyncTimestamp(userId, collectionName, timestamp)
        }
    }

    /**
     * Checks if the user's legacy records have already undergone the one-time bootstrap process.
     */
    fun hasBootstrappedLegacy(userId: String): Boolean {
        if (userId.isBlank()) return false
        val key = "$KEY_BOOTSTRAP_PREFIX$userId"
        val prefs = sharedPrefs
        return if (prefs != null) {
            prefs.getBoolean(key, false)
        } else {
            memoryBootstrap[key] ?: false
        }
    }

    /**
     * Records that the one-time legacy bootstrap process has completed for the given user.
     */
    fun setBootstrappedLegacy(userId: String, bootstrapped: Boolean) {
        if (userId.isBlank()) return
        val key = "$KEY_BOOTSTRAP_PREFIX$userId"
        val prefs = sharedPrefs
        if (prefs != null) {
            prefs.edit().putBoolean(key, bootstrapped).apply()
        }
        memoryBootstrap[key] = bootstrapped
    }

    /**
     * Clears all cursors and bootstrap state for a specific user (e.g. upon user data reset).
     */
    fun clearCursorsForUser(userId: String) {
        if (userId.isBlank()) return
        val prefs = sharedPrefs
        if (prefs != null) {
            val editor = prefs.edit()
            for (key in prefs.all.keys) {
                if (key.startsWith("${userId}_") || key == "$KEY_BOOTSTRAP_PREFIX$userId") {
                    editor.remove(key)
                }
            }
            editor.apply()
        }
        memoryFallback.keys.removeIf { it.startsWith("${userId}_") }
        memoryBootstrap.remove("$KEY_BOOTSTRAP_PREFIX$userId")
    }

    /**
     * Clears all stored cursor and bootstrap data (useful for test resets).
     */
    fun clearAll() {
        sharedPrefs?.edit()?.clear()?.apply()
        memoryFallback.clear()
        memoryBootstrap.clear()
    }
}

