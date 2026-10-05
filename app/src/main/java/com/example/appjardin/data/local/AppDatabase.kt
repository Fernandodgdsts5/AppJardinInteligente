package com.example.appjardin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PlantEntity::class, SessionEntity::class, DiagnosisEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun sessionDao(): SessionDao
    abstract fun diagnosisDao(): DiagnosisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN humedadMasBaja REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN humedadMasAlta REAL NOT NULL DEFAULT 100.0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN imagePath TEXT")
                db.execSQL("ALTER TABLE plants ADD COLUMN defaultKey TEXT")
                db.execSQL("UPDATE plants SET defaultKey = lower(name) WHERE name IN ('Tomate','Geranio','Rosa','Helecho') AND id <= 4")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN isUserCreated INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE plants SET isUserCreated = 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `diagnostics` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`plantId` INTEGER, " +
                            "`plantName` TEXT NOT NULL, " +
                            "`imagePath` TEXT NOT NULL, " +
                            "`result` TEXT NOT NULL, " +
                            "`confidence` REAL NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "`modelVersion` TEXT NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jardin_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
