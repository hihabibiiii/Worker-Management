package com.example.workermanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.workermanager.data.dao.*
import com.example.workermanager.data.entity.*

@Database(
    entities = [
        WorkerEntity::class,
        AttendanceEntity::class,
        PaymentEntity::class,
        SiteEntity::class,
        WorkerSiteHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun siteDao(): SiteDao
    abstract fun workerSiteHistoryDao(): WorkerSiteHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "worker_manager_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
