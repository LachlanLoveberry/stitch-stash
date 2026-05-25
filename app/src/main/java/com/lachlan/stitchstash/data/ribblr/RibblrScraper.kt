package com.lachlan.stitchstash.data.ribblr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

data class RibblrImport(
    val name: String?,
    val designer: String?,
    val coverImageUrl: String?,
    val localCoverPath: String?,
    val sourceUrl: String,
)

object RibblrScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0 Mobile Safari/537.36"

    /**
     * Fetches the public Ribblr (or any) pattern page and pulls Open Graph metadata
     * plus the cover image. Image is downloaded to internal storage so it survives
     * Ribblr CDN URL changes.
     *
     * Returns null fields when the scrape fails — caller falls back to manual entry.
     */
    suspend fun import(context: Context, url: String): Result<RibblrImport> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string() ?: error("Empty body")
            }

            val doc = Jsoup.parse(html, url)
            val name = doc.firstNonBlank(
                "meta[property=og:title]" to "content",
                "meta[name=twitter:title]" to "content",
                "title" to null,
            )
            val designer = doc.firstNonBlank(
                "meta[name=author]" to "content",
                "meta[property=article:author]" to "content",
                "meta[property=og:site_name]" to "content",
            )
            val imageUrl = doc.firstNonBlank(
                "meta[property=og:image]" to "content",
                "meta[name=twitter:image]" to "content",
            )

            val localPath = imageUrl?.let { downloadImage(context, it) }

            RibblrImport(
                name = name?.trim()?.takeIf { it.isNotEmpty() },
                designer = designer?.trim()?.takeIf { it.isNotEmpty() },
                coverImageUrl = imageUrl,
                localCoverPath = localPath,
                sourceUrl = url,
            )
        }
    }

    private fun org.jsoup.nodes.Document.firstNonBlank(
        vararg selectors: Pair<String, String?>,
    ): String? {
        for ((selector, attr) in selectors) {
            val el = selectFirst(selector) ?: continue
            val value = if (attr != null) el.attr(attr) else el.text()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun downloadImage(context: Context, url: String): String? = runCatching {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val dir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
            val file = File(dir, "ribblr_${UUID.randomUUID()}.jpg")
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            file.absolutePath
        }
    }.getOrNull()
}
