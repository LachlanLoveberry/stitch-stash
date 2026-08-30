package com.lachlan.stitchstash.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real MIGRATION_1_2 -> MIGRATION_2_3 path against a populated v1 database,
 * rather than just letting Room create the latest schema fresh (which every other test does).
 * A broken migration is one of the more common ways a real app corrupts a user's data on update.
 *
 * Schema v2 was never exported (app/schemas only has 1.json and 3.json), so this validates the
 * chained 1->3 migration against the v3 reference schema rather than stopping at v2.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StitchStashDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To3_preservesExistingDataAndAddsNewColumnsAndTables() {
        // Seed a v1 database using the historical (NOT NULL name, no reflection columns) schema.
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO patterns (id, name, coverImageUri, ribblrUrl, designer, estimateHours, " +
                    "estimateBucket, similarToPatternId, createdAt) VALUES " +
                    "(1, 'Amigurumi Fox', NULL, NULL, NULL, NULL, NULL, NULL, 1000)",
            )
            execSQL(
                "INSERT INTO colourways (id, patternId, name, swatchHex, targetCount, createdAt) " +
                    "VALUES (1, 1, 'Rust', NULL, 5, 1000)",
            )
            execSQL(
                "INSERT INTO completions (id, colourwayId, completedAtEpochDay, photoUri, notes, " +
                    "energyTag, createdAt) VALUES (1, 1, 19000, NULL, NULL, NULL, 1000)",
            )
            execSQL(
                "INSERT INTO markets (id, name, dateEpochDay, isSkipped, createdAt) " +
                    "VALUES (1, 'Spring Fair', 19100, 0, 1000)",
            )
            close()
        }

        // Run the real migrations (not a fresh-schema shortcut) and validate against the v3 schema.
        val migrated = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_1_2, MIGRATION_2_3)

        // Pre-existing data survived the rebuild-heavy MIGRATION_1_2 (markets table was recreated).
        migrated.query("SELECT name, dateEpochDay FROM markets WHERE id = 1").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Spring Fair")
            assertThat(cursor.getLong(1)).isEqualTo(19100L)
        }
        migrated.query("SELECT name FROM patterns WHERE id = 1").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Amigurumi Fox")
        }
        migrated.query("SELECT colourwayId, completedAtEpochDay FROM completions WHERE id = 1").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(1L)
        }

        // New v2 reflection columns exist, default to null/false, and don't block existing rows.
        migrated.query(
            "SELECT reflectionPrompted, attended, howItWent FROM markets WHERE id = 1",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(0)
            assertThat(cursor.isNull(1)).isTrue()
            assertThat(cursor.isNull(2)).isTrue()
        }

        // A market's name is nullable post-migration (the whole point of the v1->v2 rebuild).
        migrated.execSQL(
            "INSERT INTO markets (id, name, dateEpochDay, isSkipped, createdAt, reflectionPrompted) " +
                "VALUES (2, NULL, 19200, 0, 2000, 0)",
        )
        migrated.query("SELECT name FROM markets WHERE id = 2").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.isNull(0)).isTrue()
        }

        // The v3 market_todos table exists and is usable.
        migrated.execSQL(
            "INSERT INTO market_todos (id, marketId, text, isDone, createdAt) " +
                "VALUES (1, 1, 'Pack yarn', 0, 3000)",
        )
        migrated.query("SELECT text FROM market_todos WHERE id = 1").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Pack yarn")
        }
    }
}
