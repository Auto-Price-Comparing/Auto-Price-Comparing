package com.team.pricecompare.overlay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.team.pricecompare.parsers.CollectorAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OverlayController {

    private val _accessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var observer: ContentObserver? = null

    fun matchesEnabledService(raw: String?, expected: String): Boolean {
        if (raw.isNullOrBlank()) return false
        return raw.split(':').any { it.trim().equals(expected, ignoreCase = true) }
    }

    fun bind(context: Context) {
        val app = context.applicationContext
        if (observer == null) {
            val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (uri != null) {
                val o = object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean) {
                        refresh(app)
                    }
                }
                app.contentResolver.registerContentObserver(uri, false, o)
                observer = o
            }
        }
        refresh(app)
    }

    fun refresh(context: Context) {
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        _accessibilityEnabled.value = matchesEnabledService(raw, expectedComponent(context))
    }

    fun ensureService(context: Context) {
        refresh(context)
        val shouldRun = _accessibilityEnabled.value && Settings.canDrawOverlays(context)
        val intent = Intent(context, OverlayService::class.java)
        if (shouldRun) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }

    private fun expectedComponent(context: Context): String {
        val cls = CollectorAccessibilityService::class.java.name
        return ComponentName(context.packageName, cls).flattenToString()
    }
}
