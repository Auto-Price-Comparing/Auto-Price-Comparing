package com.team.pricecompare

import android.app.Application
import com.team.pricecompare.overlay.OverlayController

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        OverlayController.bind(this)
    }
}
