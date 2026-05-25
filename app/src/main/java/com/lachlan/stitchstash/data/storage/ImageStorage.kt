package com.lachlan.stitchstash.data.storage

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ImageStorage {

    private fun imageDir(context: Context): File =
        File(context.filesDir, "images").apply { if (!exists()) mkdirs() }

    /** Copies a picker-returned Uri into internal storage and returns the absolute path. */
    suspend fun persist(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        val target = File(imageDir(context), "${UUID.randomUUID()}.jpg")
        runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } ?: return@withContext null
            target.absolutePath
        }.getOrNull()
    }

    fun delete(path: String) {
        runCatching { File(path).delete() }
    }
}
