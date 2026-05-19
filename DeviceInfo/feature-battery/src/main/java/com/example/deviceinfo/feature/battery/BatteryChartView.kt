package com.example.deviceinfo.feature.battery

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Lightweight in-session battery % line chart.
 * No external library — pure Canvas drawing.
 * Max 60 data points kept (≈ 2 minutes at 2s interval).
 */
class BatteryChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dataPoints = mutableListOf<Int>()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt()   // primary purple
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap  = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt()
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt()
        textSize = 28f
        typeface = Typeface.DEFAULT
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        strokeWidth = 1f
    }

    fun addDataPoint(percent: Int) {
        dataPoints.add(percent)
        if (dataPoints.size > MAX_POINTS) dataPoints.removeAt(0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padL = 48f; val padR = 16f; val padT = 16f; val padB = 28f
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        // Axis lines
        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        // Y labels
        for (pct in listOf(0, 25, 50, 75, 100)) {
            val y = padT + chartH - (pct / 100f) * chartH
            canvas.drawText("$pct%", 0f, y + 9f, textPaint)
            canvas.drawLine(padL, y, padL + chartW, y, axisPaint.apply { alpha = 30 })
        }
        axisPaint.alpha = 255

        if (dataPoints.size < 2) {
            // Not enough data — show placeholder
            val msg = "Collecting data…"
            canvas.drawText(msg, padL + chartW / 2f - 80f, padT + chartH / 2f, textPaint)
            return
        }

        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { i, pct ->
            val x = padL + i * (chartW / (MAX_POINTS - 1).toFloat())
            val y = padT + chartH - (pct.coerceIn(0, 100) / 100f) * chartH
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, padT + chartH)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Fill gradient
        val lastX = padL + (dataPoints.size - 1) * (chartW / (MAX_POINTS - 1).toFloat())
        fillPath.lineTo(lastX, padT + chartH)
        fillPath.close()

        val shader = LinearGradient(0f, padT, 0f, padT + chartH,
            0x557B2FBE, 0x007B2FBE, Shader.TileMode.CLAMP)
        fillPaint.shader = shader
        canvas.drawPath(fillPath, fillPaint)

        // Line
        canvas.drawPath(path, linePaint)

        // Current value dot + label
        val lastPct = dataPoints.last()
        val lx = padL + (dataPoints.size - 1) * (chartW / (MAX_POINTS - 1).toFloat())
        val ly = padT + chartH - (lastPct.coerceIn(0, 100) / 100f) * chartH
        canvas.drawCircle(lx, ly, 6f, dotPaint)
        canvas.drawText("$lastPct%", (lx + 8f).coerceAtMost(padL + chartW - 60f), ly + 10f, textPaint)
    }

    companion object {
        private const val MAX_POINTS = 60
    }
}
