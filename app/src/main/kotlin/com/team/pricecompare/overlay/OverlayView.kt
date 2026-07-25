package com.team.pricecompare.overlay

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.team.pricecompare.Morandi
import com.team.pricecompare.data.StoreInfo
import com.team.pricecompare.data.UserDealInput
import com.team.pricecompare.engine.match.NamePair
import com.team.pricecompare.engine.match.ProductMatcher
import com.team.pricecompare.engine.strategy.StrategyRecommender

class OverlayView(context: Context) : LinearLayout(context) {

    private val header: TextView
    private val collapseBtn: Button
    private val editBtn: Button
    private val rowsContainer: LinearLayout
    private val bestLabel: TextView
    private val redPacketInput: EditText
    private val confirmBtn: Button
    private val footer: TextView

    private var stores: List<StoreInfo> = emptyList()
    private var redPacket: Double = 0.0
    private var collapsed: Boolean = false
    private var editMode: Boolean = false
    private var serviceEnabled: Boolean = true
    private var confirmedPairs: Set<NamePair> = emptySet()
    private var pendingPairs: List<NamePair> = emptyList()

    private var downX = 0f
    private var downY = 0f

    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null
    var onToggleEditMode: ((focusable: Boolean) -> Unit)? = null
    var onConfirmPending: ((List<NamePair>) -> Unit)? = null

