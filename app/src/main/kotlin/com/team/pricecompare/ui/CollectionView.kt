package com.team.pricecompare.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.team.pricecompare.Morandi
import com.team.pricecompare.data.UserDealInput
import com.team.pricecompare.engine.strategy.StrategyRecommender
import com.team.pricecompare.parsers.CollectionState

class CollectionView(context: Context) : LinearLayout(context) {

    private val storeInput: EditText
    private val startBtn: MaterialButton
    private val progressView: TextView
    private val resultContainer: LinearLayout

    var onCollect: ((String) -> Unit)? = null

    init {
        orientation = VERTICAL
        setPadding(48, 96, 48, 48)
        setBackgroundColor(Morandi.screenBg)

        addView(TextView(context).apply {
            text = "一键全采（V2）"
            setTextColor(Morandi.textMain)
            textSize = 20f
            setPadding(0, 0, 0, 20)
        })
        storeInput = EditText(context).apply {
            hint = "店铺名"
            setText("老王盖码饭（示范店）")
            setTextColor(Morandi.textMain)
            setHintTextColor(Morandi.textSub)
            background = Morandi.card(context, Morandi.surface, 8f)
            setPadding(20, 14, 20, 14)
        }
        addView(storeInput)

        startBtn = MaterialButton(context).apply {
            text = "开始全采"
            setOnClickListener { onCollect?.invoke(storeInput.text.toString()) }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 20
            }
        }
        addView(startBtn)

        progressView = TextView(context).apply {
            setTextColor(Morandi.priceText)
            textSize = 14f
            setPadding(0, 24, 0, 8)
        }
        addView(progressView)

        resultContainer = LinearLayout(context).apply { orientation = VERTICAL }
        addView(resultContainer)
    }

    fun render(state: CollectionState) {
        when (state) {
            is CollectionState.InProgress -> {
                progressView.text = "${platformLabel(state.platform)}：${state.step}"
                startBtn.isEnabled = false
            }
            is CollectionState.Completed -> {
                progressView.text = "采集完成"
                startBtn.isEnabled = true
                renderResult(state.stores)
            }
            is CollectionState.Failed -> {
                progressView.text = "${platformLabel(state.platform)} 失败：${state.reason}"
                startBtn.isEnabled = true
            }
        }
    }

    private fun renderResult(stores: List<com.team.pricecompare.data.StoreInfo>) {
        resultContainer.removeAllViews()
        if (stores.isEmpty()) return
        val strategy = StrategyRecommender.recommend(stores, UserDealInput())
        for (deal in strategy.perPlatform) {
            val isBest = deal.platform == strategy.bestPlatform
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(20, 14, 20, 14)
                background = Morandi.card(
                    context,
                    if (isBest) Morandi.bestRow else Morandi.surface,
                    10f,
                )
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 10
                }
            }
            row.addView(TextView(context).apply {
                text = platformLabel(deal.platform)
                setTextColor(if (isBest) Morandi.bestText else Morandi.textMain)
                textSize = 14f
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(context).apply {
                text = "¥" + "%.2f".format(deal.finalPrice) + if (isBest) "  最优" else ""
                setTextColor(if (isBest) Morandi.bestText else Morandi.priceText)
                textSize = 14f
            })
            resultContainer.addView(row)
        }
        addReasonIfMissing(strategy.reason)
    }

    private fun addReasonIfMissing(reason: String) {
        val existing = resultContainer.findViewWithTag<TextView>("reason")
        if (existing == null) {
            resultContainer.addView(TextView(context).apply {
                tag = "reason"
                text = reason
                setTextColor(Morandi.bestText)
                textSize = 13f
                setPadding(0, 12, 0, 0)
            })
        } else {
            existing.text = reason
        }
    }

    private fun platformLabel(platform: String): String = when (platform) {
        "meituan" -> "美团"
        "flash" -> "淘宝闪购"
        else -> platform
    }
}
