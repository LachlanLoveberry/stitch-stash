package com.lachlan.stitchstash.ui.finishcard

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * FinishCardRenderer itself draws directly to android.graphics.Canvas/Bitmap and has no
 * separable pure logic worth unit testing on the JVM (rendering correctness belongs in an
 * androidTest/e2e/screenshot phase). BorderStyle.from(), however, is plain key-lookup logic
 * that lives in the same file, so it's covered here.
 */
class BorderStyleTest {

    @Test
    fun `from resolves known keys to their style`() {
        assertThat(BorderStyle.from("floral")).isEqualTo(BorderStyle.FLORAL)
        assertThat(BorderStyle.from("scallop")).isEqualTo(BorderStyle.SCALLOP)
        assertThat(BorderStyle.from("granny")).isEqualTo(BorderStyle.GRANNY)
        assertThat(BorderStyle.from("simple")).isEqualTo(BorderStyle.SIMPLE)
    }

    @Test
    fun `from falls back to FLORAL for unknown key`() {
        assertThat(BorderStyle.from("unknown")).isEqualTo(BorderStyle.FLORAL)
    }
}
