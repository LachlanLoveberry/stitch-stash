package com.lachlan.stitchstash.data.storage

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImageStorageTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun persist_copiesSourceIntoInternalStorageAndReturnsItsPath() = runTest {
        val source = File(context.cacheDir, "source-${System.nanoTime()}.jpg")
        source.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        val persistedPath = ImageStorage.persist(context, Uri.fromFile(source))

        assertThat(persistedPath).isNotNull()
        val persistedFile = File(persistedPath!!)
        assertThat(persistedFile.exists()).isTrue()
        assertThat(persistedFile.readBytes()).isEqualTo(source.readBytes())
        // Persisted into this app's own internal storage, not left pointing at the source file.
        assertThat(persistedFile.absolutePath).contains(context.filesDir.absolutePath)
        assertThat(persistedFile.absolutePath).isNotEqualTo(source.absolutePath)

        source.delete()
        persistedFile.delete()
    }

    @Test
    fun persist_returnsNullForAnUnreadableSource() = runTest {
        val missing = Uri.fromFile(File(context.cacheDir, "does-not-exist-${System.nanoTime()}.jpg"))

        val persistedPath = ImageStorage.persist(context, missing)

        assertThat(persistedPath).isNull()
    }

    @Test
    fun delete_removesTheFileAtTheGivenPath() = runTest {
        val source = File(context.cacheDir, "to-persist-${System.nanoTime()}.jpg")
        source.writeBytes(byteArrayOf(9, 9, 9))
        val persistedPath = ImageStorage.persist(context, Uri.fromFile(source))!!
        assertThat(File(persistedPath).exists()).isTrue()

        ImageStorage.delete(persistedPath)

        assertThat(File(persistedPath).exists()).isFalse()
        source.delete()
    }

    @Test
    fun delete_onANonexistentPathDoesNotThrow() {
        ImageStorage.delete(File(context.filesDir, "never-existed.jpg").absolutePath)
    }
}
