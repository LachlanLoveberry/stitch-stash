package com.lachlan.stitchstash

import android.app.Application
import com.lachlan.stitchstash.data.db.StitchStashDatabase
import com.lachlan.stitchstash.data.repository.StitchRepository

class StitchStashApp : Application() {
    lateinit var database: StitchStashDatabase
        private set
    lateinit var repository: StitchRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = StitchStashDatabase.build(this)
        repository = StitchRepository(database)
    }
}
