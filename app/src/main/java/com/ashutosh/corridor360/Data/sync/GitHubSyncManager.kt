package com.ashutosh.corridor360.Data.sync

import com.ashutosh.corridor360.Data.local.dao.EdgeDao
import com.ashutosh.corridor360.Data.local.EdgeEntity
import com.ashutosh.corridor360.Data.local.dao.NodeDao
import com.ashutosh.corridor360.entity.NodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

class GitHubSyncManager(
    private val nodeDao: NodeDao,
    private val edgeDao: EdgeDao,
    private val token: String,
    private val owner: String = "<your-github-username>",
    private val repo: String = "Indoor-navigation-system",
    private val path: String = "data/corridor_graph.json",
    private val branch: String = "main"
) {
    private val client = OkHttpClient()
    private val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$path"

    suspend fun pull() = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$apiUrl?ref=$branch")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext
            val body = JSONObject(resp.body!!.string())
            val decoded = String(Base64.getDecoder().decode(body.getString("content").replace("\n", "")))
            mergeRemoteJson(decoded)
        }
    }

    suspend fun push() = withContext(Dispatchers.IO) {
        pull() // always merge remote first so you don't clobber teammates
        val sha = getCurrentSha()
        val json = buildLocalJson()
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray())
        val payload = JSONObject().apply {
            put("message", "Sync corridor graph (${android.os.Build.MODEL})")
            put("content", encoded)
            put("branch", branch)
            sha?.let { put("sha", it) }
        }
        val req = Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .put(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().close()
    }

    private fun getCurrentSha(): String? {
        val req = Request.Builder().url("$apiUrl?ref=$branch")
            .header("Authorization", "Bearer $token").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return JSONObject(resp.body!!.string()).getString("sha")
        }
    }

    private suspend fun buildLocalJson(): String {
        val nodes = nodeDao.getAllNodes().first()
        val edges = edgeDao.getAllEdges().first()
        val nodeArr = JSONArray(nodes.map { n ->
            JSONObject().apply {
                put("nodeId", n.nodeId); put("name", n.name); put("floor", n.floor)
                put("x", n.x); put("y", n.y); put("status", n.status)
                put("panoramaPath", n.panoramaPath ?: JSONObject.NULL)
            }
        })
        val edgeArr = JSONArray(edges.map { e ->
            JSONObject().apply {
                put("edgeId", e.edgeId); put("fromNodeId", e.fromNodeId)
                put("toNodeId", e.toNodeId); put("distanceMeters", e.distanceMeters)
            }
        })
        return JSONObject().apply { put("nodes", nodeArr); put("edges", edgeArr) }.toString()
    }

    private suspend fun mergeRemoteJson(json: String) {
        val root = JSONObject(json)
        val nodes = root.getJSONArray("nodes")
        for (i in 0 until nodes.length()) {
            val o = nodes.getJSONObject(i)
            nodeDao.insertNode(
                NodeEntity(
                    nodeId = o.getString("nodeId"),
                    name = o.getString("name"),
                    floor = o.getInt("floor"),
                    x = o.getDouble("x").toFloat(),
                    y = o.getDouble("y").toFloat(),
                    status = o.getString("status"),
                    panoramaPath = if (o.isNull("panoramaPath")) null else o.getString("panoramaPath")
                )
            )
        }
        val edges = root.getJSONArray("edges")
        for (i in 0 until edges.length()) {
            val o = edges.getJSONObject(i)
            edgeDao.insertEdge(
                EdgeEntity( // requires the String-id change above
                    edgeId = o.getString("edgeId"),
                    fromNodeId = o.getString("fromNodeId"),
                    toNodeId = o.getString("toNodeId"),
                    distanceMeters = o.getDouble("distanceMeters").toFloat()
                )
            )
        }
    }
}