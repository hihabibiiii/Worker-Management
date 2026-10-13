package com.example.workermanager.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.example.workermanager.data.database.AppDatabase
import com.example.workermanager.data.entity.*
import com.example.workermanager.data.preference.AutoBackupPreferenceManager
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

data class BackupPayload(
    var version: Int = 1,
    var appName: String = "WorkCraft",
    var timestamp: Long = System.currentTimeMillis(),
    var workers: List<WorkerEntity> = emptyList(),
    var sites: List<SiteEntity> = emptyList(),
    var attendance: List<AttendanceEntity> = emptyList(),
    var payments: List<PaymentEntity> = emptyList(),
    var contracts: List<ContractEntity> = emptyList(),
    var clientPayments: List<ClientPaymentEntity> = emptyList(),
    var workerSiteHistory: List<WorkerSiteHistoryEntity> = emptyList(),
    var siteAttendance: List<SiteAttendanceEntity> = emptyList()
)

object BackupManager {

    private val backupMutex = Mutex()

    private fun getObjectMapper(): ObjectMapper {
        return jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }

    suspend fun createBackupPayload(database: AppDatabase): BackupPayload = withContext(Dispatchers.IO) {
        BackupPayload(
            version = 1,
            appName = "WorkCraft",
            timestamp = System.currentTimeMillis(),
            workers = database.workerDao().getAllWorkersRaw(),
            sites = database.siteDao().getAllSitesRaw(),
            attendance = database.attendanceDao().getAllAttendanceRaw(),
            payments = database.paymentDao().getAllPaymentsRaw(),
            contracts = database.contractDao().getAllContractsRaw(),
            clientPayments = database.clientPaymentDao().getAllClientPaymentsRaw(),
            workerSiteHistory = database.workerSiteHistoryDao().getAllHistory(),
            siteAttendance = database.siteAttendanceDao().getAllSiteAttendance()
        )
    }

