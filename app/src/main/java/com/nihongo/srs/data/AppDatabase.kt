package com.nihongo.srs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The single Room database of the app. Everything is stored on the phone,
 * so the app works 100% offline.
 */
@Database(
    entities = [VocabEntity::class, UserProgress::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vocabDao(): VocabDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nihongo_srs.db"
                ).build().also { INSTANCE = it }
            }
    }
}
