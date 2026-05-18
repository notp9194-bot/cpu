package com.cpua.deviceinfo.feature.benchmark

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Speedometer-style benchmark gauge.
 * Shows score 0–10000 on an arc with colored zones.
 * Pure Canvas — no external library. Play Store safe.
 */
class BenchmarkGaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var score: Int = 0
    private var maxScore: Int = 10000
    private var label: String = "Score"
    private var isAnimating: Boolean = false

    // Arc goes from 150° to 390° (240° sweep)
    private val START_ANGLE = 150f
    private val SWEEP_ANGLE = 240f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x1A000000
        strokeCap = Paint.Cap.ROUND
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt()
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val needleDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt()
        style = Paint.Style.FILL
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        color = 0xFF212121.toInt()
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFF78909C.toInt()
    }
    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF90A4AE.toInt()
        strokeWidth = 2f
    }
    private val tickTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFF90A4AE.toInt()
    }

    // Zone colors: red=slow, orange=fair, yellow=good, green=fast, purple=extreme
    private val zones = listOf(
        Triple(0f,    0.20f, 0xFFF44336.toInt()),   // 0-20%    Slow
        Triple(0.20f, 0.40f, 0xFFFF9800.toInt()),   // 20-40%   Fair
        Triple(0.40f, 0.60f, 0xFFFFEB3B.toInt()),   // 40-60%   Good
        Triple(0.60f, 0.80f, 0xFF4CAF50.toInt()),   // 60-80%   Fast
        Triple(0.80f, 1.00f, 0xFF7B2FBE.toInt()),   // 80-100%  Extreme
    )

    fun update(score: Int, maxScore: Int = 10000, label: String = "Score") {
        this.score = score.coerceIn(0, maxScore)
        this.maxScore = maxScore
        this.label = label
        invalidate()
    }

    fun setAnimating(animating: Boolean) {
        isAnimating = animating
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        // Push center down a bit so gauge looks balanced
        val cy = h * 0.52f
        val r = (minOf(cx, cy) * 0.80f).coerceAtLeast(40f)
        val strokeW = r * 0.14f
        trackPaint.strokeWidth = strokeW
        zonePaint.strokeWidth  = strokeW

        val oval = RectF(cx - r, cy - r, cx + r, cy + r)

        // Draw track background
        canvas.drawArc(oval, START_ANGLE, SWEEP_ANGLE, false, trackPaint)

        // Draw color zones
        zones.forEach { (startFrac, endFrac, color) ->
            zonePaint.color = color
            val zStart = START_ANGLE + startFrac * SWEEP_ANGLE
            val zSweep = (endFrac - startFrac) * SWEEP_ANGLE - 1f // 1° gap
            canvas.drawArc(oval, zStart, zSweep, false, zonePaint)
        }

        // Draw tick marks
        tickTextPaint.textSize = r * 0.16f
        tickPaint.strokeWidth = r * 0.025f
        for (i in 0..10) {
            val frac = i / 10f
            val angleDeg = START_ANGLE + frac * SWEEP_ANGLE
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val outerR = r + strokeW / 2f + r * 0.04f
            val innerR = r - strokeW / 2f - r * 0.04f
            val txr    = r - strokeW / 2f - r * 0.22f
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()
            canvas.drawLine(
                cx + innerR * cosA, cy + innerR * sinA,
                cx + outerR * cosA, cy + outerR * sinA,
                tickPaint
            )
            if (i % 2 == 0) {
                val tickLabel = "${(frac * maxScore / 1000).toInt()}K"
                canvas.drawText(tickLabel, cx + txr * cosA, cy + txr * sinA + tickTextPaint.textSize / 3f, tickTextPaint)
            }
        }

        // Draw needle
        val scoreFrac = score.toFloat() / maxScore.toFloat()
        val needleAngleDeg = START_ANGLE + scoreFrac * SWEEP_ANGLE
        val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
        val needleLen = r - strokeW / 2f - r * 0.05f
        needlePaint.strokeWidth = r * 0.045f
        canvas.drawLine(
            cx, cy,
            cx + needleLen * cos(needleAngleRad).toFloat(),
            cy + needleLen * sin(needleAngleRad).toFloat(),
            needlePaint
        )
        // Needle center dot
        canvas.drawCircle(cx, cy, r * 0.07f, needleDotPaint)

        // Score number
        scorePaint.textSize = r * 0.42f
        val scoreStr = if (isAnimating) "…" else "%,d".format(score)
        canvas.drawText(scoreStr, cx, cy + r * 0.28f, scorePaint)

        // Label below score
        labelPaint.textSize = r * 0.20f
        canvas.drawText(label, cx, cy + r * 0.52f, labelPaint)

        // Zone label
        val zoneLabel = when {
            isAnimating -> "Running…"
            score == 0  -> "Tap Run Benchmark"
            scoreFrac < 0.20f -> "Slow"
            scoreFrac < 0.40f -> "Fair"
            scoreFrac < 0.60f -> "Good"
            scoreFrac < 0.80f -> "Fast"
            else              -> "Extreme"
        }
        labelPaint.textSize = r * 0.18f
        canvas.drawText(zoneLabel, cx, cy + r * 0.72f, labelPaint)
    }
}
