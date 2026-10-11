package com.example.workermanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.workermanager.data.dao.*
import com.example.workermanager.data.entity.*

@Database(
    entities = [
        WorkerEntity::class,
        AttendanceEntity::class,
        PaymentEntity::class,
        SiteEntity::class,
        WorkerSiteHistoryEntity::class,
        ContractEntity::class,
        ClientPaymentEntity::class,
        SiteAttendanceEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun siteDao(): SiteDao
    abstract fun workerSiteHistoryDao(): WorkerSiteHistoryDao
    abstract fun contractDao(): ContractDao
    abstract fun clientPaymentDao(): ClientPaymentDao
    abstract fun siteAttendanceDao(): SiteAttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workers ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workers ADD COLUMN deletedAt INTEGER DEFAULT NULL")

                db.execSQL("ALTER TABLE sites ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sites ADD COLUMN deletedAt INTEGER DEFAULT NULL")

                db.execSQL("ALTER TABLE contracts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contracts ADD COLUMN deletedAt INTEGER DEFAULT NULL")

                db.execSQL("ALTER TABLE payments ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payments ADD COLUMN deletedAt INTEGER DEFAULT NULL")

                db.execSQL("ALTER TABLE client_payments ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE client_payments ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "worker_manager_database"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
