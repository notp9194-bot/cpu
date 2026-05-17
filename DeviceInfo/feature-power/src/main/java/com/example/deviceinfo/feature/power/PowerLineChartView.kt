package com.example.deviceinfo.feature.power

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Live wattage line chart over time.
 * Pure Canvas. Play Store safe.
 */
class PowerLineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val data = mutableListOf<Float>()
    private val MAX_POINTS = 60

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF9800.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF9800.toInt(); style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF607D8B.toInt(); textSize = 22f
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22000000; strokeWidth = 1f
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); textSize = 26f; typeface = Typeface.DEFAULT_BOLD
    }
    private val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF9800.toInt(); textSize = 22f; typeface = Typeface.DEFAULT_BOLD
    }

    fun addDataPoint(watts: Float) {
        data.add(watts)
        if (data.size > MAX_POINTS) data.removeAt(0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 48f; val padR = 16f; val padT = 38f; val padB = 24f
        val chartW = w - padL - padR; val chartH = h - padT - padB

        val cur = data.lastOrNull() ?: 0f
        canvas.drawText("Wattage History", padL, padT - 10f, titlePaint)
        valPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${"%.1f".format(cur)}W", padL + chartW, padT - 10f, valPaint)
        valPaint.textAlign = Paint.Align.LEFT

        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        val maxVal = (data.maxOrNull() ?: 0f).coerceAtLeast(5f) * 1.2f
        for (i in 0..3) {
            val y = padT + (i / 3f) * chartH
            val v = maxVal * (1f - i / 3f)
            canvas.drawText("${v.toInt()}W", 0f, y + 9f, textPaint)
            axisPaint.alpha = 20
            canvas.drawLine(padL, y, padL + chartW, y, axisPaint)
            axisPaint.alpha = 255
        }

        if (data.size < 2) {
            canvas.drawText("Collecting…", padL + 20f, padT + chartH / 2, textPaint)
            return
        }

        val path = Path(); val fill = Path()
        data.forEachIndexed { i, v ->
            val x = padL + i * (chartW / (MAX_POINTS - 1).toFloat())
            val y = padT + chartH - (v / maxVal).coerceIn(0f, 1f) * chartH
            if (i == 0) { path.moveTo(x, y); fill.moveTo(x, padT + chartH); fill.lineTo(x, y) }
            else        { path.lineTo(x, y); fill.lineTo(x, y) }
        }
        val lx = padL + (data.size - 1) * (chartW / (MAX_POINTS - 1).toFloat())
        fill.lineTo(lx, padT + chartH); fill.close()

        val shader = LinearGradient(0f, padT, 0f, padT + chartH,
            0x88FF9800.toInt(), 0x00FF9800, Shader.TileMode.CLAMP)
        fillPaint.shader = shader
        canvas.drawPath(fill, fillPaint)
        canvas.drawPath(path, linePaint)

        val ly = padT + chartH - (cur / maxVal).coerceIn(0f, 1f) * chartH
        canvas.drawCircle(lx, ly, 6f, dotPaint)
    }
}
