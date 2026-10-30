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

sealed class IntegrityValidationResult {
    object Valid : IntegrityValidationResult()
    data class Invalid(val reason: String) : IntegrityValidationResult()
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

    fun validateBackupIntegrity(payload: BackupPayload): IntegrityValidationResult {
        if (payload.appName != "WorkCraft") {
            return IntegrityValidationResult.Invalid("Invalid app signature. Expected WorkCraft backup.")
        }
        if (payload.version <= 0 || payload.version > 2) {
            return IntegrityValidationResult.Invalid("Unsupported backup version: ${payload.version}.")
        }

        // Validate Foreign Key Dependencies
        val siteIds = payload.sites.map { it.id }.toSet()
        val workerIds = payload.workers.map { it.id }.toSet()
        val contractIds = payload.contracts.map { it.id }.toSet()
        val expenseIds = payload.siteExpenses.map { it.id }.toSet()
        val materialIds = payload.materials.map { it.id }.toSet()
        val contractorMatIds = payload.contractorMaterials.map { it.id }.toSet()

        // 1. Workers currentSiteId
        for (w in payload.workers) {
            if (w.currentSiteId != null && w.currentSiteId !in siteIds) {
                return IntegrityValidationResult.Invalid("Worker '${w.name}' (id=${w.id}) references non-existent siteId=${w.currentSiteId}.")
            }
        }

        // 2. Attendance
        for (att in payload.attendance) {
            if (att.workerId !in workerIds) {
                return IntegrityValidationResult.Invalid("Attendance entry (id=${att.id}) references non-existent workerId=${att.workerId}.")
            }
            if (att.siteId !in siteIds) {
                return IntegrityValidationResult.Invalid("Attendance entry (id=${att.id}) references non-existent siteId=${att.siteId}.")
            }
        }

        // 3. Payments
        for (p in payload.payments) {
            if (p.workerId !in workerIds) {
                return IntegrityValidationResult.Invalid("Payment (id=${p.id}) references non-existent workerId=${p.workerId}.")
            }
            if (p.siteId !in siteIds) {
                return IntegrityValidationResult.Invalid("Payment (id=${p.id}) references non-existent siteId=${p.siteId}.")
            }
        }

        // 4. Contracts
        for (c in payload.contracts) {
            if (c.siteId != null && c.siteId !in siteIds) {
                return IntegrityValidationResult.Invalid("Contract '${c.clientName}' (id=${c.id}) references non-existent siteId=${c.siteId}.")
            }
        }

        // 5. Client Payments
        for (cp in payload.clientPayments) {
            if (cp.contractId !in contractIds) {
                return IntegrityValidationResult.Invalid("Client payment (id=${cp.id}) references non-existent contractId=${cp.contractId}.")
            }
        }

        // 6. Worker Site History
        for (h in payload.workerSiteHistory) {
            if (h.workerId !in workerIds) {
                return IntegrityValidationResult.Invalid("Worker history (id=${h.id}) references non-existent workerId=${h.workerId}.")
            }
            if (h.siteId !in siteIds) {
                return IntegrityValidationResult.Invalid("Worker history (id=${h.id}) references non-existent siteId=${h.siteId}.")
            }
        }

        // 7. Site Attendance
        for (sa in payload.siteAttendance) {
            if (sa.siteId !in siteIds) {
                return IntegrityValidationResult.Invalid("Site attendance (id=${sa.id}) references non-existent siteId=${sa.siteId}.")
            }
        }

        // 8. Site Expenses
        for (e in payload.siteExpenses) {
            if (e.siteId !in siteIds) {
                return IntegrityValidationResult.Invalid("Site expense (id=${e.id}) references non-existent siteId=${e.siteId}.")
            }
        }

        // 9. Site Expense Payments
        for (ep in payload.siteExpensePayments) {
            if (ep.expenseId !in expenseIds) {
                return IntegrityValidationResult.Invalid("Expense payment (id=${ep.id}) references non-existent expenseId=${ep.expenseId}.")
            }
        }

        // 10. Material Transactions
        for (mt in payload.materialTransactions) {
            if (mt.materialId !in materialIds) {
                return IntegrityValidationResult.Invalid("Material transaction (id=${mt.id}) references non-existent materialId=${mt.materialId}.")
            }
            if (mt.siteId !in siteIds) {
                return IntegrityValidationResult.Invalid("Material transaction (id=${mt.id}) references non-existent siteId=${mt.siteId}.")
            }
        }

        // 11. Contractor Material Transactions
        for (cmt in payload.contractorMaterialTransactions) {
            if (cmt.materialId !in contractorMatIds) {
                return IntegrityValidationResult.Invalid("Contractor material transaction (id=${cmt.id}) references non-existent materialId=${cmt.materialId}.")
            }
            if (cmt.siteId != null && cmt.siteId !in siteIds) {
                return IntegrityValidationResult.Invalid("Contractor material transaction (id=${cmt.id}) references non-existent siteId=${cmt.siteId}.")
            }
        }

        return IntegrityValidationResult.Valid
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
        
        val integrity = validateBackupIntegrity(payload)
        if (integrity is IntegrityValidationResult.Invalid) {
            throw IllegalArgumentException(integrity.reason)
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
        val ownership = validateBackupOwnership(payload)
        if (ownership is OwnershipValidationResult.Invalid) {
            throw IllegalStateException(ownership.userFacingErrorMessage)
        }

        val integrity = validateBackupIntegrity(payload)
        if (integrity is IntegrityValidationResult.Invalid) {
            throw IllegalArgumentException("Backup integrity check failed: ${integrity.reason}")
        }

        // Safety check: protect against restoring completely empty backup over populated database
        val currentWorkers = database.workerDao().getAllWorkersRaw().size
        val currentSites = database.siteDao().getAllSitesRaw().size
        val currentAttendance = database.attendanceDao().getAllAttendanceRaw().size
        val totalExisting = currentWorkers + currentSites + currentAttendance

        val payloadTotal = payload.workers.size + payload.sites.size + payload.attendance.size
        if (totalExisting > 0 && payloadTotal == 0) {
            throw IllegalArgumentException("Refusing to restore an empty backup over an existing database containing $totalExisting records.")
        }

        database.withTransaction {
            database.clearAllTables()

            val now = System.currentTimeMillis()

            // 1. Insert Parent Sites first
            payload.sites.forEach { 
                database.siteDao().insertSite(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }

            // 2. Insert Materials and Contractor Materials
            payload.materials.forEach { 
                database.materialDao().insertMaterial(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }
            payload.contractorMaterials.forEach { 
                database.contractorMaterialDao().insertMaterial(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }

            // 3. Insert Workers & Contracts (may reference Sites)
            payload.workers.forEach { 
                database.workerDao().insertWorker(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }
            payload.contracts.forEach { 
                database.contractDao().insertContract(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }

            // 4. Insert Dependent Child Entities
            payload.attendance.forEach { 
                database.attendanceDao().insertAttendance(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }
            payload.payments.forEach { 
                database.paymentDao().insertPayment(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }
            payload.clientPayments.forEach { 
                database.clientPaymentDao().insertPayment(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }
            payload.workerSiteHistory.forEach { 
                database.workerSiteHistoryDao().insert(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }
            payload.siteAttendance.forEach { 
                database.siteAttendanceDao().insertSiteAttendance(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }

            // 5. Insert Site Expenses and Expense Payments
            payload.siteExpenses.forEach { 
                database.siteExpenseDao().insertExpense(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }
            payload.siteExpensePayments.forEach { 
                database.siteExpensePaymentDao().insertPayment(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }

            // 6. Insert Material Transactions and Contractor Material Transactions
            payload.materialTransactions.forEach { 
                database.materialTransactionDao().insertTransaction(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }
            payload.contractorMaterialTransactions.forEach { 
                database.contractorMaterialTransactionDao().insertTransaction(
                    it.copy(
                        isPendingSync = true, 
                        updatedAt = maxOf(it.updatedAt, now),
                        syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                        syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
                    )
                ) 
            }

            // Verify count consistency
            val sitesCount = database.siteDao().getAllSitesRaw().size
            val workersCount = database.workerDao().getAllWorkersRaw().size
            val attendanceCount = database.attendanceDao().getAllAttendanceRaw().size
            val paymentsCount = database.paymentDao().getAllPaymentsRaw().size
            val contractsCount = database.contractDao().getAllContractsRaw().size

            if (sitesCount != payload.sites.size ||
                workersCount != payload.workers.size ||
                attendanceCount != payload.attendance.size ||
                paymentsCount != payload.payments.size ||
                contractsCount != payload.contracts.size) {
                throw IllegalStateException("Restoration verification failed: record counts do not match payload.")
            }
        }
    }
}
