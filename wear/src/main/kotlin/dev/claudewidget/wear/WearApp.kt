package dev.claudewidget.wear

import android.app.Application

class WearApp : Application() {

    lateinit var store: WearAccountStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = WearAccountStore(this)
    }

    companion object {
        lateinit var instance: WearApp
            private set
    }
}
