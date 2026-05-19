package com.example.deviceinfo.feature.memory

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Dual-line live RAM chart: Used RAM (blue) and Available RAM (green).
 * Shows last 60 data points (~2 minutes at 2-second polling).
 * Pure Canvas. No external library. Play Store safe.
 */
class MemoryLineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val usedPoints  = mutableListOf<Float>()   // used RAM in MB
    private val availPoints = mutableListOf<Float>()   // avail RAM in MB
    private var totalRamMb: Float = 1f

    private val usedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2196F3.toInt(); strokeWidth = 3f
        style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val availLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4CAF50.toInt(); strokeWidth = 3f
        style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val usedFillPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val availFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000; strokeWidth = 1f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF888888.toInt(); textSize = 24f }
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 24f; typeface = Typeface.DEFAULT_BOLD }
    private val dotUsed  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2196F3.toInt(); style = Paint.Style.FILL }
    private val dotAvail = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4CAF50.toInt(); style = Paint.Style.FILL }

    fun setTotalRam(totalMb: Float) { totalRamMb = totalMb.coerceAtLeast(1f) }

    fun addDataPoint(usedMb: Float, availMb: Float) {
        usedPoints.add(usedMb.coerceAtLeast(0f))
        availPoints.add(availMb.coerceAtLeast(0f))
        if (usedPoints.size > MAX_POINTS)  usedPoints.removeAt(0)
        if (availPoints.size > MAX_POINTS) availPoints.removeAt(0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 56f; val padR = 12f; val padT = 36f; val padB = 28f
        val chartW = w - padL - padR; val chartH = h - padT - padB

        // Legend
        legendPaint.color = 0xFF2196F3.toInt()
        canvas.drawText("● Used", padL, 26f, legendPaint)
        legendPaint.color = 0xFF4CAF50.toInt()
        canvas.drawText("● Free", padL + 100f, 26f, legendPaint)

        // Axes
        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        // Y grid lines
        for (step in 0..4) {
            val mb = totalRamMb * step / 4f
            val y  = padT + chartH - (mb / totalRamMb) * chartH
            val label = if (mb >= 1024f) "${"%.0f".format(mb / 1024f)}G" else "${"%.0f".format(mb)}M"
            canvas.drawText(label, 0f, y + 8f, textPaint)
            canvas.drawLine(padL, y, padL + chartW, y, axisPaint.apply { alpha = 25 })
        }
        axisPaint.alpha = 255

        if (usedPoints.size < 2) {
            canvas.drawText("Collecting data…", padL + chartW / 2 - 80f, padT + chartH / 2, textPaint)
            return
        }

        fun buildPaths(points: List<Float>): Pair<Path, Path> {
            val line = Path(); val fill = Path()
            points.forEachIndexed { i, mb ->
                val x = padL + i * (chartW / (MAX_POINTS - 1).toFloat())
                val y = padT + chartH - (mb / totalRamMb).coerceIn(0f, 1f) * chartH
                if (i == 0) { line.moveTo(x, y); fill.moveTo(x, padT + chartH); fill.lineTo(x, y) }
                else         { line.lineTo(x, y); fill.lineTo(x, y) }
            }
            val lastX = padL + (points.size - 1) * (chartW / (MAX_POINTS - 1).toFloat())
            fill.lineTo(lastX, padT + chartH); fill.close()
            return line to fill
        }

        // Avail fill (green, below)
        val (availLine, availFill) = buildPaths(availPoints)
        availFillPaint.shader = LinearGradient(0f, padT, 0f, padT + chartH, 0x404CAF50, 0x004CAF50, Shader.TileMode.CLAMP)
        canvas.drawPath(availFill, availFillPaint)
        canvas.drawPath(availLine, availLinePaint)

        // Used fill (blue, above)
        val (usedLine, usedFill) = buildPaths(usedPoints)
        usedFillPaint.shader = LinearGradient(0f, padT, 0f, padT + chartH, 0x402196F3, 0x002196F3, Shader.TileMode.CLAMP)
        canvas.drawPath(usedFill, usedFillPaint)
        canvas.drawPath(usedLine, usedLinePaint)

        // Live dots + labels
        fun dotAndLabel(points: List<Float>, dotP: Paint, lineP: Paint) {
            val last = points.last()
            val lx = padL + (points.size - 1) * (chartW / (MAX_POINTS - 1).toFloat())
            val ly = padT + chartH - (last / totalRamMb).coerceIn(0f, 1f) * chartH
            canvas.drawCircle(lx, ly, 6f, dotP)
            val label = if (last >= 1024f) "${"%.1f".format(last / 1024f)} GB" else "${"%.0f".format(last)} MB"
            textPaint.color = lineP.color
            canvas.drawText(label, (lx + 8f).coerceAtMost(padL + chartW - 70f), ly - 4f, textPaint)
            textPaint.color = 0xFF888888.toInt()
        }
        dotAndLabel(availPoints, dotAvail, availLinePaint)
        dotAndLabel(usedPoints,  dotUsed,  usedLinePaint)
    }

    companion object { private const val MAX_POINTS = 60 }
}
