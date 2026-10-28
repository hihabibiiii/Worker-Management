package com.example.workermanager

import android.app.Application
import android.util.Log
import com.example.workermanager.data.database.AppDatabase
import com.example.workermanager.data.preference.AuthPreferenceManager
import com.example.workermanager.data.preference.AutoBackupPreferenceManager
import com.example.workermanager.data.preference.ThemePreferenceManager
import com.example.workermanager.data.repository.*
import com.example.workermanager.util.BackupManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class UserSession(
    val userId: String,
    val database: AppDatabase,
    val siteRepository: SiteRepository,
    val workerRepository: WorkerRepository,
    val attendanceRepository: AttendanceRepository,
    val paymentRepository: PaymentRepository,
    val contractRepository: ContractRepository,
    val clientPaymentRepository: ClientPaymentRepository,
    val siteExpenseRepository: SiteExpenseRepository,
    val materialRepository: MaterialRepository,
    val contractorMaterialRepository: ContractorMaterialRepository,
    val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
)

class UserSessionManager(private val context: Application) {
    private val lock = Any()
    @Volatile
    private var activeSession: UserSession? = null

    fun getActiveSession(expectedUserId: String? = null): UserSession? {
        synchronized(lock) {
            val session = activeSession ?: return null
            if (expectedUserId != null && session.userId != expectedUserId) {
                return null
            }
            return session
        }
    }

    fun getOrCreateSession(userId: String): UserSession {
        require(userId.isNotBlank()) { "Firebase UID cannot be blank for UserSession creation" }
        synchronized(lock) {
            val current = activeSession
            if (current != null) {
                if (current.userId == userId) {
                    return current
                }
                closeSessionInternal(current)
            }

            val syncTrigger = {
                com.example.workermanager.util.CloudSyncWorker.scheduleImmediateSync(context)
            }

            val db = AppDatabase.getDatabaseForUser(context, userId)
            val session = UserSession(
                userId = userId,
                database = db,
                siteRepository = SiteRepository(db.siteDao(), syncTrigger),
                workerRepository = WorkerRepository(db.workerDao(), db.workerSiteHistoryDao(), db.siteDao(), syncTrigger),
                attendanceRepository = AttendanceRepository(db.attendanceDao(), db.siteAttendanceDao(), syncTrigger),
                paymentRepository = PaymentRepository(db.paymentDao(), syncTrigger),
                contractRepository = ContractRepository(db.contractDao(), syncTrigger),
                clientPaymentRepository = ClientPaymentRepository(db.clientPaymentDao(), syncTrigger),
                siteExpenseRepository = SiteExpenseRepository(db.siteExpenseDao(), db.siteExpensePaymentDao(), syncTrigger),
                materialRepository = MaterialRepository(db.materialDao(), db.materialTransactionDao(), syncTrigger),
                contractorMaterialRepository = ContractorMaterialRepository(db.contractorMaterialDao(), db.contractorMaterialTransactionDao(), syncTrigger)
            )
            activeSession = session

            // Attach real-time cloud listeners & schedule initial sync
            com.example.workermanager.data.remote.sync.engine.FirestoreRealtimeSyncManager.startListening(session)
            com.example.workermanager.util.CloudSyncWorker.scheduleImmediateSync(context)

            return session
        }
    }

    fun closeActiveSession() {
        synchronized(lock) {
            val current = activeSession
            if (current != null) {
                closeSessionInternal(current)
                activeSession = null
            }
        }
    }

    private fun closeSessionInternal(session: UserSession) {
        try {
            com.example.workermanager.data.remote.sync.engine.FirestoreRealtimeSyncManager.stopListening()
            session.sessionScope.cancel()
        } catch (e: Exception) {
            Log.e("UserSessionManager", "Error cancelling session scope for ${session.userId}", e)
        }
        AppDatabase.closeUserDatabase(session.userId)
    }
}

class WorkerApplication : Application() {
    val themePreferenceManager by lazy { ThemePreferenceManager(this) }
    val authPreferenceManager by lazy { AuthPreferenceManager(this) }
    val userSessionManager by lazy { UserSessionManager(this) }

    // Legacy database accessor preserved strictly for safety backups and offline tools
    val legacyDatabase by lazy { AppDatabase.getDatabase(this) }

    val database: AppDatabase
        get() {
            val user = FirebaseAuth.getInstance().currentUser
            return if (user != null && user.uid.isNotBlank()) {
                userSessionManager.getOrCreateSession(user.uid).database
            } else {
                legacyDatabase
            }
        }

    val contractorMaterialRepository: ContractorMaterialRepository
        get() {
            val user = FirebaseAuth.getInstance().currentUser
            return if (user != null && user.uid.isNotBlank()) {
                userSessionManager.getOrCreateSession(user.uid).contractorMaterialRepository
            } else {
                ContractorMaterialRepository(legacyDatabase.contractorMaterialDao(), legacyDatabase.contractorMaterialTransactionDao())
            }
        }

    override fun onCreate() {
        super.onCreate()

        // Safety Backup Verification of Legacy Database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val legacyDb = legacyDatabase
                val payload = BackupManager.createBackupPayload(legacyDb)
                Log.i(
                    "WorkCraftLegacyBackup",
                    "Legacy Backup Verified. Counts: " +
                    "Workers=${payload.workers.size}, Sites=${payload.sites.size}, " +
                    "Attendance=${payload.attendance.size}, Payments=${payload.payments.size}, " +
                    "Contracts=${payload.contracts.size}, ClientPayments=${payload.clientPayments.size}, " +
                    "WorkerSiteHistory=${payload.workerSiteHistory.size}, SiteAttendance=${payload.siteAttendance.size}, " +
                    "SiteExpenses=${payload.siteExpenses.size}, SiteExpensePayments=${payload.siteExpensePayments.size}, " +
                    "Materials=${payload.materials.size}, MaterialTx=${payload.materialTransactions.size}, " +
                    "ContractorMaterials=${payload.contractorMaterials.size}, ContractorMaterialTx=${payload.contractorMaterialTransactions.size}"
                )
            } catch (e: Exception) {
                Log.e("WorkCraftLegacyBackup", "Error performing legacy backup verification", e)
            }
        }

        val autoBackupManager = AutoBackupPreferenceManager(this)
        if (autoBackupManager.isAutoBackupEnabled.value) {
            autoBackupManager.scheduleWorkManager()
        }
    }
}
