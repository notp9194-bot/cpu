package com.cpua.deviceinfo.feature.health

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Animated arc gauge showing Device Health Score 0-100.
 * Large central score + grade label. Pure Canvas. Play Store safe ✅
 */
class DeviceHealthScoreView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var score     = 0
    private var animScore = 0f
    private var gradeColor = 0xFF4CAF50.toInt()
    private var gradeLabel = "Good"

    private val bgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x1AFFFFFF
        strokeCap = Paint.Cap.ROUND
    }
    private val fgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val gradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.DEFAULT_BOLD
    }
    private val outOfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color     = 0xFF888888.toInt()
    }

    private val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1200
        interpolator = android.view.animation.DecelerateInterpolator()
        addUpdateListener {
            animScore = score * (it.animatedValue as Float)
            invalidate()
        }
    }

    fun setScore(s: Int, color: Int, grade: String) {
        score      = s.coerceIn(0, 100)
        gradeColor = color
        gradeLabel = grade
        animator.cancel()
        animScore = 0f
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f
        val r  = (minOf(cx, cy) - 24f).coerceAtLeast(30f)
        val sw = r * 0.18f

        bgArcPaint.strokeWidth = sw
        fgArcPaint.strokeWidth = sw

        val startAngle = 150f
        val sweepTotal = 240f
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)

        // Background arc
        canvas.drawArc(oval, startAngle, sweepTotal, false, bgArcPaint)

        // Foreground arc with color
        fgArcPaint.color = gradeColor
        val sweep = (animScore / 100f) * sweepTotal
        if (sweep > 0f) canvas.drawArc(oval, startAngle, sweep, false, fgArcPaint)

        // Score number
        scorePaint.textSize = r * 0.52f
        scorePaint.color = gradeColor
        canvas.drawText("${animScore.toInt()}", cx, cy + r * 0.18f, scorePaint)

        // "/100" small
        outOfPaint.textSize = r * 0.24f
        canvas.drawText("/100", cx, cy + r * 0.45f, outOfPaint)

        // Grade label
        gradePaint.textSize = r * 0.28f
        gradePaint.color = gradeColor
        canvas.drawText(gradeLabel, cx, cy - r * 0.35f, gradePaint)
    }
}
