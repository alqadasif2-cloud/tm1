package com.example.tforwarder

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        TgClient.init(this)
    }
}
