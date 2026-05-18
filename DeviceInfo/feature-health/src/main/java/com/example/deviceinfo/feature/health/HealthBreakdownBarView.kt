package com.example.deviceinfo.feature.health

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Horizontal bar chart showing individual score components.
 * Each row: label | filled bar | pts/max
 */
class HealthBreakdownBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarItem(val label: String, val value: Int, val maxValue: Int, val color: Int)

    private var items: List<BarItem> = emptyList()
    private var animFraction = 0f

    private val bgBarPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF }
    private val fgBarPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = 0xFFCCCCCC.toInt()
        textSize = 28f
    }
    private val valuePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = 0xFF888888.toInt()
        textSize  = 26f
        textAlign = Paint.Align.RIGHT
    }

    private val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        interpolator = android.view.animation.DecelerateInterpolator()
        addUpdateListener { animFraction = it.animatedValue as Float; invalidate() }
    }

    fun setItems(list: List<BarItem>) {
        items = list
        animFraction = 0f
        animator.cancel()
        animator.start()
    }

    override fun onMeasure(w: Int, h: Int) {
        val rowH = 56
        val desired = rowH * items.size + 16
        setMeasuredDimension(
            resolveSize(w, w),
            resolveSize(desired, h)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) return
        val w = width.toFloat()
        val rowH = height.toFloat() / items.size
        val labelW = 200f
        val valW   = 80f
        val barX   = labelW + 8f
        val barW   = w - labelW - valW - 16f
        val barH   = rowH * 0.36f
        val radius  = barH / 2f

        items.forEachIndexed { i, item ->
            val cy  = i * rowH + rowH / 2f
            val barTop    = cy - barH / 2f
            val barBottom = cy + barH / 2f

            // Background bar
            canvas.drawRoundRect(barX, barTop, barX + barW, barBottom, radius, radius, bgBarPaint)

            // Filled portion
            val ratio = (item.value.toFloat() / item.maxValue.coerceAtLeast(1)) * animFraction
            val filled = (ratio * barW).coerceAtLeast(if (ratio > 0f) radius * 2 else 0f)
            fgBarPaint.color = item.color
            if (filled > 0f)
                canvas.drawRoundRect(barX, barTop, barX + filled, barBottom, radius, radius, fgBarPaint)

            // Label
            canvas.drawText(item.label, 0f, cy + 10f, labelPaint)

            // Value
            canvas.drawText("${item.value}/${item.maxValue}", w - 8f, cy + 10f, valuePaint)
        }
    }
}
