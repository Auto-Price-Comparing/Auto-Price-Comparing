package com.team.pricecompare.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.team.pricecompare.Morandi
import com.team.pricecompare.data.StoreInfo
import com.team.pricecompare.data.UserDealInput
import com.team.pricecompare.data.repo.HistoryPoint
import com.team.pricecompare.engine.strategy.StrategyRecommender

class MerchantAnalysisView(context: Context) : LinearLayout(context) {

    private val compareContainer: LinearLayout
    private val strategyLabel: TextView
    private val chart: ChartView
    private val recordBtn: Button

    private var stores: List<StoreInfo> = emptyList()

    var onRecordSnapshot: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        setPadding(48, 96, 48, 48)
        setBackgroundColor(Morandi.screenBg)

        addView(TextView(context).apply {
            text = "商家分析"
            setTextColor(Morandi.textMain)
            textSize = 20f
            setPadding(0, 0, 0, 20)
        })

        compareContainer = LinearLayout(context).apply { orientation = VERTICAL }
        addView(compareContainer)

        strategyLabel = TextView(context).apply {
            setTextColor(Morandi.bestText)
            textSize = 14f
            setPadding(0, 24, 0, 20)
        }
        addView(strategyLabel)

        addView(TextView(context).apply {
            text = "历史参考价（商品+包装+配送，近 30 条）"
            setTextColor(Morandi.textSub)
            textSize = 12f
        })
        chart = ChartView(context).apply {
            background = Morandi.card(context, Morandi.surface, 12f)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0).apply {
                height = 0
                weight = 1f
                topMargin = 12
            }
        }
        addView(chart)

        recordBtn = Button(context).apply {
            text = "记录当前快照"
            background = Morandi.card(context, Morandi.surface, 12f)
            setTextColor(Morandi.textMain)
            setOnClickListener { onRecordSnapshot?.invoke() }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 24
            }
        }
        addView(recordBtn)
    }

    fun setStores(list: List<StoreInfo>) {
        stores = list
        renderCompare()
    }

    fun setHistory(points: List<HistoryPoint>) {
        chart.setPoints(points)
    }

    private fun renderCompare() {
        compareContainer.removeAllViews()
        if (stores.isEmpty()) {
            strategyLabel.text = "暂无数据"
            return
        }
        val strategy = StrategyRecommender.recommend(stores, UserDealInput())
        val bestPlatform = strategy.bestPlatform

        compareContainer.addView(row("平台", "评分", "月售", "配送", "起送", header = true, best = false))
        for (s in stores) {
            compareContainer.addView(
                row(
                    platformLabel(s.platform),
                    "%.1f".format(s.rating),
                    s.monthlySales.toString(),
                    "¥" + "%.0f".format(s.deliveryFee),
                    "¥" + "%.0f".format(s.minOrder),
                    header = false,
                    best = s.platform == bestPlatform,
                )
            )
        }
        strategyLabel.text = strategy.reason
    }

    private fun row(
        a: String, b: String, c: String, d: String, e: String,
        header: Boolean, best: Boolean,
    ): android.view.View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(20, 16, 20, 16)
        val bg = when {
            best -> Morandi.card(context, Morandi.bestRow, 10f)
            header -> Morandi.card(context, Morandi.surface, 10f)
            else -> Morandi.card(context, Morandi.surface, 10f)
        }
        background = bg
        val color = when {
            best -> Morandi.bestText
            header -> Morandi.priceText
            else -> Morandi.textMain
        }
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 10
        }
        layoutParams = lp
        addView(cell(a, 1f, color, header))
        addView(cell(b, 1f, color, header))
        addView(cell(c, 1f, color, header))
        addView(cell(d, 1f, color, header))
        addView(cell(e, 1f, color, header))
    }

    private fun cell(text: String, weight: Float, color: Int, header: Boolean): TextView =
        TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = if (header) 12f else 13f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, weight)
        }

    private fun platformLabel(platform: String): String = when (platform) {
        "meituan" -> "美团"
        "flash" -> "淘宝闪购"
        else -> platform
    }
}