    suspend fun exportBackup(database: AppDatabase, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val payload = createBackupPayload(database)
        val json = getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(payload)
        outputStream.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    suspend fun validateAndParseBackup(inputStream: InputStream): BackupPayload = withContext(Dispatchers.IO) {
        val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val mapper = getObjectMapper()
        val payload = mapper.readValue(json, BackupPayload::class.java)
        
        if (payload.appName != "WorkCraft" || payload.version <= 0 || payload.version > 1) {
            throw IllegalArgumentException("Incompatible or invalid WorkCraft backup file.")
        }
        payload
    }

    suspend fun createSafetyBackupToStream(database: AppDatabase, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        exportBackup(database, outputStream)
    }

    suspend fun createSafetyBackupInternal(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        val payload = createBackupPayload(database)
        val json = getObjectMapper().writeValueAsString(payload)
        val safetyFile = File(context.filesDir, "safety_backup_temp.json")
        safetyFile.writeText(json, Charsets.UTF_8)
    }

    suspend fun performAutoBackup(
        context: Context,
        database: AppDatabase,
        prefManager: AutoBackupPreferenceManager
    ): Boolean = backupMutex.withLock {
        withContext(Dispatchers.IO) {
            var lastError: String? = null

            try {
                val payload = createBackupPayload(database)
                val json = getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(payload)

                // Attempt 1: User-selected SAF Tree URI
                val uriString = prefManager.backupUri.value
                if (!uriString.isNullOrBlank()) {
                    try {
                        val treeUri = Uri.parse(uriString)
                        val treeDir = DocumentFile.fromTreeUri(context, treeUri)
                        if (treeDir != null && treeDir.exists() && treeDir.canWrite()) {
                            val autoFile = treeDir.findFile("WorkCraft_AutoBackup.json")
                            val prevFile = treeDir.findFile("WorkCraft_PreviousBackup.json")

                            // Rotate existing auto backup content to previous backup file
                            if (autoFile != null && autoFile.exists()) {
                                try {
                                    val existingJson = context.contentResolver.openInputStream(autoFile.uri)?.use {
                                        it.bufferedReader(Charsets.UTF_8).readText()
                                    }
                                    if (!existingJson.isNullOrBlank()) {
                                        val targetPrev = prevFile ?: treeDir.createFile("application/json", "WorkCraft_PreviousBackup.json")
                                        targetPrev?.uri?.let { prevUri ->
                                            context.contentResolver.openOutputStream(prevUri, "wt")?.use { stream ->
                                                stream.write(existingJson.toByteArray(Charsets.UTF_8))
                                                stream.flush()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("WorkCraftAutoBackup", "Failed SAF rotate to previous backup", e)
                                }
                            }

                            val targetAuto = autoFile ?: treeDir.createFile("application/json", "WorkCraft_AutoBackup.json")
                            if (targetAuto != null) {
                                context.contentResolver.openOutputStream(targetAuto.uri, "wt")?.use { stream ->
                                    stream.write(json.toByteArray(Charsets.UTF_8))
                                    stream.flush()
                                }
                                cleanupObsoleteAutoBackups(context, treeDir)
                                prefManager.updateBackupStatus(System.currentTimeMillis(), "Success")
                                return@withContext true
                            }
                        }
                    } catch (e: Exception) {
                        lastError = "SAF write: ${e.localizedMessage ?: "Permission denied"}"
                        android.util.Log.w("WorkCraftAutoBackup", "SAF backup attempt failed", e)
                    }
                }

                // Attempt 2: MediaStore API for public Documents/WorkCraftBackups
                try {
                    val written = writeBackupViaMediaStore(context, "WorkCraft_AutoBackup.json", json)
                    if (written) {
                        cleanupObsoleteAutoBackups(context, null)
                        prefManager.updateBackupStatus(System.currentTimeMillis(), "Success")
                        return@withContext true
                    }
                } catch (e: Exception) {
                    lastError = "MediaStore: ${e.localizedMessage ?: "Write error"}"
                    android.util.Log.w("WorkCraftAutoBackup", "MediaStore backup attempt failed", e)
                }

                // Attempt 3: Always-writable App External Shared Documents Directory
                try {
                    val externalDocDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "WorkCraftBackups")
                    if (!externalDocDir.exists()) {
                        externalDocDir.mkdirs()
                    }

                    val autoFile = File(externalDocDir, "WorkCraft_AutoBackup.json")
                    val prevFile = File(externalDocDir, "WorkCraft_PreviousBackup.json")

                    if (autoFile.exists()) {
                        try {
                            autoFile.copyTo(prevFile, overwrite = true)
                        } catch (e: Exception) {
                            android.util.Log.w("WorkCraftAutoBackup", "File rotation copy failed", e)
                        }
                    }

                    val tempFile = File(externalDocDir, "WorkCraft_AutoBackup_temp.json")
                    tempFile.writeText(json, Charsets.UTF_8)
                    if (tempFile.exists() && tempFile.length() > 0) {
                        if (autoFile.exists()) {
                            autoFile.delete()
                        }
                        tempFile.renameTo(autoFile)
                        cleanupObsoleteAutoBackups(context, null)
                        prefManager.updateBackupStatus(System.currentTimeMillis(), "Success")
                        return@withContext true
                    }
                } catch (e: Exception) {
                    lastError = "App external dir: ${e.localizedMessage ?: "Write error"}"
                    android.util.Log.w("WorkCraftAutoBackup", "App external dir backup failed", e)
                }

                prefManager.updateBackupStatus(System.currentTimeMillis(), "Backup Failed: ${lastError ?: "Storage access restricted"}")
                false
            } catch (e: Exception) {
                android.util.Log.e("WorkCraftAutoBackup", "performAutoBackup unexpected error", e)
                prefManager.updateBackupStatus(System.currentTimeMillis(), "Backup Failed: ${e.localizedMessage ?: "Unexpected error"}")
                false
            }
        }
    }

    private fun cleanupObsoleteAutoBackups(context: Context, treeDir: DocumentFile?) {
        try {
            if (treeDir != null && treeDir.exists()) {
                treeDir.listFiles().forEach { file ->
                    val name = file.name ?: ""
                    if ((name.startsWith("WorkCraft_AutoBackup_") || name.startsWith("WorkCraft_PreviousBackup_")) &&
                        name != "WorkCraft_AutoBackup.json" && name != "WorkCraft_PreviousBackup.json"
                    ) {
                        try { file.delete() } catch (e: Exception) { /* ignore */ }
                    }
                }
            }

            listOf(
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "WorkCraftBackups"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "WorkCraftBackups")
            ).forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        val name = file.name
                        if ((name.startsWith("WorkCraft_AutoBackup_") || name.startsWith("WorkCraft_PreviousBackup_")) &&
                            name != "WorkCraft_AutoBackup.json" && name != "WorkCraft_PreviousBackup.json"
                        ) {
                            try { file.delete() } catch (e: Exception) { /* ignore */ }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("WorkCraftAutoBackup", "cleanupObsoleteAutoBackups error", e)
        }
    }

    private fun writeBackupViaMediaStore(context: Context, fileName: String, json: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val existingUri = findBackupViaMediaStore(context, fileName)

            // Rotate existing auto backup to previous backup
            if (fileName == "WorkCraft_AutoBackup.json" && existingUri != null) {
                try {
                    val existingJson = resolver.openInputStream(existingUri)?.use {
                        it.bufferedReader(Charsets.UTF_8).readText()
                    }
                    if (!existingJson.isNullOrBlank()) {
                        writeBackupViaMediaStore(context, "WorkCraft_PreviousBackup.json", existingJson)
                    }
                } catch (ex: Exception) {
                    android.util.Log.w("WorkCraftAutoBackup", "Failed to rotate previous backup in MediaStore", ex)
                }
            }

            val targetUri = if (existingUri != null) {
                existingUri
            } else {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/WorkCraftBackups")
                }
                resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            }

            if (targetUri != null) {
                resolver.openOutputStream(targetUri, "wt")?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("WorkCraftAutoBackup", "writeBackupViaMediaStore error for $fileName", e)
            false
        }
    }

    suspend fun findExistingAutoBackup(context: Context, prefManager: AutoBackupPreferenceManager): BackupPayload? = withContext(Dispatchers.IO) {
        val candidateFileNames = listOf("WorkCraft_AutoBackup.json", "WorkCraft_PreviousBackup.json")

        for (fileName in candidateFileNames) {
            try {
                // 1. Try SAF persisted URI if configured
                val uriString = prefManager.backupUri.value
                if (!uriString.isNullOrBlank()) {
                    try {
                        val treeUri = Uri.parse(uriString)
                        val treeDir = DocumentFile.fromTreeUri(context, treeUri)
                        val autoFile = treeDir?.findFile(fileName)
                        if (autoFile != null && autoFile.exists()) {
                            context.contentResolver.openInputStream(autoFile.uri)?.use { stream ->
                                return@withContext validateAndParseBackup(stream)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("WorkCraftAutoBackup", "SAF read auto backup failed for $fileName", e)
                    }
                }

                // 2. Query MediaStore API for Documents/WorkCraftBackups/ (Android 10+)
                val mediaStoreUri = findBackupViaMediaStore(context, fileName)
                if (mediaStoreUri != null) {
                    try {
                        context.contentResolver.openInputStream(mediaStoreUri)?.use { stream ->
                            return@withContext validateAndParseBackup(stream)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("WorkCraftAutoBackup", "MediaStore read auto backup failed for $fileName", e)
                    }
                }

                // 3. Try App External Shared Documents Directory
                val externalDocDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "WorkCraftBackups")
                val autoFile = File(externalDocDir, fileName)
                if (autoFile.exists()) {
                    try {
                        autoFile.inputStream().use { stream ->
                            return@withContext validateAndParseBackup(stream)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("WorkCraftAutoBackup", "External dir read auto backup failed for $fileName", e)
                    }
                }

                // 4. Try Public Documents File path
                val docDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "WorkCraftBackups")
                val publicAutoFile = File(docDir, fileName)
                if (publicAutoFile.exists()) {
                    try {
                        publicAutoFile.inputStream().use { stream ->
                            return@withContext validateAndParseBackup(stream)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("WorkCraftAutoBackup", "Public dir read auto backup failed for $fileName", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkCraftAutoBackup", "Error reading $fileName", e)
            }
        }
        null
    }

    private fun findBackupViaMediaStore(context: Context, fileName: String = "WorkCraft_AutoBackup.json"): Uri? {
        return try {
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf("%Documents/WorkCraftBackups%", fileName)
            val queryUri = MediaStore.Files.getContentUri("external")

            context.contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val id = cursor.getLong(idIdx)
                    return ContentUris.withAppendedId(queryUri, id)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun restoreBackup(database: AppDatabase, payload: BackupPayload) = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.clearAllTables()

            // 1. Insert Parent Sites first
            payload.sites.forEach { database.siteDao().insertSite(it) }

            // 2. Insert Workers & Contracts (may reference Sites)
            payload.workers.forEach { database.workerDao().insertWorker(it) }
            payload.contracts.forEach { database.contractDao().insertContract(it) }

            // 3. Insert Dependent Child Entities
            payload.attendance.forEach { database.attendanceDao().insertAttendance(it) }
            payload.payments.forEach { database.paymentDao().insertPayment(it) }
            payload.clientPayments.forEach { database.clientPaymentDao().insertPayment(it) }
            payload.workerSiteHistory.forEach { database.workerSiteHistoryDao().insert(it) }
            payload.siteAttendance.forEach { database.siteAttendanceDao().insertSiteAttendance(it) }
        }
    }
}
