package com.ashutosh.corridor360.Data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ashutosh.corridor360.Data.local.dao.EdgeDao
import com.ashutosh.corridor360.Data.local.dao.NodeDao
import com.ashutosh.corridor360.Data.local.dao.FrameDao
import com.ashutosh.corridor360.Data.local.entity.FrameEntity
import com.ashutosh.corridor360.entity.NodeEntity
// TODO: add the real import path for EdgeEntity — not shown in what you've sent me

@Database(
    entities = [NodeEntity::class, EdgeEntity::class, FrameEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun nodeDao(): NodeDao
    abstract fun edgeDao(): EdgeDao
    abstract fun frameDao(): FrameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        fun resetInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}