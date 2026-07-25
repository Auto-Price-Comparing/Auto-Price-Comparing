package com.team.pricecompare.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import android.view.View
import com.team.pricecompare.Morandi
import com.team.pricecompare.data.repo.HistoryPoint

class ChartView(context: Context) : View(context) {

    private var points: List<HistoryPoint> = emptyList()

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Morandi.divider
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Morandi.bestText
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#336E8B5E")
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Morandi.textSub
        textSize = dp(11f)
    }

    fun setPoints(pts: List<HistoryPoint>) {
        points = pts.sortedBy { it.capturedAt }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = dp(8f)
        canvas.drawRoundRect(pad, pad, w - pad, h - pad, dp(10f), dp(10f), axisPaint)
        if (points.size < 2) {
            labelPaint.color = Morandi.textSub
            canvas.drawText("暂无历史", pad + dp(6f), h / 2f, labelPaint)
            return
        }

        val prices = points.map { it.subtotal }
        val minP = prices.min()
        val maxP = prices.max()
        val span = (maxP - minP).coerceAtLeast(0.01)
        val left = pad * 2
        val right = w - pad
        val top = pad * 2
        val bottom = h - pad * 2

        fun xAt(i: Int) = left + (right - left) * i / (points.size - 1)
        fun yAt(p: Double) = (bottom - (bottom - top) * ((p - minP) / span)).toFloat()

        val fillPath = Path()
        fillPath.moveTo(xAt(0), bottom)
        for (i in points.indices) fillPath.lineTo(xAt(i), yAt(points[i].subtotal))
        fillPath.lineTo(xAt(points.size - 1), bottom)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        val linePath = Path()
        linePath.moveTo(xAt(0), yAt(points[0].subtotal))
        for (i in points.indices) linePath.lineTo(xAt(i), yAt(points[i].subtotal))
        canvas.drawPath(linePath, linePaint)

        canvas.drawText("¥%.2f".format(maxP), left, top, labelPaint)
        canvas.drawText("¥%.2f".format(minP), left, bottom, labelPaint)
    }

    private fun dp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
}
