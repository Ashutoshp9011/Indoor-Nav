package com.ashutosh.corridor360.Data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ashutosh.corridor360.Data.local.dao.EdgeDao
import com.ashutosh.corridor360.Data.local.dao.NodeDao
import com.ashutosh.corridor360.entity.NodeEntity

@Database(
    entities = [NodeEntity::class, EdgeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun nodeDao(): NodeDao
    abstract fun edgeDao(): EdgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Call this once at app start, with the path of the synced/downloaded sqlite file
        fun getInstance(context: Context, dbFilePath: String): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbFilePath
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Force-close and rebuild after a fresh GitHub sync replaces the file
        fun resetInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
