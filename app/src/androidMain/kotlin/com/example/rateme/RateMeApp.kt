package com.example.rateme

import android.app.Application
import android.content.Context

class RateMeApp : Application() {
    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}
