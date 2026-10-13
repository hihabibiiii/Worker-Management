package com.example.workermanager.util

import android.content.Context
import androidx.room.withTransaction
import com.example.workermanager.data.database.AppDatabase
import com.example.workermanager.data.entity.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
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
