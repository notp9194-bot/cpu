package com.example.deviceinfo.feature.battery

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

/**
 * 24-hour persistent battery history chart.
 * Data comes from Room DB via BatteryFragment — no sensor/IO here.
 */
class BatteryHistoryChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class HistoryPoint(val timestamp: Long, val percent: Int, val isCharging: Boolean)

    private var data: List<HistoryPoint> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt(); textSize = 24f
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33AABBCC; strokeWidth = 1f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E5FF.toInt(); textSize = 26f; typeface = Typeface.DEFAULT_BOLD
    }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF607080.toInt(); textSize = 22f
    }

    fun setData(points: List<HistoryPoint>) {
        data = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 52f; val padR = 16f; val padT = 24f; val padB = 32f
        val chartW = w - padL - padR; val chartH = h - padT - padB

        canvas.drawText("24h Battery History", padL + 4f, padT - 6f, labelPaint)

        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        for (pct in listOf(0, 25, 50, 75, 100)) {
            val y = padT + chartH - (pct / 100f) * chartH
            canvas.drawText("$pct%", 2f, y + 8f, textPaint)
            canvas.drawLine(padL, y, padL + chartW, y, axisPaint)
        }

        if (data.size < 2) {
            canvas.drawText("Collecting history…", padL + 20f, padT + chartH / 2f, textPaint)
            return
        }

        val now = System.currentTimeMillis()
        val windowMs = 24 * 60 * 60 * 1000L
        val startMs  = now - windowMs

        val path     = Path(); val fillPath = Path()
        var firstPt  = true

        data.forEach { pt ->
            val xFrac = ((pt.timestamp - startMs).toFloat() / windowMs).coerceIn(0f, 1f)
            val x = padL + xFrac * chartW
            val y = padT + chartH - (pt.percent.coerceIn(0, 100) / 100f) * chartH
            val color = if (pt.isCharging) 0xFF00E5FF.toInt() else 0xFF7B2FBE.toInt()
            linePaint.color = color
            if (firstPt) {
                path.moveTo(x, y)
                fillPath.moveTo(x, padT + chartH); fillPath.lineTo(x, y)
                firstPt = false
            } else {
                path.lineTo(x, y); fillPath.lineTo(x, y)
            }
        }

        val lastPt = data.last()
        val lastXFrac = ((lastPt.timestamp - startMs).toFloat() / windowMs).coerceIn(0f, 1f)
        val lastX = padL + lastXFrac * chartW
        fillPath.lineTo(lastX, padT + chartH); fillPath.close()

        fillPaint.shader = LinearGradient(0f, padT, 0f, padT + chartH,
            0x557B2FBE, 0x007B2FBE, Shader.TileMode.CLAMP)
        canvas.drawPath(fillPath, fillPaint)
        linePaint.color = if (lastPt.isCharging) 0xFF00E5FF.toInt() else 0xFF7B2FBE.toInt()
        canvas.drawPath(path, linePaint)

        // Current dot + value
        val ly = padT + chartH - (lastPt.percent.coerceIn(0, 100) / 100f) * chartH
        dotPaint.color = linePaint.color
        canvas.drawCircle(lastX, ly, 7f, dotPaint)
        canvas.drawText("${lastPt.percent}%", (lastX + 10f).coerceAtMost(padL + chartW - 50f), ly + 9f, textPaint)

        // X-axis time labels (every 6 hours)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        for (h6 in 0..4) {
            val ms = startMs + h6 * 6 * 60 * 60 * 1000L
            val xPos = padL + (h6 / 4f) * chartW
            canvas.drawText(sdf.format(Date(ms)), xPos - 16f, padT + chartH + 22f, timePaint)
        }
    }
}
