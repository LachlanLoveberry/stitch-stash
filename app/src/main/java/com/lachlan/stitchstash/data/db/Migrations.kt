package com.lachlan.stitchstash.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // `name` changes from NOT NULL to nullable, which SQLite's ALTER TABLE can't express
        // directly, so the table is rebuilt alongside adding the new reflection columns.
        db.execSQL(
            """
            CREATE TABLE markets_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT,
                dateEpochDay INTEGER NOT NULL,
                isSkipped INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                reflectionPrompted INTEGER NOT NULL DEFAULT 0,
                attended INTEGER,
                howItWent TEXT,
                howItFelt TEXT,
                whatLearned TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO markets_new (id, name, dateEpochDay, isSkipped, createdAt)
            SELECT id, name, dateEpochDay, isSkipped, createdAt FROM markets
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE markets")
        db.execSQL("ALTER TABLE markets_new RENAME TO markets")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS market_todos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                marketId INTEGER NOT NULL,
                text TEXT NOT NULL,
                isDone INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_market_todos_marketId ON market_todos(marketId)")
    }
}
