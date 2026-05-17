package com.example.deviceinfo.feature.benchmark

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Shows last N benchmark runs as a bar+line combo chart.
 * ST = single-threaded (blue), MT = multi-threaded (purple).
 * Pure Canvas. Play Store safe.
 */
class BenchmarkHistoryChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class RunResult(val stScore: Int, val mtScore: Int, val label: String)

    private val history = mutableListOf<RunResult>()
    private val MAX_RUNS = 5

    private val stBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E88E5.toInt(); style = Paint.Style.FILL
    }
    private val mtBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); style = Paint.Style.FILL
    }
    private val stLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E88E5.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val mtLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22000000; strokeWidth = 1f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF607D8B.toInt(); textSize = 22f; textAlign = Paint.Align.CENTER
    }
    private val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF37474F.toInt(); textSize = 20f; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f; typeface = Typeface.DEFAULT_BOLD
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); textSize = 28f; typeface = Typeface.DEFAULT_BOLD
    }

    fun addResult(result: RunResult) {
        history.add(result)
        if (history.size > MAX_RUNS) history.removeAt(0)
        invalidate()
    }

    fun clearHistory() { history.clear(); invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 12f; val padR = 12f; val padT = 50f; val padB = 36f
        val chartW = w - padL - padR; val chartH = h - padT - padB

        // Title + legend
        canvas.drawText("Score History", padL + 6f, padT - 24f, titlePaint)
        legendPaint.color = 0xFF1E88E5.toInt()
        canvas.drawText("● ST", padL + titlePaint.measureText("Score History") + 20f, padT - 24f, legendPaint)
        legendPaint.color = 0xFF7B2FBE.toInt()
        canvas.drawText("● MT", padL + titlePaint.measureText("Score History") + 80f, padT - 24f, legendPaint)

        if (history.isEmpty()) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("No runs yet — tap Run Benchmark", padL + 8f, padT + chartH / 2f, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
            return
        }

        // Axes
        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        val maxScore = (history.flatMap { listOf(it.stScore, it.mtScore) }.maxOrNull() ?: 1000)
            .let { (it * 1.15).toInt() }  // 15% headroom

        // Grid lines
        for (i in 0..3) {
            val y = padT + (i / 3f) * chartH
            axisPaint.alpha = 25
            canvas.drawLine(padL, y, padL + chartW, y, axisPaint)
            axisPaint.alpha = 255
        }

        val n = history.size
        val slotW = chartW / MAX_RUNS.toFloat()
        val barW = slotW * 0.25f

        // Draw bars + line points
        val stPoints = mutableListOf<Pair<Float, Float>>()
        val mtPoints = mutableListOf<Pair<Float, Float>>()

        history.forEachIndexed { i, run ->
            // Center of slot for this run (right-aligned in the 5-slot window)
            val slotStart = padL + (MAX_RUNS - n + i) * slotW
            val cx = slotStart + slotW / 2f

            val stH = (run.stScore.toFloat() / maxScore) * chartH
            val mtH = (run.mtScore.toFloat() / maxScore) * chartH

            val stY = padT + chartH - stH
            val mtY = padT + chartH - mtH

            // ST bar (left of center)
            val rx = 6f
            canvas.drawRoundRect(cx - barW - 2f, stY, cx - 2f, padT + chartH, rx, rx, stBarPaint)
            // MT bar (right of center)
            canvas.drawRoundRect(cx + 2f, mtY, cx + barW + 2f, padT + chartH, rx, rx, mtBarPaint)

            // Value labels above bars
            if (stH > 30f) {
                valPaint.color = 0xFF1E88E5.toInt()
                canvas.drawText(fmtK(run.stScore), cx - barW / 2f - 2f, stY - 4f, valPaint)
            }
            if (mtH > 30f) {
                valPaint.color = 0xFF7B2FBE.toInt()
                canvas.drawText(fmtK(run.mtScore), cx + barW / 2f + 2f, mtY - 4f, valPaint)
            }

            // Run label
            canvas.drawText(run.label, cx, padT + chartH + 26f, textPaint)

            stPoints.add(cx to stY)
            mtPoints.add(cx to mtY)
        }

        // Draw connecting lines
        if (stPoints.size >= 2) {
            drawLine(canvas, stPoints, stLinePaint, 0xFF1E88E5.toInt())
            drawLine(canvas, mtPoints, mtLinePaint, 0xFF7B2FBE.toInt())
        }
    }

    private fun drawLine(canvas: Canvas, pts: List<Pair<Float, Float>>, paint: Paint, dotColor: Int) {
        val path = Path()
        pts.forEachIndexed { i, (x, y) ->
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
        dotPaint.color = dotColor
        pts.forEach { (x, y) -> canvas.drawCircle(x, y, 7f, dotPaint) }
    }

    private fun fmtK(score: Int): String =
        if (score >= 1000) "${"%.1f".format(score / 1000f)}K" else "$score"
}
