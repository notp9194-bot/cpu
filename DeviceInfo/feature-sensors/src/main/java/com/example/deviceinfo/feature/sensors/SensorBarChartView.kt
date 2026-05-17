package com.example.deviceinfo.feature.sensors

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Horizontal bar chart showing sensor count by category.
 * Pure Canvas — no external library. No permissions needed.
 */
class SensorBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class SensorCategory(val label: String, val count: Int, val color: Int)

    private var categories: List<SensorCategory> = emptyList()

    private val barPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0x12000000 }
    private val lblPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF546E7A.toInt(); textSize = 26f }
    private val valPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 24f }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00897B.toInt(); textSize = 28f; typeface = Typeface.DEFAULT_BOLD
    }
    private val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF78909C.toInt(); textSize = 24f
    }

    fun update(cats: List<SensorCategory>) { categories = cats; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (categories.isEmpty()) return
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 140f; val padR = 54f; val padT = 38f; val padB = 8f
        val chartW = w - padL - padR
        val maxCount = categories.maxOf { it.count }.coerceAtLeast(1)
        val rowH = ((h - padT - padB) / categories.size).coerceAtLeast(24f)
        val barH = (rowH * 0.55f).coerceAtLeast(12f)
        val total = categories.sumOf { it.count }

        canvas.drawText("Sensors  ($total total)", padL, padT - 8f, titlePaint)

        categories.forEachIndexed { i, cat ->
            val y = padT + i * rowH + rowH / 2f
            val barW = (cat.count.toFloat() / maxCount) * chartW

            // label (left)
            lblPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(cat.label, padL - 8f, y + 9f, lblPaint)

            // track
            canvas.drawRoundRect(padL, y - barH / 2, padL + chartW, y + barH / 2, barH / 2, barH / 2, trackPaint)

            // bar
            if (barW > 2f) {
                barPaint.color = cat.color
                canvas.drawRoundRect(padL, y - barH / 2, padL + barW, y + barH / 2, barH / 2, barH / 2, barPaint)
            }

            // count value (right of bar)
            valPaint.color = cat.color
            canvas.drawText("${cat.count}", padL + barW + 8f, y + 9f, valPaint)
        }
    }
}
