package com.team.pricecompare.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.team.pricecompare.data.db.AppDatabase
import com.team.pricecompare.data.repo.MatchMemory
import com.team.pricecompare.data.repo.StoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var overlayView: OverlayView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        showOverlay()
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) return

        val view = OverlayView(this)
        overlayView = view

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 220
        }

        view.onDrag = { dx, dy ->
            params.x += dx
            params.y += dy
            windowManager.updateViewLayout(view, params)
        }
        view.onToggleEditMode = { focusable ->
            if (focusable) {
                params.flags = (params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()) or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            } else {
                params.flags = (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()) or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            windowManager.updateViewLayout(view, params)
        }

        windowManager.addView(view, params)

        val repo = StoreRepository.get(AppDatabase.get(this))
        val matchMemory = MatchMemory.get(AppDatabase.get(this))
        scope.launch {
            OverlayController.accessibilityEnabled.collect { enabled ->
                overlayView?.setServiceEnabled(enabled)
            }
        }
        scope.launch {
            repo.stores.collect { list ->
                overlayView?.setStores(list)
            }
        }
        scope.launch {
            matchMemory.confirmed.collect { pairs ->
                overlayView?.setConfirmed(pairs)
            }
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "比价悬浮窗",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("外卖比价助手")
            .setContentText("悬浮窗运行中")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
    }

    companion object {
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIF_ID = 1001
    }
}
