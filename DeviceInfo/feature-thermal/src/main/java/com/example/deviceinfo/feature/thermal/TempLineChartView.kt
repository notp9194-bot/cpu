package com.cpua.deviceinfo.feature.thermal

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class TempLineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val data = mutableListOf<Float>()

    private val linePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF607D8B.toInt(); textSize = 24f }
    private val axisPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000; strokeWidth = 1f }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 28f; typeface = Typeface.DEFAULT_BOLD }
    private val warnPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55F44336; strokeWidth = 1.5f
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    fun addDataPoint(tempC: Float) {
        data.add(tempC)
        if (data.size > MAX_POINTS) data.removeAt(0)
        invalidate()
    }

    private fun colorForTemp(t: Float) = when {
        t >= 60f -> 0xFFF44336.toInt()
        t >= 45f -> 0xFFFF9800.toInt()
        else     -> 0xFF4CAF50.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 52f; val padR = 16f; val padT = 42f; val padB = 28f
        val chartW = w - padL - padR; val chartH = h - padT - padB

        val lastT = data.lastOrNull() ?: 0f
        val col = colorForTemp(lastT)
        titlePaint.color = col
        canvas.drawText("Temp  ${"%.1f".format(lastT)}\u00b0C", padL, padT - 8f, titlePaint)

        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        val maxT = 100f
        for (t in listOf(0, 25, 50, 75, 100)) {
            val y = padT + chartH - (t / maxT) * chartH
            canvas.drawText("${t}\u00b0", 0f, y + 9f, textPaint)
            axisPaint.alpha = 25
            canvas.drawLine(padL, y, padL + chartW, y, axisPaint)
            axisPaint.alpha = 255
        }

        val warnY = padT + chartH - (50f / maxT) * chartH
        canvas.drawLine(padL, warnY, padL + chartW, warnY, warnPaint)

        if (data.size < 2) {
            canvas.drawText("Collecting data\u2026", padL + 20f, padT + chartH / 2, textPaint)
            return
        }

        val path = Path(); val fill = Path()
        data.forEachIndexed { i, t ->
            val x = padL + i * (chartW / (MAX_POINTS - 1).toFloat())
            val y = padT + chartH - (t.coerceIn(0f, maxT) / maxT) * chartH
            if (i == 0) { path.moveTo(x, y); fill.moveTo(x, padT + chartH); fill.lineTo(x, y) }
            else { path.lineTo(x, y); fill.lineTo(x, y) }
        }
        val lx = padL + (data.size - 1) * (chartW / (MAX_POINTS - 1).toFloat())
        fill.lineTo(lx, padT + chartH); fill.close()

        fillPaint.color = (col and 0x00FFFFFF) or 0x44000000
        canvas.drawPath(fill, fillPaint)
        linePaint.color = col
        canvas.drawPath(path, linePaint)

        val ly = padT + chartH - (lastT.coerceIn(0f, maxT) / maxT) * chartH
        dotPaint.color = col
        canvas.drawCircle(lx, ly, 7f, dotPaint)
    }

    companion object { private const val MAX_POINTS = 60 }
}
