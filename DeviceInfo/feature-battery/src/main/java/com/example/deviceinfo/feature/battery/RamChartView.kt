package com.example.deviceinfo.feature.battery

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Lightweight in-session RAM usage line chart (used RAM %).
 * Same canvas-based approach as BatteryChartView — no external library.
 * Max 60 data points kept (≈ 2 minutes at 2s interval).
 */
class RamChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dataPoints = mutableListOf<Int>()   // used RAM %

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2196F3.toInt()   // blue
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap  = Paint.Cap.ROUND
    }
    private val fillPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2196F3.toInt(); style = Paint.Style.FILL
    }
    private val textPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt(); textSize = 26f
    }
    private val axisPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000; strokeWidth = 1f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2196F3.toInt(); textSize = 24f; typeface = Typeface.DEFAULT_BOLD
    }

    fun addDataPoint(usedPercent: Int) {
        dataPoints.add(usedPercent.coerceIn(0, 100))
        if (dataPoints.size > MAX_POINTS) dataPoints.removeAt(0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padL = 48f; val padR = 16f; val padT = 12f; val padB = 24f
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        // Label
        canvas.drawText("RAM", padL + 4f, padT + 18f, labelPaint)

        // Axes
        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        // Y labels
        for (pct in listOf(0, 50, 100)) {
            val y = padT + chartH - (pct / 100f) * chartH
            canvas.drawText("$pct%", 0f, y + 8f, textPaint)
            canvas.drawLine(padL, y, padL + chartW, y, axisPaint.apply { alpha = 30 })
        }
        axisPaint.alpha = 255

        if (dataPoints.size < 2) {
            canvas.drawText("Collecting data…", padL + chartW / 2f - 80f, padT + chartH / 2f, textPaint)
            return
        }

        val path = Path(); val fillPath = Path()
        dataPoints.forEachIndexed { i, pct ->
            val x = padL + i * (chartW / (MAX_POINTS - 1).toFloat())
            val y = padT + chartH - (pct / 100f) * chartH
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, padT + chartH); fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y); fillPath.lineTo(x, y)
            }
        }
        val lastX = padL + (dataPoints.size - 1) * (chartW / (MAX_POINTS - 1).toFloat())
        fillPath.lineTo(lastX, padT + chartH); fillPath.close()

        fillPaint.shader = LinearGradient(0f, padT, 0f, padT + chartH,
            0x552196F3, 0x002196F3, Shader.TileMode.CLAMP)
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        val lastPct = dataPoints.last()
        val lx = padL + (dataPoints.size - 1) * (chartW / (MAX_POINTS - 1).toFloat())
        val ly = padT + chartH - (lastPct / 100f) * chartH
        canvas.drawCircle(lx, ly, 5f, dotPaint)
        canvas.drawText("$lastPct%", (lx + 8f).coerceAtMost(padL + chartW - 50f), ly + 8f, textPaint)
    }

    companion object { private const val MAX_POINTS = 60 }
}
