package com.cpua.deviceinfo.feature.sensorlive

import android.animation.ValueAnimator; import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View; import kotlin.math.*

class SensorNeedleGauge @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private var currentAngle = -140f; private var targetAngle = -140f; private var label = ""; private var valueText = "0"
    private val minAngle = -140f; private val maxAngle = 140f; private var min = -10f; private var max = 10f
    private var accentColor = 0xFF4CAF50.toInt()
    private val bg     = Paint().apply { color = 0xFF1E1E2E.toInt(); isAntiAlias = true }
    private val arcBg  = Paint().apply { color = 0xFF333344.toInt(); style = Paint.Style.STROKE; strokeWidth = 16f; isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
    private val arcFg  = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 16f; isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
    private val needle = Paint().apply { color = 0xFFFF5722.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE; isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
    private val center = Paint().apply { color = 0xFFFF5722.toInt(); isAntiAlias = true }
    private val tp     = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private var animator: ValueAnimator? = null

    fun setup(lbl: String, mn: Float, mx: Float, color: Int) { label = lbl; min = mn; max = mx; accentColor = color; arcFg.color = color; invalidate() }

    fun setValue(v: Float) {
        val pct = ((v - min) / (max - min)).coerceIn(0f, 1f)
        targetAngle = minAngle + pct * (maxAngle - minAngle)
        valueText = if (kotlin.math.abs(v) > 100) v.toInt().toString() else "${"%.2f".format(v)}"
        animator?.cancel()
        animator = ValueAnimator.ofFloat(currentAngle, targetAngle).apply {
            duration = 300; addUpdateListener { currentAngle = it.animatedValue as Float; invalidate() }; start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height).toFloat(); val cx = width / 2f; val cy = height * 0.55f
        val r = size * 0.38f; val startAng = 200f; val sweepAng = 280f
        canvas.drawCircle(cx, cy, size * 0.48f, bg)
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, startAng, sweepAng, false, arcBg)
        val pct = ((currentAngle - minAngle) / (maxAngle - minAngle)).coerceIn(0f, 1f)
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, startAng, pct * sweepAng, false, arcFg)
        val rad = Math.toRadians((startAng + pct * sweepAng).toDouble())
        val nx = cx + (r * 0.82f * cos(rad)).toFloat(); val ny = cy + (r * 0.82f * sin(rad)).toFloat()
        canvas.drawLine(cx, cy, nx, ny, needle); canvas.drawCircle(cx, cy, 8f, center)
        tp.textSize = 28f; tp.color = 0xFFFFFFFF.toInt(); canvas.drawText(valueText, cx, cy - r * 0.4f, tp)
        tp.textSize = 22f; tp.color = accentColor.toLong().toInt(); canvas.drawText(label, cx, cy + r * 0.5f, tp)
    }
}
