package com.nihongo.srs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The single Room database of the app. Everything is stored on the phone,
 * so the app works 100% offline.
 */
@Database(
    entities = [VocabEntity::class, UserProgress::class, StudyDay::class, ExamResult::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vocabDao(): VocabDao
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1 -> v2: example sentences, phrase categories, learnedAt. Keeps all progress. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("example", "exampleReading", "exampleEn", "exampleBn", "category").forEach {
                    db.execSQL("ALTER TABLE vocab ADD COLUMN $it TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL("ALTER TABLE user_progress ADD COLUMN learnedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE user_progress SET learnedAt = lastReviewed")
            }
        }

        /** v2 -> v3: daily study log and exam history for the Progress screen. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(MigrationSql.CREATE_STUDY_DAY)
                db.execSQL(MigrationSql.CREATE_EXAM_RESULT)
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nihongo_srs.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}
