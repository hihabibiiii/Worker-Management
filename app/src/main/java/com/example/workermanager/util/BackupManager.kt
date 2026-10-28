package com.example.workermanager.util

import android.content.ContentUris
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

data class BackupPayload(
    var version: Int = 2,
    var appName: String = "WorkCraft",
    var timestamp: Long = System.currentTimeMillis(),
    var ownerUid: String? = null,
    var workers: List<WorkerEntity> = emptyList(),
    var sites: List<SiteEntity> = emptyList(),
    var attendance: List<AttendanceEntity> = emptyList(),
    var payments: List<PaymentEntity> = emptyList(),
    var contracts: List<ContractEntity> = emptyList(),
    var clientPayments: List<ClientPaymentEntity> = emptyList(),
    var workerSiteHistory: List<WorkerSiteHistoryEntity> = emptyList(),
    var siteAttendance: List<SiteAttendanceEntity> = emptyList(),
    var siteExpenses: List<SiteExpenseEntity> = emptyList(),
    var siteExpensePayments: List<SiteExpensePaymentEntity> = emptyList(),
    var materials: List<MaterialEntity> = emptyList(),
    var materialTransactions: List<MaterialTransactionEntity> = emptyList(),
    var contractorMaterials: List<ContractorMaterialEntity> = emptyList(),
    var contractorMaterialTransactions: List<ContractorMaterialTransactionEntity> = emptyList()
)

sealed class OwnershipValidationResult {
    object Valid : OwnershipValidationResult()
    data class Invalid(val userFacingErrorMessage: String) : OwnershipValidationResult()
}

object BackupManager {

    private val backupMutex = Mutex()

    private fun getObjectMapper(): ObjectMapper {
        return jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }

    fun validateBackupOwnership(payloadOwnerUid: String?, currentFirebaseUid: String?): OwnershipValidationResult {
        if (currentFirebaseUid.isNullOrBlank()) {
            return OwnershipValidationResult.Invalid("No authenticated account found. Please sign in to restore backups.")
        }
        if (payloadOwnerUid.isNullOrBlank()) {
            return OwnershipValidationResult.Invalid("This backup file does not contain owner account verification and cannot be restored safely.")
        }
        if (payloadOwnerUid != currentFirebaseUid) {
            return OwnershipValidationResult.Invalid("This backup belongs to another account. Please sign in with the original account to restore it.")
        }
        return OwnershipValidationResult.Valid
    }

    fun validateBackupOwnership(payload: BackupPayload): OwnershipValidationResult {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        return validateBackupOwnership(payload.ownerUid, currentUid)
    }

