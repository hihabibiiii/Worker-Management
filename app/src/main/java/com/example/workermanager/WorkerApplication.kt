package com.example.workermanager

import android.app.Application
import com.example.workermanager.data.database.AppDatabase
import com.example.workermanager.data.repository.AttendanceRepository
import com.example.workermanager.data.repository.PaymentRepository
import com.example.workermanager.data.repository.SiteRepository
import com.example.workermanager.data.repository.WorkerRepository

class WorkerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    val siteRepository by lazy { SiteRepository(database.siteDao()) }
    
    val workerRepository by lazy { 
        WorkerRepository(database.workerDao(), database.workerSiteHistoryDao()) 
    }
    
    val attendanceRepository by lazy { AttendanceRepository(database.attendanceDao()) }

    val paymentRepository by lazy { PaymentRepository(database.paymentDao()) }
}
