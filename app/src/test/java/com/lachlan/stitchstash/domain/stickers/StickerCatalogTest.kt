package com.lachlan.stitchstash.domain.stickers

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StickerCatalogTest {

    @Test
    fun `get returns definition for known type`() {
        val def = StickerCatalog.get(StickerCatalog.FIRST_EVER)
        assertThat(def.type).isEqualTo(StickerCatalog.FIRST_EVER)
        assertThat(def.title).isNotEmpty()
    }

    @Test
    fun `get falls back to synthesized definition for unknown type without throwing`() {
        val def = StickerCatalog.get("not_a_real_type_from_a_future_import")
        assertThat(def.type).isEqualTo("not_a_real_type_from_a_future_import")
        assertThat(def.title).isEqualTo("not_a_real_type_from_a_future_import")
    }

    @Test
    fun `get handles blank type string`() {
        val def = StickerCatalog.get("")
        assertThat(def.type).isEqualTo("")
    }

    @Test
    fun `all returns every catalog entry`() {
        assertThat(StickerCatalog.all()).hasSize(9)
    }
}
