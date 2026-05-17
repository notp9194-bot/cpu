package com.example.deviceinfo.feature.benchmark

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Circular arc gauge showing benchmark score 0-1000 */
class ScoreGaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var score = 0
    private var label = "Score"
    private var animatedScore = 0f

    private val bgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 18f; color = 0x22AABBCC.toInt()
        strokeCap = Paint.Cap.ROUND
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 18f; strokeCap = Paint.Cap.ROUND
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E5FF.toInt(); textSize = 52f; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF607080.toInt(); textSize = 26f; textAlign = Paint.Align.CENTER
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33AABBCC.toInt(); strokeWidth = 2f
    }

    fun setScore(s: Int, lbl: String = "Score") {
        score = s.coerceIn(0, 1000); label = lbl
        animatedScore = score.toFloat()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f; val cy = height / 2f
        val r = min(cx, cy) - 30f
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        val startAngle = 150f; val sweepFull = 240f

        // Background arc
        canvas.drawArc(rect, startAngle, sweepFull, false, bgArcPaint)

        // Score arc with gradient color
        val pct = animatedScore / 1000f
        val color = when {
            pct < 0.33f -> lerpColor(0xFFFF5252.toInt(), 0xFFFFAB40.toInt(), pct / 0.33f)
            pct < 0.66f -> lerpColor(0xFFFFAB40.toInt(), 0xFF69F0AE.toInt(), (pct - 0.33f) / 0.33f)
            else        -> lerpColor(0xFF69F0AE.toInt(), 0xFF00E5FF.toInt(), (pct - 0.66f) / 0.34f)
        }
        arcPaint.color = color
        canvas.drawArc(rect, startAngle, sweepFull * pct, false, arcPaint)

        // Ticks
        for (i in 0..10) {
            val angle = Math.toRadians((startAngle + sweepFull * i / 10f).toDouble())
            val outer = r + 8f; val inner = r - 8f
            canvas.drawLine(
                cx + (cos(angle) * inner).toFloat(), cy + (sin(angle) * inner).toFloat(),
                cx + (cos(angle) * outer).toFloat(), cy + (sin(angle) * outer).toFloat(),
                tickPaint
            )
        }

        // Score text
        canvas.drawText(animatedScore.toInt().toString(), cx, cy + 18f, scorePaint.apply { color = color })
        canvas.drawText(label, cx, cy + 50f, labelPaint)
    }

    private fun lerpColor(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        return Color.rgb(r, g, bl)
    }
}
