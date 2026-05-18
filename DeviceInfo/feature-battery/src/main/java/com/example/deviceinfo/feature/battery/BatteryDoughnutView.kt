package com.cpua.deviceinfo.feature.battery

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Compact battery percentage doughnut.
 * Color: green > 50%, orange 20-50%, red < 20%.
 * Pure Canvas — no external library. Play Store safe.
 */
class BatteryDoughnutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var percent: Int = 0
    private var isCharging: Boolean = false

    private val arcFree  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x18000000 }
    private val arcUsed  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = 0xFF78909C.toInt()
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = 0xFF4CAF50.toInt()
    }

    fun update(percent: Int, isCharging: Boolean) {
        this.percent = percent.coerceIn(0, 100)
        this.isCharging = isCharging
        invalidate()
    }

    private fun colorForPct(p: Int) = when {
        p >= 50 -> 0xFF4CAF50.toInt()
        p >= 20 -> 0xFFFF9800.toInt()
        else    -> 0xFFF44336.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f
        val r  = (minOf(cx, cy) - 8f).coerceAtLeast(20f)
        val sw = r * 0.34f
        arcFree.strokeWidth = sw; arcUsed.strokeWidth = sw

        // Free arc (full circle background)
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, -90f, 360f, false, arcFree)

        // Used arc
        val col = colorForPct(percent)
        arcUsed.color = col
        val sweep = percent.toFloat() / 100f * 360f
        if (sweep > 0f) canvas.drawArc(oval, -90f, sweep, false, arcUsed)

        // Center percent text
        pctPaint.textSize = r * 0.50f; pctPaint.color = col
        canvas.drawText("$percent%", cx, cy + r * 0.18f, pctPaint)

        // Charging icon or "Battery" label below
        lblPaint.textSize = r * 0.26f
        val bottomY = cy + r + sw / 2f + r * 0.30f
        canvas.drawText(if (isCharging) "\u26a1 Charging" else "Battery", cx, bottomY, lblPaint)
    }
}
