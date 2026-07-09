package com.ashutosh.corridor360.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ashutosh.corridor360.Data.local.dao.NodeDao
import com.ashutosh.corridor360.entity.NodeEntity
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

private val Context.seedDataStore: DataStore<Preferences> by preferencesDataStore(name = "seed_prefs")
private val SEEDED_KEY = booleanPreferencesKey("nodes_seeded_v1")

/**
 * Imports the bundled nodes.sqlite asset into Room on first app launch only.
 * Guarded by a DataStore flag so it never re-seeds and clobbers user-mapped
 * data on subsequent launches.
 *
 * Expects nodes.sqlite to sit at app/src/main/assets/nodes.sqlite with a
 * table matching the columns read below — adjust the column names in
 * readNodesFromAsset() to match your actual nodes.sqlite schema.
 */
class SeedDatabaseLoader(
    private val context: Context,
    private val nodeDao: NodeDao
) {

    suspend fun seedIfNeeded() {
        val alreadySeeded = context.seedDataStore.data.first()[SEEDED_KEY] ?: false
        if (alreadySeeded) return

        try {
            val nodes = readNodesFromAsset()
            nodeDao.insertNodes(nodes)
        } catch (e: Exception) {
            return
        }

        context.seedDataStore.edit { prefs ->
            prefs[SEEDED_KEY] = true
        }
    }

    /** Escape hatch for a "reset to bundled data" dev/debug option, if you want one. */
    suspend fun forceReseed() {
        context.seedDataStore.edit { prefs -> prefs[SEEDED_KEY] = false }
        seedIfNeeded()
    }

    private fun readNodesFromAsset(): List<NodeEntity> {
        // SQLite can't be opened directly from the assets/ APK entry, so copy
        // it to internal storage first, then open it read-only from there.
        val dbFile = File(context.filesDir, "nodes_seed_temp.sqlite")
        try {
            context.assets.open("nodes.sqlite").use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        val db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        val nodes = mutableListOf<NodeEntity>()
        // TODO: adjust table/column names to match your actual nodes.sqlite schema
        db.rawQuery("SELECT nodeId, name, floor, x, y FROM nodes", null).use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow("nodeId")
            val nameCol = cursor.getColumnIndexOrThrow("name")
            val floorCol = cursor.getColumnIndexOrThrow("floor")
            val xCol = cursor.getColumnIndexOrThrow("x")
            val yCol = cursor.getColumnIndexOrThrow("y")

            while (cursor.moveToNext()) {
                nodes.add(
                    NodeEntity(
                        nodeId = cursor.getString(idCol),
                        name = cursor.getString(nameCol),
                        floor = cursor.getInt(floorCol),
                        x = cursor.getFloat(xCol),
                        y = cursor.getFloat(yCol),
                        status = "unmapped"
                    )
                )
            }
        }

        db.close()
        dbFile.delete()

        return nodes
    }
}