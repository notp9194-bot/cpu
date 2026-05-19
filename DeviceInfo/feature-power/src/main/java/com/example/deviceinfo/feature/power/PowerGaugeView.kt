package com.example.deviceinfo.feature.power

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Watt-meter style power gauge.
 * Shows charging watts with color-coded zones:
 *   grey  = 0–5W   (trickle)
 *   green = 5–15W  (normal)
 *   orange= 15–30W (fast)
 *   red   = 30W+   (super fast)
 * Pure Canvas. No external library. Play Store safe.
 */
class PowerGaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var watts: Float = 0f
    private var maxWatts: Float = 65f  // dynamically set
    private var isCharging: Boolean = false
    private var chargingType: String = ""

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0x15000000; strokeCap = Paint.Cap.ROUND
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val wattPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = 0xFF78909C.toInt()
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = 0xFF78909C.toInt()
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private fun colorForWatts(w: Float) = when {
        w <= 0f  -> 0xFF90A4AE.toInt()   // grey — not charging
        w <= 5f  -> 0xFF90A4AE.toInt()   // grey — trickle
        w <= 15f -> 0xFF4CAF50.toInt()   // green — normal
        w <= 30f -> 0xFFFF9800.toInt()   // orange — fast
        else     -> 0xFFF44336.toInt()   // red — super fast
    }

    private fun chargingLabel(w: Float): String = when {
        !isCharging || w <= 0f -> "Not Charging"
        w <= 5f  -> "Trickle Charge"
        w <= 10f -> "Slow Charge"
        w <= 18f -> "Normal Charge"
        w <= 30f -> "Fast Charge ⚡"
        w <= 45f -> "Super Fast Charge ⚡⚡"
        else     -> "Ultra Fast Charge ⚡⚡⚡"
    }

    fun update(watts: Float, isCharging: Boolean, chargingType: String = "") {
        this.watts = watts.coerceAtLeast(0f)
        this.isCharging = isCharging
        this.chargingType = chargingType
        // Dynamic max: round up to next 10W tier, at least 20W
        this.maxWatts = (maxOf(watts * 1.3f, 20f) / 10f).toInt().toFloat() * 10f + 10f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h * 0.50f
        val r  = (minOf(cx, cy) - 8f).coerceAtLeast(30f)
        val sw = r * 0.22f
        trackPaint.strokeWidth = sw; arcPaint.strokeWidth = sw

        val oval = RectF(cx - r, cy - r, cx + r, cy + r)

        // Full track (dark background ring)
        canvas.drawArc(oval, 150f, 240f, false, trackPaint)

        // Colored arc (watt fill)
        val col = colorForWatts(watts)
        arcPaint.color = col
        val frac  = if (maxWatts > 0) (watts / maxWatts).coerceIn(0f, 1f) else 0f
        val sweep = frac * 240f
        if (sweep > 0.5f) canvas.drawArc(oval, 150f, sweep, false, arcPaint)

        // End dot on the arc tip
        if (sweep > 4f) {
            val tipAngle = Math.toRadians((150.0 + sweep))
            dotPaint.color = col
            canvas.drawCircle(
                cx + r * kotlin.math.cos(tipAngle).toFloat(),
                cy + r * kotlin.math.sin(tipAngle).toFloat(),
                sw / 2f + 2f, dotPaint
            )
        }

        // Watt number center
        wattPaint.textSize = r * 0.44f
        wattPaint.color = col
        val wattStr = if (watts > 0f) "${"%.1f".format(watts)}" else "—"
        canvas.drawText(wattStr, cx, cy + r * 0.16f, wattPaint)

        // Unit
        unitPaint.textSize = r * 0.22f
        canvas.drawText("Watts", cx, cy + r * 0.44f, unitPaint)

        // Bottom label
        labelPaint.textSize = r * 0.18f
        canvas.drawText(chargingLabel(watts), cx, cy + r + sw / 2f + r * 0.24f, labelPaint)

        // Charging type (AC / USB / Wireless)
        if (chargingType.isNotBlank()) {
            labelPaint.textSize = r * 0.16f
            canvas.drawText(chargingType, cx, cy + r + sw / 2f + r * 0.46f, labelPaint)
        }

        // Min/Max labels on arc ends
        labelPaint.textSize = r * 0.17f
        canvas.drawText("0W", cx - r * 0.9f, cy + r * 0.60f, labelPaint)
        canvas.drawText("${maxWatts.toInt()}W", cx + r * 0.9f, cy + r * 0.60f, labelPaint)
    }
}
