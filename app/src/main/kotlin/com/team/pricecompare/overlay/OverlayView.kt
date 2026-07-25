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
import com.team.pricecompare.data.StoreInfo
import com.team.pricecompare.data.UserDealInput
import com.team.pricecompare.engine.ActualPriceCalculator

class OverlayView(context: Context) : LinearLayout(context) {

    private val header: TextView
    private val collapseBtn: Button
    private val editBtn: Button
    private val rowsContainer: LinearLayout
    private val bestLabel: TextView
    private val redPacketInput: EditText
    private val footer: TextView

    private var stores: List<StoreInfo> = emptyList()
    private var redPacket: Double = 0.0
    private var collapsed: Boolean = false
    private var editMode: Boolean = false
    private var serviceEnabled: Boolean = true

    private var downX = 0f
    private var downY = 0f

    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null
    var onToggleEditMode: ((focusable: Boolean) -> Unit)? = null

    init {
        orientation = VERTICAL
        setPadding(28, 22, 28, 22)
        setBackgroundColor(Color.parseColor("#CC111111"))

        val headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header = TextView(context).apply {
            text = "外卖比价"
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        editBtn = Button(context).apply {
            text = "✎"
            setOnClickListener { toggleEditMode() }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 12
            }
        }
        collapseBtn = Button(context).apply {
            text = "—"
            setOnClickListener { toggleCollapse() }
        }
        headerRow.addView(header)
        headerRow.addView(editBtn)
        headerRow.addView(collapseBtn)
        addView(headerRow)

        rowsContainer = LinearLayout(context).apply { orientation = VERTICAL }
        bestLabel = TextView(context).apply {
            setTextColor(Color.parseColor("#69F0AE"))
            textSize = 13f
            setPadding(0, 10, 0, 0)
        }
        redPacketInput = EditText(context).apply {
            hint = "红包金额"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 13f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#88FFFFFF"))
            isFocusableInTouchMode = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12
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
        footer = TextView(context).apply {
            text = "M1 · fixtures 假数据"
            setTextColor(Color.parseColor("#88FFFFFF"))
            textSize = 10f
            setPadding(0, 10, 0, 0)
        }
        addView(rowsContainer)
        addView(bestLabel)
        addView(redPacketInput)
        addView(footer)
    }

    fun setStores(list: List<StoreInfo>) {
        stores = list
        renderRows()
    }

    fun setServiceEnabled(enabled: Boolean) {
        serviceEnabled = enabled
        renderRows()
    }

    private fun bodyVisibility(): Int = if (collapsed) View.GONE else View.VISIBLE

    private fun renderRows() {
        rowsContainer.removeAllViews()

        if (!serviceEnabled) {
            rowsContainer.visibility = View.GONE
            redPacketInput.visibility = View.GONE
            footer.visibility = View.GONE
            bestLabel.visibility = View.VISIBLE
            bestLabel.text = "请先开启无障碍服务"
            bestLabel.setTextColor(Color.parseColor("#FF8A80"))
            return
        }
        bestLabel.setTextColor(Color.parseColor("#69F0AE"))

        val vis = bodyVisibility()
        rowsContainer.visibility = vis
        bestLabel.visibility = vis
        redPacketInput.visibility = vis
        footer.visibility = vis

        if (stores.isEmpty()) {
            bestLabel.text = "暂无数据"
            return
        }
        val deals = stores.map { it to ActualPriceCalculator.calculate(it, UserDealInput(redPacket = redPacket)) }
        val bestDeal = deals.minByOrNull { it.second.finalPrice }
        val bestPrice = bestDeal?.second?.finalPrice

        for ((store, deal) in deals) {
            val isBest = deal.finalPrice == bestPrice
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }
            val name = TextView(context).apply {
                text = platformLabel(store.platform)
                setTextColor(if (isBest) Color.parseColor("#69F0AE") else Color.WHITE)
                textSize = 13f
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            val price = TextView(context).apply {
                text = "¥" + "%.2f".format(deal.finalPrice)
                setTextColor(if (isBest) Color.parseColor("#69F0AE") else Color.parseColor("#FFD180"))
                textSize = 13f
            }
            val tag = TextView(context).apply {
                text = if (isBest) " 最优" else ""
                setTextColor(Color.parseColor("#69F0AE"))
                textSize = 11f
            }
            row.addView(name)
            row.addView(price)
            row.addView(tag)
            rowsContainer.addView(row)
        }

        bestLabel.text = bestDeal?.let { (s, d) ->
            "推荐: ${platformLabel(s.platform)} ¥" + "%.2f".format(d.finalPrice)
        } ?: ""
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