    suspend fun createBackupPayload(database: AppDatabase, ownerUid: String? = null): BackupPayload = withContext(Dispatchers.IO) {
        val currentUid = ownerUid ?: FirebaseAuth.getInstance().currentUser?.uid
        BackupPayload(
            version = 2,
            appName = "WorkCraft",
            timestamp = System.currentTimeMillis(),
            ownerUid = currentUid,
            workers = database.workerDao().getAllWorkersRaw(),
            sites = database.siteDao().getAllSitesRaw(),
            attendance = database.attendanceDao().getAllAttendanceRaw(),
            payments = database.paymentDao().getAllPaymentsRaw(),
            contracts = database.contractDao().getAllContractsRaw(),
            clientPayments = database.clientPaymentDao().getAllClientPaymentsRaw(),
            workerSiteHistory = database.workerSiteHistoryDao().getAllHistory(),
            siteAttendance = database.siteAttendanceDao().getAllSiteAttendance(),
            siteExpenses = database.siteExpenseDao().getAllExpensesRaw(),
            siteExpensePayments = database.siteExpensePaymentDao().getAllPaymentsRaw(),
            materials = database.materialDao().getAllMaterialsRaw(),
            materialTransactions = database.materialTransactionDao().getAllTransactionsRaw(),
            contractorMaterials = database.contractorMaterialDao().getAllMaterialsRaw(),
            contractorMaterialTransactions = database.contractorMaterialTransactionDao().getAllTransactionsRaw()
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
        
        if (payload.appName != "WorkCraft" || payload.version <= 0 || payload.version > 2) {
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
                                prefManager.updateBackupStatus(System.currentTimeMillis(), "Success (Saved to Document Folder)")
                                return@withContext true
                            }
                        }
                    } catch (e: Exception) {
                        lastError = "SAF auto-backup error: ${e.localizedMessage}"
                        android.util.Log.w("WorkCraftAutoBackup", lastError, e)
                    }
                }

                // Attempt 2: Internal Storage Fallback
                try {
                    val internalAutoFile = File(context.filesDir, "WorkCraft_AutoBackup.json")
                    val internalPrevFile = File(context.filesDir, "WorkCraft_PreviousBackup.json")

                    if (internalAutoFile.exists()) {
                        try {
                            internalAutoFile.copyTo(internalPrevFile, overwrite = true)
                        } catch (e: Exception) {
                            android.util.Log.w("WorkCraftAutoBackup", "Failed internal rotate to previous backup", e)
                        }
                    }

                    internalAutoFile.writeText(json, Charsets.UTF_8)
                    prefManager.updateBackupStatus(System.currentTimeMillis(), "Success (Internal Storage)")
                    return@withContext true
                } catch (e: Exception) {
                    lastError = "Internal auto-backup error: ${e.localizedMessage}"
                    android.util.Log.w("WorkCraftAutoBackup", lastError, e)
                }

                // Attempt 3: Public Documents Folder
                try {
                    val docDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "WorkCraftBackups")
                    if (!docDir.exists()) docDir.mkdirs()

                    val publicAutoFile = File(docDir, "WorkCraft_AutoBackup.json")
                    val publicPrevFile = File(docDir, "WorkCraft_PreviousBackup.json")

                    if (publicAutoFile.exists()) {
                        try {
                            publicAutoFile.copyTo(publicPrevFile, overwrite = true)
                        } catch (e: Exception) {
                            android.util.Log.w("WorkCraftAutoBackup", "Failed public dir rotate to previous backup", e)
                        }
                    }

                    publicAutoFile.writeText(json, Charsets.UTF_8)
                    prefManager.updateBackupStatus(System.currentTimeMillis(), "Success (Documents Folder)")
                    return@withContext true
                } catch (e: Exception) {
                    lastError = "Public dir auto-backup error: ${e.localizedMessage}"
                    android.util.Log.w("WorkCraftAutoBackup", lastError, e)
                }

                prefManager.updateBackupStatus(System.currentTimeMillis(), lastError ?: "Failed to write backup file")
                false
            } catch (e: Exception) {
                prefManager.updateBackupStatus(System.currentTimeMillis(), "Error: ${e.localizedMessage ?: "Unknown error"}")
                false
            }
        }
    }

    suspend fun findExistingAutoBackup(
        context: Context,
        prefManager: AutoBackupPreferenceManager
    ): BackupPayload? = withContext(Dispatchers.IO) {
        val fileNames = listOf("WorkCraft_AutoBackup.json", "WorkCraft_PreviousBackup.json")

        for (fileName in fileNames) {
            try {
                // 1. Try User-selected SAF Tree URI
                val uriString = prefManager.backupUri.value
                if (!uriString.isNullOrBlank()) {
                    try {
                        val treeUri = Uri.parse(uriString)
                        val treeDir = DocumentFile.fromTreeUri(context, treeUri)
                        val targetFile = treeDir?.findFile(fileName)
                        if (targetFile != null && targetFile.exists()) {
                            context.contentResolver.openInputStream(targetFile.uri)?.use { stream ->
                                return@withContext validateAndParseBackup(stream)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("WorkCraftAutoBackup", "SAF read auto backup failed for $fileName", e)
                    }
                }

                // 2. Try Internal App Storage
                val internalAutoFile = File(context.filesDir, fileName)
                if (internalAutoFile.exists()) {
                    try {
                        internalAutoFile.inputStream().use { stream ->
                            return@withContext validateAndParseBackup(stream)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("WorkCraftAutoBackup", "Internal read auto backup failed for $fileName", e)
                    }
                }

                // 3. Try MediaStore Documents File
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
        val validation = validateBackupOwnership(payload)
        if (validation is OwnershipValidationResult.Invalid) {
            throw IllegalStateException(validation.userFacingErrorMessage)
        }

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

            // 4. Insert Site Expenses and Expense Payments
            payload.siteExpenses.forEach { database.siteExpenseDao().insertExpense(it) }
            payload.siteExpensePayments.forEach { database.siteExpensePaymentDao().insertPayment(it) }

            // 5. Insert Materials and Material Transactions
            payload.materials.forEach { database.materialDao().insertMaterial(it) }
            payload.materialTransactions.forEach { database.materialTransactionDao().insertTransaction(it) }

            // 6. Insert Contractor Materials and Material Transactions
            payload.contractorMaterials.forEach { database.contractorMaterialDao().insertMaterial(it) }
            payload.contractorMaterialTransactions.forEach { database.contractorMaterialTransactionDao().insertTransaction(it) }
        }
    }
}
