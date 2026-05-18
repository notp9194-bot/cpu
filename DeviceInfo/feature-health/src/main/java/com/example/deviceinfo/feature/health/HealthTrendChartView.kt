package com.example.deviceinfo.feature.health

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * 7-Day Health Score Trend Line Chart.
 * FIX: All dp() calls moved OUT of field initializers → into onDraw/onMeasure
 *      to avoid NullPointerException on resources before view is attached.
 */
class HealthTrendChartView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private var entries: List<HealthScoreHistory.Entry> = emptyList()

    fun setEntries(data: List<HealthScoreHistory.Entry>) {
        entries = data
        invalidate()
    }

    // ── Paints (NO dp() calls here — resources not ready yet) ────────
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin= Paint.Join.ROUND
    }
    private val fillPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint        = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.WHITE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFBBBBBB.toInt(); textAlign = Paint.Align.CENTER
    }
    private val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0x33FFFFFF
    }
    private val zoneLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }
    private val noDataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt(); textAlign = Paint.Align.CENTER
    }

    private data class Zone(val minScore: Int, val label: String, val color: Int)
    private val zones = listOf(
        Zone(90, "Excellent", 0xFF4CAF50.toInt()),
        Zone(70, "Good",      0xFF8BC34A.toInt()),
        Zone(50, "Fair",      0xFFFFEB3B.toInt()),
        Zone(30, "Poor",      0xFFFF9800.toInt()),
        Zone(0,  "Critical",  0xFFF44336.toInt())
    )

    // dp() is safe to call only during draw/measure (resources available)
    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cw = width.toFloat()
        val ch = height.toFloat()
        if (cw <= 0f || ch <= 0f) return

        // Compute paddings here (safe — resources available during draw)
        val padL = dp(40f); val padR = dp(16f)
        val padT = dp(28f); val padB = dp(32f)

        // Update paint sizes that need dp
        linePaint.strokeWidth    = dp(2.5f)
        dotOutlinePaint.strokeWidth = dp(2f)
        labelPaint.textSize      = dp(10f)
        scoreLabelPaint.textSize = dp(9.5f)
        gridPaint.strokeWidth    = dp(0.8f)
        gridPaint.pathEffect     = DashPathEffect(floatArrayOf(dp(6f), dp(4f)), 0f)
        zoneLabelPaint.textSize  = dp(8f)
        noDataPaint.textSize     = dp(13f)

        val chartLeft   = padL
        val chartRight  = cw - padR
        val chartTop    = padT
        val chartBottom = ch - padB
        val chartW      = (chartRight - chartLeft).coerceAtLeast(1f)
        val chartH      = (chartBottom - chartTop).coerceAtLeast(1f)

        if (entries.isEmpty()) {
            canvas.drawText(
                "No history yet — scores will appear daily",
                cw / 2f, ch / 2f + dp(5f), noDataPaint
            )
            return
        }

        // Grade zone dashed lines
        listOf(90, 70, 50, 30).forEach { score ->
            val y = scoreToY(score.toFloat(), chartTop, chartH)
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            zoneLabelPaint.color = scoreColor(score.toFloat())
            zoneLabelPaint.alpha = 140
            canvas.drawText(gradeLabel(score), chartLeft + dp(2f), y - dp(2f), zoneLabelPaint)
        }

        // Point positions
        val n     = entries.size
        val xStep = if (n > 1) chartW / (n - 1).toFloat() else chartW / 2f
        val points = entries.mapIndexed { i, e ->
            val x = if (n == 1) chartLeft + chartW / 2f else chartLeft + i * xStep
            PointF(x, scoreToY(e.score.toFloat(), chartTop, chartH))
        }

        // Gradient fill
        val avgScore = entries.map { it.score }.average().toFloat()
        val topColor = scoreColor(avgScore)
        fillPaint.shader = LinearGradient(
            0f, chartTop, 0f, chartBottom,
            intArrayOf((topColor and 0x00FFFFFF) or 0x66000000, Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        val fillPath = Path().apply {
            moveTo(points.first().x, chartBottom)
            lineTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val cp1x = (points[i-1].x + points[i].x) / 2f
                cubicTo(cp1x, points[i-1].y, cp1x, points[i].y, points[i].x, points[i].y)
            }
            lineTo(points.last().x, chartBottom)
            close()
        }
        canvas.drawPath(fillPath, fillPaint)

        // Line with gradient
        linePaint.shader = LinearGradient(
            chartLeft, 0f, chartRight, 0f,
            points.map { p -> scoreColor(yToScore(p.y, chartTop, chartH)) }.toIntArray(),
            points.map { p -> (p.x - chartLeft) / chartW }.toFloatArray(),
            Shader.TileMode.CLAMP
        )
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val cp1x = (points[i-1].x + points[i].x) / 2f
                cubicTo(cp1x, points[i-1].y, cp1x, points[i].y, points[i].x, points[i].y)
            }
        }
        canvas.drawPath(linePath, linePaint)

        // Dots + labels
        val dotR = dp(5f)
        points.forEachIndexed { i, p ->
            val sc = entries[i].score
            val dc = scoreColor(sc.toFloat())
            dotPaint.color = dc
            canvas.drawCircle(p.x, p.y, dotR, dotPaint)
            canvas.drawCircle(p.x, p.y, dotR, dotOutlinePaint)
            scoreLabelPaint.color = dc
            canvas.drawText("$sc", p.x, p.y - dp(9f), scoreLabelPaint)
            canvas.drawText(entries[i].dateLabel, p.x, chartBottom + dp(14f), labelPaint)
        }
    }

    private fun scoreToY(score: Float, top: Float, h: Float) =
        top + h - (score.coerceIn(0f, 100f) / 100f) * h

    private fun yToScore(y: Float, top: Float, h: Float) =
        ((top + h - y) / h * 100f).coerceIn(0f, 100f)

    private fun scoreColor(score: Float) = when {
        score >= 90 -> 0xFF4CAF50.toInt()
        score >= 70 -> 0xFF8BC34A.toInt()
        score >= 50 -> 0xFFFFEB3B.toInt()
        score >= 30 -> 0xFFFF9800.toInt()
        else         -> 0xFFF44336.toInt()
    }

    private fun gradeLabel(score: Int) = when {
        score >= 90 -> "Excellent"
        score >= 70 -> "Good"
        score >= 50 -> "Fair"
        else         -> "Poor"
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val defH = (resources.displayMetrics.density * 180f).toInt()
        val w = MeasureSpec.getSize(wSpec)
        val h = when (MeasureSpec.getMode(hSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(hSpec)
            MeasureSpec.AT_MOST -> minOf(defH, MeasureSpec.getSize(hSpec))
            else                 -> defH
        }
        setMeasuredDimension(w, h)
    }
}
