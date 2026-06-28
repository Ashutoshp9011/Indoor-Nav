package com.ashutosh.corridor360.Data.sync

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.Base64

class GitHubSyncManager(
    private val context: Context,
    private val repoOwner: String,
    private val repoName: String,
    private val filePath: String = "database/nodes.sqlite",
    private val branch: String = "main",
    private val token: String // pass from BuildConfig or encrypted storage, never hardcode
) {
    private val client = OkHttpClient()
    private var currentSha: String? = null // needed later for push (Step 9)

    private fun apiUrl() =
        "https://api.github.com/repos/$repoOwner/$repoName/contents/$filePath?ref=$branch"

    // Returns local File pointing to the downloaded DB
    suspend fun pullDatabase(): File {
        val request = Request.Builder()
            .url(apiUrl())
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("GitHub pull failed: ${response.code} ${response.message}")
        }

        val body = response.body?.string() ?: throw Exception("Empty response body")
        val json = JSONObject(body)

        currentSha = json.getString("sha") // store for Step 9 push
        val base64Content = json.getString("content").replace("\n", "")
        val decodedBytes = Base64.getDecoder().decode(base64Content)

        val dbFile = File(context.filesDir, "nodes.sqlite")
        dbFile.writeBytes(decodedBytes)

        return dbFile
    }

    fun getCurrentSha(): String? = currentSha

    suspend fun pushDatabase(localFile: File) {
        val content = Base64.getEncoder().encodeToString(localFile.readBytes())
        val sha = currentSha ?: throw Exception("No SHA found. Pull before pushing.")

        val jsonBody = JSONObject().apply {
            put("message", "Sync database from Android app")
            put("content", content)
            put("sha", sha)
            put("branch", branch)
        }

        val request = Request.Builder()
            .url(apiUrl())
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .put(okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json"),
                jsonBody.toString()
            ))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("GitHub push failed: ${response.code} ${response.message}")
        }

        val responseBody = response.body?.string()
        if (responseBody != null) {
            currentSha = JSONObject(responseBody).getJSONObject("content").getString("sha")
        }
    }
}