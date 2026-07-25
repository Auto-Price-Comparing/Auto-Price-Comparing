package com.team.pricecompare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.team.pricecompare.Morandi
import com.team.pricecompare.data.db.AppDatabase
import com.team.pricecompare.data.repo.FakeCollectionOrchestrator
import com.team.pricecompare.data.repo.StoreRepository
import com.team.pricecompare.parsers.CollectionState
import com.team.pricecompare.overlay.OverlayController
import com.team.pricecompare.overlay.OverlayService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var homeRoot: LinearLayout
    private lateinit var analysisRoot: LinearLayout
    private lateinit var collectionRoot: LinearLayout
    private lateinit var analysisView: MerchantAnalysisView
    private lateinit var collectionView: CollectionView
    private lateinit var statusView: TextView

    private var currentStores: List<com.team.pricecompare.data.StoreInfo> = emptyList()
    private val orchestrator = FakeCollectionOrchestrator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildHomeRoot()
        buildAnalysisRoot()
        buildCollectionRoot()
        setContentView(homeRoot)

        val repo = StoreRepository.get(AppDatabase.get(this))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                OverlayController.accessibilityEnabled.collect { a11y ->
                    renderStatus(a11y)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.stores.collect { list ->
                    currentStores = list
                    analysisView.setStores(list)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.historyFor(DEMO_STORE, 30).collect { pts ->
                    analysisView.setHistory(pts)
                }
            }
        }
        lifecycleScope.launch { repo.seedIfEmpty() }
    }

    override fun onResume() {
        super.onResume()
        OverlayController.refresh(this)
        OverlayController.ensureService(this)
        renderStatus(OverlayController.accessibilityEnabled.value)
    }

    private fun buildHomeRoot() {
        statusView = TextView(this).apply {
            textSize = 14f
            setTextColor(Morandi.textMain)
            setPadding(48, 56, 48, 24)
        }
        val actionBtn = homeButton("启动悬浮窗") { onAction() }
        val analyzeBtn = homeButton("商家分析") { setContentView(analysisRoot) }
        val collectBtn = homeButton("一键全采（V2）") { setContentView(collectionRoot) }
        val batteryBtn = homeButton("关闭电池优化") { requestIgnoreBatteryOptimizations() }
        val info = TextView(this).apply {
            text = "外卖比价助手\n\n1) 授予悬浮窗权限\n2) 开启无障碍服务\n3) 打开美团店铺页看悬浮窗"
            textSize = 15f
            setTextColor(Morandi.textSub)
            setPadding(48, 24, 48, 48)
        }
        homeRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Morandi.screenBg)
            addView(statusView)
            addView(actionBtn)
            addView(analyzeBtn)
            addView(collectBtn)
            addView(batteryBtn)
            addView(info)
        }
    }

    private fun homeButton(text: String, onClick: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            this.text = text
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12 }
        }

    private fun buildAnalysisRoot() {
        analysisView = MerchantAnalysisView(this)
        analysisView.onRecordSnapshot = {
            lifecycleScope.launch {
                StoreRepository.get(AppDatabase.get(this@MainActivity)).recordAll(currentStores)
            }
        }
        val backBtn = MaterialButton(this).apply {
            text = "返回"
            setOnClickListener { setContentView(homeRoot) }
        }
        analysisRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Morandi.screenBg)
            addView(analysisView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            addView(backBtn)
        }
    }

    private fun buildCollectionRoot() {
        collectionView = CollectionView(this)
        collectionView.onCollect = { storeName ->
            lifecycleScope.launch {
                val repo = StoreRepository.get(AppDatabase.get(this@MainActivity))
                orchestrator.collect(storeName).collect { state ->
                    collectionView.render(state)
                    if (state is CollectionState.Completed) {
                        currentStores = state.stores
                        repo.recordAll(state.stores)
                    }
                }
            }
        }
        val backBtn = MaterialButton(this).apply {
            text = "返回"
            setOnClickListener { setContentView(homeRoot) }
        }
        collectionRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Morandi.screenBg)
            addView(collectionView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            addView(backBtn)
        }
    }

    private fun renderStatus(a11y: Boolean) {
        val overlay = Settings.canDrawOverlays(this)
        statusView.text = buildString {
            append("无障碍服务: ").append(if (a11y) "已开启" else "未开启")
            append('\n')
            append("悬浮窗权限: ").append(if (overlay) "已授予" else "未授予")
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

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            statusView.text = statusView.text.toString() + "\n电池优化：已豁免"
            return
        }
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    companion object {
        private const val DEMO_STORE = "老王盖码饭（示范店）"
    }
}
