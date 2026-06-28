package com.ashutosh.entrance_mapper.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object DatabaseExporter {

    fun exportDatabase(context: Context): String {
        return try {
            val dbFile = context.getDatabasePath("entrance_mapper.db")
            if (!dbFile.exists()) return "Database not found"

            val exportDir = File(context.getExternalFilesDir(null), "EntranceMapper")
            if (!exportDir.exists()) exportDir.mkdirs()

            val exportFile = File(exportDir, "nodes_export.db")
            dbFile.copyTo(exportFile, overwrite = true)

            // Share via intent
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                exportFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Database"))

            "Database exported successfully"
        } catch (e: Exception) {
            "Export failed: ${e.message}"
        }
    }
}