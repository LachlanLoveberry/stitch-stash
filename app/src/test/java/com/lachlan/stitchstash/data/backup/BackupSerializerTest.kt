package com.lachlan.stitchstash.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.StitchStashDatabase
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.db.entities.Pattern
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupSerializerTest {

    private lateinit var db: StitchStashDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StitchStashDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun freshDb(): StitchStashDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, StitchStashDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun `round-trip export then import preserves key fields`() = runTest {
        val patternId = db.patternDao().insert(Pattern(name = "Granny Square", designer = "Jo"))
        val colourwayId = db.colourwayDao().insert(
            Colourway(patternId = patternId, name = "Rainbow", targetCount = 3),
        )
        db.completionDao().insert(
            Completion(colourwayId = colourwayId, completedAtEpochDay = 19000L, notes = "first one"),
        )
        db.marketDao().insert(Market(name = "Spring Fair", dateEpochDay = 19100L))
        db.appSettingsDao().upsert(AppSettings(weeklyHours = 6f, targetPieces = 12))

        val json = BackupSerializer.export(db)

        val target = freshDb()
        try {
            val summary = BackupSerializer.import(target, json)

            assertThat(summary.patterns).isEqualTo(1)
            assertThat(summary.colourways).isEqualTo(1)
            assertThat(summary.completions).isEqualTo(1)
            assertThat(summary.markets).isEqualTo(1)

            val importedPattern = target.patternDao().getById(patternId)
            assertThat(importedPattern?.name).isEqualTo("Granny Square")
            assertThat(importedPattern?.designer).isEqualTo("Jo")

            val importedColourway = target.colourwayDao().getById(colourwayId)
            assertThat(importedColourway?.name).isEqualTo("Rainbow")
            assertThat(importedColourway?.targetCount).isEqualTo(3)

            val importedSettings = target.appSettingsDao().get()
            assertThat(importedSettings?.weeklyHours).isEqualTo(6f)
            assertThat(importedSettings?.targetPieces).isEqualTo(12)
        } finally {
            target.close()
        }
    }

    @Test(expected = Exception::class)
    fun `malformed json throws on import`() = runTest {
        BackupSerializer.import(db, "{ this is not valid json ")
    }

    @Test(expected = Exception::class)
    fun `truncated json throws on import`() {
        val valid = """{"schemaVersion":1,"exportedAt":1,"appVersion":"1.0","patterns":[],"colourways":[],"completions":[],"markets":[],"stickers":[],"scenarios":[],"finishCards":[],"settings":{"""
        runTest { BackupSerializer.import(db, valid) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `schemaVersion newer than supported throws`() = runTest {
        val futureVersionJson = """
            {
              "schemaVersion": ${BACKUP_SCHEMA_VERSION + 1},
              "exportedAt": 1,
              "appVersion": "1.0",
              "patterns": [],
              "colourways": [],
              "completions": [],
              "markets": [],
              "marketTodos": [],
              "stickers": [],
              "scenarios": [],
              "finishCards": [],
              "settings": {
                "weeklyHours": 4.0,
                "targetPieces": 0,
                "forecastVisible": true,
                "weeklyRecapEnabled": false,
                "stickerBookEnabled": true,
                "finishCardBorderStyle": "floral",
                "avgHoursPerPieceSeed": 4.0,
                "driveBackupEnabled": false,
                "driveFolderId": null,
                "lastBackupAt": null,
                "onboardingComplete": false
              }
            }
        """.trimIndent()

        BackupSerializer.import(db, futureVersionJson)
    }

    @Test
    fun `empty lists import cleanly with zero-count summary`() = runTest {
        val emptyJson = """
            {
              "schemaVersion": $BACKUP_SCHEMA_VERSION,
              "exportedAt": 1,
              "appVersion": "1.0",
              "patterns": [],
              "colourways": [],
              "completions": [],
              "markets": [],
              "marketTodos": [],
              "stickers": [],
              "scenarios": [],
              "finishCards": [],
              "settings": {
                "weeklyHours": 4.0,
                "targetPieces": 0,
                "forecastVisible": true,
                "weeklyRecapEnabled": false,
                "stickerBookEnabled": true,
                "finishCardBorderStyle": "floral",
                "avgHoursPerPieceSeed": 4.0,
                "driveBackupEnabled": false,
                "driveFolderId": null,
                "lastBackupAt": null,
                "onboardingComplete": false
              }
            }
        """.trimIndent()

        val summary = BackupSerializer.import(db, emptyJson)

        assertThat(summary.patterns).isEqualTo(0)
        assertThat(summary.colourways).isEqualTo(0)
        assertThat(summary.completions).isEqualTo(0)
        assertThat(summary.markets).isEqualTo(0)
        assertThat(summary.marketTodos).isEqualTo(0)
        assertThat(summary.stickers).isEqualTo(0)
        assertThat(summary.scenarios).isEqualTo(0)
        assertThat(summary.finishCards).isEqualTo(0)
    }

    @Test
    fun `market with null optional fields round-trips fine`() = runTest {
        db.marketDao().insert(
            Market(
                name = null,
                dateEpochDay = 19100L,
                attended = null,
                howItWent = null,
                howItFelt = null,
                whatLearned = null,
            ),
        )

        val json = BackupSerializer.export(db)

        val target = freshDb()
        try {
            val summary = BackupSerializer.import(target, json)
            assertThat(summary.markets).isEqualTo(1)

            val markets = target.marketDao().observeAll()
            // Query directly instead of Flow collection to keep this simple.
            val imported = target.marketDao().getById(1L)
            assertThat(imported?.name).isNull()
            assertThat(imported?.attended).isNull()
            assertThat(imported?.howItWent).isNull()
        } finally {
            target.close()
        }
    }
}
