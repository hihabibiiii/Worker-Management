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
    version = 6,
    exportSchema = true
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `worker_site_history` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`workerId` INTEGER NOT NULL, " +
                    "`siteId` INTEGER NOT NULL, " +
                    "`startDate` TEXT NOT NULL, " +
                    "`endDate` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `contracts` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`clientName` TEXT NOT NULL, " +
                    "`mobile` TEXT NOT NULL, " +
                    "`siteId` INTEGER, " +
                    "`totalAmount` REAL NOT NULL, " +
                    "`startDate` TEXT NOT NULL, " +
                    "`notes` TEXT NOT NULL, " +
                    "`status` TEXT NOT NULL, " +
                    "FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `client_payments` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`contractId` INTEGER NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`paymentDate` TEXT NOT NULL, " +
                    "`method` TEXT NOT NULL, " +
                    "`note` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`contractId`) REFERENCES `contracts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `site_attendance` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`siteId` INTEGER NOT NULL, " +
                    "`date` TEXT NOT NULL, " +
                    "`isPresent` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_site_attendance_siteId_date` ON `site_attendance` (`siteId`, `date`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE client_payments ADD COLUMN isEdited INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "worker_manager_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
