package com.lachlan.stitchstash.data.update

import com.lachlan.stitchstash.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub Releases for a newer version. Set OWNER and REPO to your repo
 * once it's pushed.
 */
object UpdateChecker {

    private const val OWNER = "lachlanloveberry"
    private const val REPO = "stitch-stash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            val body = client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                resp.body?.string() ?: return@runCatching null
            }
            val release = json.decodeFromString(GitHubRelease.serializer(), body)
            val latest = release.tag_name.removePrefix("v")
            if (isNewer(latest, BuildConfig.VERSION_NAME)) {
                UpdateInfo(
                    latestVersion = latest,
                    htmlUrl = release.html_url,
                    notes = release.body.orEmpty(),
                )
            } else null
        }.getOrNull()
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(l.size, c.size)
        for (i in 0 until size) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}

data class UpdateInfo(val latestVersion: String, val htmlUrl: String, val notes: String)

@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val body: String? = null,
)
