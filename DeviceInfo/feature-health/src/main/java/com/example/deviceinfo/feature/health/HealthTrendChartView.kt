package com.example.deviceinfo.feature.health

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * 7-Day Health Score Trend Line Chart.
 *
 * FIXES:
 *  1. dp() NOT called at field init time (resources not ready → NPE)
 *  2. LinearGradient guard for single-point entries (1-element positions → IllegalArgumentException)
 *  3. All paint sizes set inside onDraw where resources is guaranteed available
 */
class HealthTrendChartView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private var entries: List<HealthScoreHistory.Entry> = emptyList()

    fun setEntries(data: List<HealthScoreHistory.Entry>) {
        entries = data
        invalidate()
    }

    // Paints — NO dp() or resources access here (view not attached yet)
    private val linePaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint        = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.WHITE
    }
    private val labelPaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFBBBBBB.toInt(); textAlign = Paint.Align.CENTER
    }
    private val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val gridPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0x33FFFFFF
    }
    private val zoneLabelPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val noDataPaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt(); textAlign = Paint.Align.CENTER
    }

    // Safe: called only inside onDraw/onMeasure where resources is available
    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cw = width.toFloat()
        val ch = height.toFloat()
        if (cw <= 0f || ch <= 0f) return

        // Set paint sizes here — safe, resources available during draw
        val density = resources.displayMetrics.density
        linePaint.strokeWidth       = 2.5f * density
        dotOutlinePaint.strokeWidth = 2f   * density
        labelPaint.textSize         = 10f  * density
        scoreLabelPaint.textSize    = 9.5f * density
        gridPaint.strokeWidth       = 0.8f * density
        gridPaint.pathEffect        = DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
        zoneLabelPaint.textSize     = 8f   * density
        noDataPaint.textSize        = 13f  * density

        val padL = 40f * density; val padR = 16f * density
        val padT = 28f * density; val padB = 32f * density

        val chartLeft   = padL
        val chartRight  = cw - padR
        val chartTop    = padT
        val chartBottom = ch - padB
        val chartW      = (chartRight - chartLeft).coerceAtLeast(1f)
        val chartH      = (chartBottom - chartTop).coerceAtLeast(1f)

        // No data
        if (entries.isEmpty()) {
            canvas.drawText("No history yet — scores appear daily", cw / 2f, ch / 2f, noDataPaint)
            return
        }

        // Grade zone lines
        listOf(90, 70, 50, 30).forEach { score ->
            val y = scoreToY(score.toFloat(), chartTop, chartH)
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            zoneLabelPaint.color = scoreColor(score.toFloat())
            zoneLabelPaint.alpha = 140
            canvas.drawText(gradeLabel(score), chartLeft + 2f * density, y - 2f * density, zoneLabelPaint)
        }

        // Point positions
        val n      = entries.size
        val xStep  = if (n > 1) chartW / (n - 1).toFloat() else chartW / 2f
        val points = entries.mapIndexed { i, e ->
            val x = if (n == 1) chartLeft + chartW / 2f else chartLeft + i * xStep
            PointF(x, scoreToY(e.score.toFloat(), chartTop, chartH))
        }

        // Gradient fill under line
        val avgScore = entries.map { it.score }.average().toFloat()
        val topColor = scoreColor(avgScore)
        fillPaint.shader = LinearGradient(
            0f, chartTop, 0f, chartBottom,
            intArrayOf((topColor and 0x00FFFFFF) or 0x66000000, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),          // always exactly 2 stops — no crash
            Shader.TileMode.CLAMP
        )
        val fillPath = Path().apply {
            moveTo(points.first().x, chartBottom)
            lineTo(points.first().x, points.first().y)
            if (points.size == 1) {
                lineTo(points.first().x, chartBottom)
            } else {
                for (i in 1 until points.size) {
                    val cpx = (points[i-1].x + points[i].x) / 2f
                    cubicTo(cpx, points[i-1].y, cpx, points[i].y, points[i].x, points[i].y)
                }
                lineTo(points.last().x, chartBottom)
            }
            close()
        }
        canvas.drawPath(fillPath, fillPaint)

        // Line — use solid color for single point, gradient for multiple
        if (points.size == 1) {
            linePaint.shader = null
            linePaint.color  = topColor
            canvas.drawCircle(points[0].x, points[0].y, 4f * density, dotPaint.also { it.color = topColor })
        } else {
            val colors    = points.map { p -> scoreColor(yToScore(p.y, chartTop, chartH)) }.toIntArray()
            val positions = points.map { p -> ((p.x - chartLeft) / chartW).coerceIn(0f, 1f) }.toFloatArray()
            // Ensure first=0, last=1 to avoid IllegalArgumentException
            positions[0]                = 0f
            positions[positions.size-1] = 1f
            linePaint.shader = LinearGradient(
                chartLeft, 0f, chartRight, 0f,
                colors, positions, Shader.TileMode.CLAMP
            )
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val cpx = (points[i-1].x + points[i].x) / 2f
                    cubicTo(cpx, points[i-1].y, cpx, points[i].y, points[i].x, points[i].y)
                }
            }
            canvas.drawPath(linePath, linePaint)
        }

        // Dots + labels
        val dotR = 5f * density
        points.forEachIndexed { i, p ->
            val sc = entries[i].score
            val dc = scoreColor(sc.toFloat())
            dotPaint.color = dc
            canvas.drawCircle(p.x, p.y, dotR, dotPaint)
            canvas.drawCircle(p.x, p.y, dotR, dotOutlinePaint)
            scoreLabelPaint.color = dc
            canvas.drawText("$sc", p.x, p.y - 9f * density, scoreLabelPaint)
            canvas.drawText(entries[i].dateLabel, p.x, chartBottom + 14f * density, labelPaint)
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
