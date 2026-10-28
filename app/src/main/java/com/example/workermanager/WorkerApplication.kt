package com.example.workermanager

import android.app.Application
import com.example.workermanager.data.database.AppDatabase
import com.example.workermanager.data.preference.AutoBackupPreferenceManager
import com.example.workermanager.data.preference.ThemePreferenceManager
import com.example.workermanager.data.repository.*

class WorkerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    val themePreferenceManager by lazy { ThemePreferenceManager(this) }

    val authPreferenceManager by lazy { com.example.workermanager.data.preference.AuthPreferenceManager(this) }

    val siteRepository by lazy { SiteRepository(database.siteDao()) }
    
    val workerRepository by lazy { 
        WorkerRepository(database.workerDao(), database.workerSiteHistoryDao(), database.siteDao()) 
    }
    
    val attendanceRepository by lazy { 
        AttendanceRepository(database.attendanceDao(), database.siteAttendanceDao()) 
    }

    val paymentRepository by lazy { PaymentRepository(database.paymentDao()) }

    val contractRepository by lazy { ContractRepository(database.contractDao()) }

    val clientPaymentRepository by lazy { ClientPaymentRepository(database.clientPaymentDao()) }

    val siteExpenseRepository by lazy { SiteExpenseRepository(database.siteExpenseDao(), database.siteExpensePaymentDao()) }

    val materialRepository by lazy { MaterialRepository(database.materialDao(), database.materialTransactionDao()) }

    val contractorMaterialRepository by lazy {
        ContractorMaterialRepository(database.contractorMaterialDao(), database.contractorMaterialTransactionDao())
    }

    override fun onCreate() {
        super.onCreate()
        val autoBackupManager = AutoBackupPreferenceManager(this)
        if (autoBackupManager.isAutoBackupEnabled.value) {
            autoBackupManager.scheduleWorkManager()
        }
    }
}