    init {
        orientation = VERTICAL
        setPadding(32, 24, 32, 24)
        background = Morandi.card(context, Morandi.overlayBg, 22f, Morandi.overlayStroke, 0.5f)

        val headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header = TextView(context).apply {
            text = "外卖比价"
            setTextColor(Morandi.overlayText)
            textSize = 14f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        editBtn = compactButton("✎") { toggleEditMode() }
        collapseBtn = compactButton("—") { toggleCollapse() }
        headerRow.addView(header)
        headerRow.addView(editBtn)
        headerRow.addView(collapseBtn)
        addView(headerRow)

        rowsContainer = LinearLayout(context).apply { orientation = VERTICAL }
        bestLabel = TextView(context).apply {
            setTextColor(Morandi.overlayBest)
            textSize = 13f
            setPadding(0, 12, 0, 0)
        }
        redPacketInput = EditText(context).apply {
            hint = "红包金额"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 13f
            setTextColor(Morandi.overlayText)
            setHintTextColor(Morandi.overlaySub)
            background = Morandi.card(context, Color.parseColor("#33ECE6DC"), 8f)
            setPadding(20, 14, 20, 14)
            isFocusableInTouchMode = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 14
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val v = s?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
                    redPacket = v
                    renderRows()
                }
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })
        }
        confirmBtn = Button(context).apply {
            text = "确认匹配"
            setTextColor(Morandi.overlayOnAccent)
            background = Morandi.card(context, Morandi.overlayBest, 8f)
            visibility = View.GONE
            setOnClickListener { onConfirmPending?.invoke(pendingPairs) }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10
            }
        }
        footer = TextView(context).apply {
            text = "M2 · fixtures 假数据"
            setTextColor(Morandi.overlaySub)
            textSize = 10f
            setPadding(0, 12, 0, 0)
        }
        addView(rowsContainer)
        addView(bestLabel)
        addView(redPacketInput)
        addView(confirmBtn)
        addView(footer)
    }

    private fun compactButton(label: String, onClick: () -> Unit): Button =
        Button(context).apply {
            text = label
            setTextColor(Morandi.overlayText)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { onClick() }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = 10
            }
            minWidth = 0
            minimumWidth = 0
        }

    fun setStores(list: List<StoreInfo>) {
        stores = list
        renderRows()
    }

    fun setServiceEnabled(enabled: Boolean) {
        serviceEnabled = enabled
        renderRows()
    }

    fun setConfirmed(pairs: Set<NamePair>) {
        confirmedPairs = pairs
        renderRows()
    }

    private fun bodyVisibility(): Int = if (collapsed) View.GONE else View.VISIBLE

    private fun renderRows() {
        rowsContainer.removeAllViews()

        if (!serviceEnabled) {
            rowsContainer.visibility = View.GONE
            redPacketInput.visibility = View.GONE
            confirmBtn.visibility = View.GONE
            footer.visibility = View.GONE
            bestLabel.visibility = View.VISIBLE
            bestLabel.text = "请先开启无障碍服务"
            bestLabel.setTextColor(Morandi.overlayWarn)
            return
        }
        bestLabel.setTextColor(Morandi.overlayBest)

        val vis = bodyVisibility()
        rowsContainer.visibility = vis
        bestLabel.visibility = vis
        redPacketInput.visibility = vis
        footer.visibility = vis

        if (stores.isEmpty()) {
            bestLabel.text = "暂无数据"
            confirmBtn.visibility = View.GONE
            return
        }

        val strategy = StrategyRecommender.recommend(stores, UserDealInput(redPacket = redPacket))
        for (deal in strategy.perPlatform) {
            val isBest = deal.platform == strategy.bestPlatform
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16, 10, 16, 10)
                if (isBest) background = Morandi.card(context, Morandi.overlayBest, 8f)
            }
            val name = TextView(context).apply {
                text = platformLabel(deal.platform)
                setTextColor(if (isBest) Morandi.overlayOnAccent else Morandi.overlayText)
                textSize = 13f
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            val price = TextView(context).apply {
                text = "¥" + "%.2f".format(deal.finalPrice)
                setTextColor(if (isBest) Morandi.overlayOnAccent else Morandi.overlayPrice)
                textSize = 13f
            }
            val tag = TextView(context).apply {
                text = if (isBest) " 最优" else ""
                setTextColor(Morandi.overlayOnAccent)
                textSize = 11f
            }
            row.addView(name)
            row.addView(price)
            row.addView(tag)
            rowsContainer.addView(row)
        }

        bestLabel.text = strategy.reason
        renderMatchSummary()
    }

    private fun renderMatchSummary() {
        val meituan = stores.firstOrNull { it.platform == "meituan" }
        val flash = stores.firstOrNull { it.platform == "flash" }
        if (meituan == null || flash == null) {
            pendingPairs = emptyList()
            confirmBtn.visibility = View.GONE
            footer.text = "M2 · fixtures 假数据"
            return
        }
        val matches = ProductMatcher.match(meituan, flash, confirmedPairs)
        val auto = matches.count { it.matched && !it.needsConfirm }
        val pending = matches.filter { it.needsConfirm }
        val unmatched = matches.size - auto - pending.size

        pendingPairs = pending.mapNotNull { m ->
            val a = m.itemA?.name ?: return@mapNotNull null
            val b = m.itemB?.name ?: return@mapNotNull null
            NamePair(a, b)
        }
        footer.text = "匹配: 自动 $auto / 待确认 ${pending.size} / 未配 $unmatched"
        confirmBtn.visibility = if (pendingPairs.isEmpty()) View.GONE else View.VISIBLE
        confirmBtn.text = "确认 ${pendingPairs.size} 项匹配"
    }

    private fun toggleCollapse() {
        collapsed = !collapsed
        collapseBtn.text = if (collapsed) "+" else "—"
        renderRows()
    }

    private fun toggleEditMode() {
        editMode = !editMode
        editBtn.text = if (editMode) "✓" else "✎"
        if (editMode) {
            redPacketInput.requestFocus()
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(redPacketInput, InputMethodManager.SHOW_IMPLICIT)
        }
        onToggleEditMode?.invoke(editMode)
    }

    private fun platformLabel(platform: String): String = when (platform) {
        "meituan" -> "美团"
        "flash" -> "淘宝闪购"
        else -> platform
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - downX).toInt()
                val dy = (event.rawY - downY).toInt()
                downX = event.rawX
                downY = event.rawY
                onDrag?.invoke(dx, dy)
            }
        }
        return true
    }
}
