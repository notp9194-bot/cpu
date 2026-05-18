package com.cpua.deviceinfo.feature.battery

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

/**
 * 24-hour persistent battery history line chart.
 * Canvas-only, no external library. Play Store safe ✅
 *
 * Color zones:
 *   ≥ 50% → green gradient
 *   20–49% → orange gradient
 *   < 20%  → red gradient
 *   (gradient transitions smoothly between segments)
 */
class BatteryHistoryChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var entries: List<BatteryHistoryManager.Entry> = emptyList()

    // Paints
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33888888; strokeWidth = 1.5f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1A888888; strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3.5f
        style  = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap  = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = 0xFF888888.toInt()
        textSize = 26f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = 0xFFCCCCCC.toInt()
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = 0xFF666666.toInt()
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val noDataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = 0xFF666666.toInt()
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun setEntries(list: List<BatteryHistoryManager.Entry>) {
        entries = list
        invalidate()
    }

    private fun colorForPct(pct: Int): Int = when {
        pct >= 50 -> 0xFF4CAF50.toInt()   // green
        pct >= 20 -> 0xFFFF9800.toInt()   // orange
        else       -> 0xFFF44336.toInt()  // red
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padL = 52f; val padR = 16f; val padT = 32f; val padB = 44f
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        // ── Background label ──────────────────────────────────────────
        labelPaint.alpha = 30
        canvas.drawText("24H BATTERY", w / 2f, padT + chartH / 2f + 12f, labelPaint)
        labelPaint.alpha = 255

        // ── Axes ──────────────────────────────────────────────────────
        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        // ── Y grid + labels ───────────────────────────────────────────
        for (pct in listOf(0, 25, 50, 75, 100)) {
            val y = padT + chartH - (pct / 100f) * chartH
            canvas.drawLine(padL, y, padL + chartW, y, gridPaint)
            canvas.drawText("$pct%", padL - 8f, y + 9f, textPaint.apply { textAlign = Paint.Align.RIGHT })
        }

        if (entries.size < 2) {
            canvas.drawText(
                if (entries.isEmpty()) "No history yet — data logs every 5 min"
                else "Collecting… (need 2+ points)",
                padL + chartW / 2f, padT + chartH / 2f, noDataPaint
            )
            return
        }

        val minTs = entries.first().timestampMs
        val maxTs = entries.last().timestampMs
        val tsRange = (maxTs - minTs).coerceAtLeast(1L)

        fun xOf(ts: Long)  = padL + ((ts - minTs).toFloat() / tsRange) * chartW
        fun yOf(pct: Int)  = padT + chartH - (pct.coerceIn(0, 100) / 100f) * chartH

        // ── Fill gradient (multi-color by avg pct) ────────────────────
        val avgPct = entries.map { it.percent }.average().toInt()
        val topCol = colorForPct(avgPct) and 0x00FFFFFF or 0x55000000
        val fillShader = LinearGradient(
            0f, padT, 0f, padT + chartH,
            topCol or colorForPct(avgPct), 0x00000000, Shader.TileMode.CLAMP
        )
        fillPaint.shader = fillShader

        val fillPath = Path()
        fillPath.moveTo(xOf(entries.first().timestampMs), padT + chartH)
        entries.forEach { e -> fillPath.lineTo(xOf(e.timestampMs), yOf(e.percent)) }
        fillPath.lineTo(xOf(entries.last().timestampMs), padT + chartH)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        // ── Colored line segments by pct zone ─────────────────────────
        for (i in 1 until entries.size) {
            val prev = entries[i - 1]; val curr = entries[i]
            val col = colorForPct((prev.percent + curr.percent) / 2)
            linePaint.color = col
            canvas.drawLine(xOf(prev.timestampMs), yOf(prev.percent),
                            xOf(curr.timestampMs), yOf(curr.percent), linePaint)
        }

        // ── X-axis time labels (up to 5 evenly spaced) ────────────────
        val labelCount = minOf(5, entries.size)
        val step = (entries.size - 1) / (labelCount - 1).coerceAtLeast(1)
        for (k in 0 until labelCount) {
            val idx = (k * step).coerceAtMost(entries.size - 1)
            val e   = entries[idx]
            val x   = xOf(e.timestampMs)
            canvas.drawText(timeFmt.format(Date(e.timestampMs)), x, padT + chartH + 32f, timePaint)
        }

        // ── Current value dot ─────────────────────────────────────────
        val last = entries.last()
        val lx   = xOf(last.timestampMs)
        val ly   = yOf(last.percent)
        val col  = colorForPct(last.percent)
        dotPaint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(lx, ly, 8f, dotPaint)
        dotPaint.color = col
        canvas.drawCircle(lx, ly, 6f, dotPaint)

        // Value label near dot
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = col
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 28f
        canvas.drawText("${last.percent}%",
            (lx + 12f).coerceAtMost(padL + chartW - 60f), ly - 8f, textPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = 0xFF888888.toInt()
        textPaint.textSize = 26f
    }
}
