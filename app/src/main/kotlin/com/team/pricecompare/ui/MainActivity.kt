package com.team.pricecompare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.team.pricecompare.overlay.OverlayController
import com.team.pricecompare.overlay.OverlayService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var actionBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView = TextView(this).apply {
            textSize = 14f
            setPadding(48, 48, 48, 24)
        }
        actionBtn = Button(this).apply { text = "启动悬浮窗" }
        actionBtn.setOnClickListener { onAction() }

        val info = TextView(this).apply {
            text = "外卖比价助手\n\n1) 授予悬浮窗权限\n2) 开启无障碍服务\n3) 打开美团店铺页看悬浮窗"
            textSize = 16f
            setPadding(48, 24, 48, 48)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(statusView)
            addView(actionBtn)
            addView(info)
        }
        setContentView(root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                OverlayController.accessibilityEnabled.collect { a11y ->
                    renderStatus(a11y)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OverlayController.refresh(this)
        OverlayController.ensureService(this)
        renderStatus(OverlayController.accessibilityEnabled.value)
    }

    private fun renderStatus(a11y: Boolean) {
        val overlay = Settings.canDrawOverlays(this)
        statusView.text = buildString {
            append("无障碍服务: ").append(if (a11y) "已开启" else "未开启")
            append('\n')
            append("悬浮窗权限: ").append(if (overlay) "已授予" else "未授予")
        }
        actionBtn.text = when {
            !a11y -> "开启无障碍服务"
            !overlay -> "授予悬浮窗权限"
            else -> "启动悬浮窗"
        }
    }

    private fun onAction() {
        val a11y = OverlayController.accessibilityEnabled.value
        when {
            !a11y -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            !Settings.canDrawOverlays(this) -> startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
            )
            else -> ContextCompat.startForegroundService(
                this,
                Intent(this, OverlayService::class.java),
            )
        }
    }
}
