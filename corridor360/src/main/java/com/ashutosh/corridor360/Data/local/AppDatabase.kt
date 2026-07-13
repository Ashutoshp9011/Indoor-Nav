package com.ashutosh.corridor360.Data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ashutosh.corridor360.Data.local.dao.EdgeDao
import com.ashutosh.corridor360.Data.local.dao.NodeDao
import com.ashutosh.corridor360.Data.local.dao.FrameDao
import com.ashutosh.corridor360.Data.local.dao.PanoramaDao
import com.ashutosh.corridor360.entity.FrameEntity
import com.ashutosh.corridor360.entity.NodeEntity
import com.ashutosh.corridor360.entity.PanoramaEntity

@Database(
    entities = [NodeEntity::class, EdgeEntity::class, FrameEntity::class, PanoramaEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun nodeDao(): NodeDao
    abstract fun edgeDao(): EdgeDao
    abstract fun frameDao(): FrameDao
    abstract fun panoramaDao(): PanoramaDao

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