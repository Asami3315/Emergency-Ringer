package com.emergencyringer.app

import android.app.Application

class EmergencyRingerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
    }
}
