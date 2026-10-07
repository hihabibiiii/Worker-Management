package com.example.workermanager

import android.app.Application
import com.example.workermanager.data.database.AppDatabase
import com.example.workermanager.data.repository.*

class WorkerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    val siteRepository by lazy { SiteRepository(database.siteDao()) }
    
    val workerRepository by lazy { 
        WorkerRepository(database.workerDao(), database.workerSiteHistoryDao()) 
    }
    
    val attendanceRepository by lazy { AttendanceRepository(database.attendanceDao()) }

    val paymentRepository by lazy { PaymentRepository(database.paymentDao()) }

    val contractRepository by lazy { ContractRepository(database.contractDao()) }

    val clientPaymentRepository by lazy { ClientPaymentRepository(database.clientPaymentDao()) }
}
