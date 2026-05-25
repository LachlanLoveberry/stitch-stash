package com.lachlan.stitchstash.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lachlan.stitchstash.data.db.dao.AppSettingsDao
import com.lachlan.stitchstash.data.db.dao.ColourwayDao
import com.lachlan.stitchstash.data.db.dao.CompletionDao
import com.lachlan.stitchstash.data.db.dao.FinishCardDao
import com.lachlan.stitchstash.data.db.dao.MarketDao
import com.lachlan.stitchstash.data.db.dao.PatternDao
import com.lachlan.stitchstash.data.db.dao.ScenarioDao
import com.lachlan.stitchstash.data.db.dao.StickerDao
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.FinishCard
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.db.entities.Scenario
import com.lachlan.stitchstash.data.db.entities.Sticker

@Database(
    entities = [
        Pattern::class,
        Colourway::class,
        Completion::class,
        Market::class,
        AppSettings::class,
        Sticker::class,
        Scenario::class,
        FinishCard::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StitchStashDatabase : RoomDatabase() {
    abstract fun patternDao(): PatternDao
    abstract fun colourwayDao(): ColourwayDao
    abstract fun completionDao(): CompletionDao
    abstract fun marketDao(): MarketDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun stickerDao(): StickerDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun finishCardDao(): FinishCardDao

    companion object {
        fun build(context: Context): StitchStashDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                StitchStashDatabase::class.java,
                "stitch_stash.db",
            ).build()
    }
}
