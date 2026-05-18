package com.example.deviceinfo.feature.health

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * 7-Day Health Score Trend Line Chart.
 *
 * Features:
 *  • Smooth cubic bezier line
 *  • Gradient fill under the line (score color → transparent)
 *  • Horizontal dashed grade zones (Excellent / Good / Fair / Poor)
 *  • Day labels on X-axis
 *  • Score dots with white outline
 *  • Score value text above each dot
 *  • "No data" placeholder when empty
 *
 * No third-party libs. Play Store safe ✅
 */
class HealthTrendChartView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    // ── Data ─────────────────────────────────────────────────────────
    private var entries: List<HealthScoreHistory.Entry> = emptyList()

    fun setEntries(data: List<HealthScoreHistory.Entry>) {
        entries = data
        invalidate()
    }

    // ── Dimensions ───────────────────────────────────────────────────
    private val padL  = dp(40f)
    private val padR  = dp(16f)
    private val padT  = dp(28f)
    private val padB  = dp(32f)

    // ── Paints ───────────────────────────────────────────────────────
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap   = Paint.Cap.ROUND
        strokeJoin  = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        color       = Color.WHITE
        strokeWidth = dp(2f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = 0xFFBBBBBB.toInt()
        textSize  = dp(10f)
        textAlign = Paint.Align.CENTER
    }

    private val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = dp(9.5f)
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.DEFAULT_BOLD
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = dp(0.8f)
        color       = 0x33FFFFFF
        pathEffect  = DashPathEffect(floatArrayOf(dp(6f), dp(4f)), 0f)
    }

    private val zoneLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = dp(8f)
        textAlign = Paint.Align.LEFT
    }

    private val noDataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = 0xFF888888.toInt()
        textSize  = dp(13f)
        textAlign = Paint.Align.CENTER
    }

    // ── Grade zones ──────────────────────────────────────────────────
    private data class Zone(val minScore: Int, val label: String, val color: Int)
    private val zones = listOf(
        Zone(90, "Excellent", 0xFF4CAF50.toInt()),
        Zone(70, "Good",      0xFF8BC34A.toInt()),
        Zone(50, "Fair",      0xFFFFEB3B.toInt()),
        Zone(30, "Poor",      0xFFFF9800.toInt()),
        Zone(0,  "Critical",  0xFFF44336.toInt())
    )

    // ── Draw ─────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cw = width.toFloat()
        val ch = height.toFloat()

        val chartLeft   = padL
        val chartRight  = cw - padR
        val chartTop    = padT
        val chartBottom = ch - padB
        val chartW      = chartRight - chartLeft
        val chartH      = chartBottom - chartTop

        // ── No data ───────────────────────────────────────────────
        if (entries.isEmpty()) {
            canvas.drawText(
                "No history yet — scores will appear daily",
                cw / 2f, ch / 2f + dp(5f), noDataPaint
            )
            return
        }

        // ── Grade zone dashed lines ───────────────────────────────
        val zoneThresholds = listOf(90, 70, 50, 30)
        zoneThresholds.forEach { score ->
            val y = scoreToY(score.toFloat(), chartTop, chartH)
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            zoneLabelPaint.color = gradeColor(score)
            zoneLabelPaint.alpha = 140
            canvas.drawText(
                gradeLabel(score),
                chartLeft + dp(2f), y - dp(2f), zoneLabelPaint
            )
        }

        // ── Compute point positions ───────────────────────────────
        val n      = entries.size
        val xStep  = if (n > 1) chartW / (n - 1).toFloat() else chartW / 2f
        val points = entries.mapIndexed { i, e ->
            val x = if (n == 1) chartLeft + chartW / 2f else chartLeft + i * xStep
            val y = scoreToY(e.score.toFloat(), chartTop, chartH)
            PointF(x, y)
        }

        // ── Gradient fill ─────────────────────────────────────────
        val avgScore = entries.map { it.score }.average().toFloat()
        val topColor  = scoreColor(avgScore)
        val gradShader = LinearGradient(
            0f, chartTop, 0f, chartBottom,
            intArrayOf(topColor and 0x00FFFFFF or 0x66000000 or (topColor and 0x00FFFFFF), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        fillPaint.shader = gradShader

        val fillPath = Path()
        fillPath.moveTo(points.first().x, chartBottom)
        fillPath.lineTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val cp1x = (points[i - 1].x + points[i].x) / 2f
            fillPath.cubicTo(cp1x, points[i - 1].y, cp1x, points[i].y, points[i].x, points[i].y)
        }
        fillPath.lineTo(points.last().x, chartBottom)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        // ── Line ──────────────────────────────────────────────────
        val lineShader = LinearGradient(
            chartLeft, 0f, chartRight, 0f,
            points.map { p -> scoreColor(yToScore(p.y, chartTop, chartH)) }.toIntArray(),
            points.map { p -> (p.x - chartLeft) / chartW }.toFloatArray(),
            Shader.TileMode.CLAMP
        )
        linePaint.shader = lineShader

        val linePath = Path()
        linePath.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val cp1x = (points[i - 1].x + points[i].x) / 2f
            linePath.cubicTo(cp1x, points[i - 1].y, cp1x, points[i].y, points[i].x, points[i].y)
        }
        canvas.drawPath(linePath, linePaint)

        // ── Dots + labels ─────────────────────────────────────────
        points.forEachIndexed { i, p ->
            val sc = entries[i].score
            val dc = scoreColor(sc.toFloat())

            dotPaint.color = dc
            canvas.drawCircle(p.x, p.y, dp(5f), dotPaint)
            canvas.drawCircle(p.x, p.y, dp(5f), dotOutlinePaint)

            // Score text above dot
            scoreLabelPaint.color = dc
            canvas.drawText("$sc", p.x, p.y - dp(9f), scoreLabelPaint)

            // Day label below chart
            canvas.drawText(entries[i].dateLabel, p.x, chartBottom + dp(14f), labelPaint)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────
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

    private fun gradeColor(score: Int) = scoreColor(score.toFloat())

    private fun gradeLabel(score: Int) = when {
        score >= 90 -> "Excellent"
        score >= 70 -> "Good"
        score >= 50 -> "Fair"
        else         -> "Poor"
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val defH = (dp(180f)).toInt()
        val w    = MeasureSpec.getSize(wSpec)
        val h    = when (MeasureSpec.getMode(hSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(hSpec)
            MeasureSpec.AT_MOST -> minOf(defH, MeasureSpec.getSize(hSpec))
            else                 -> defH
        }
        setMeasuredDimension(w, h)
    }
}
