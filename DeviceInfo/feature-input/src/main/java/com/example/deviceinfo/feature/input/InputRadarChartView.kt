package com.example.deviceinfo.feature.input

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Radar chart for input capabilities:
 * Multitouch, Stylus, Fingerprint, Gamepad, Keyboard, Face, Gyro, Mic.
 * Pure Canvas. No external library. Play Store safe.
 * Reuses same pattern as RadarChartView in feature-connectivity.
 */
class InputRadarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class InputAxis(val label: String, val value: Float)  // 0f..1f

    private var axes: List<InputAxis> = emptyList()

    private val webPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000; strokeWidth = 1.5f; style = Paint.Style.STROKE }
    private val axisPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000; strokeWidth = 1.5f; style = Paint.Style.STROKE }
    private val fillPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55E91E63; style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE91E63.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE91E63.toInt(); style = Paint.Style.FILL }
    private val labelPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF37474F.toInt(); typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    private val titlePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE91E63.toInt(); typeface = Typeface.DEFAULT_BOLD; textSize = 28f
    }

    fun update(newAxes: List<InputAxis>) { axes = newAxes; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawText("Input Capabilities", 16f, 36f, titlePaint)
        if (axes.isEmpty()) return

        val n = axes.size
        val cx = w / 2f; val cy = h * 0.52f
        val r  = min(cx, cy - 36f) * 0.62f

        labelPaint.textSize = (r * 0.20f).coerceIn(20f, 32f)

        fun angle(i: Int) = Math.toRadians(-90.0 + 360.0 / n * i)

        // Web rings
        for (ring in 1..4) {
            val rr = r * ring / 4f
            val path = Path()
            for (i in 0 until n) {
                val a = angle(i)
                val x = cx + rr * cos(a).toFloat()
                val y = cy + rr * sin(a).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close(); canvas.drawPath(path, webPaint)
        }

        // Axis lines
        for (i in 0 until n) {
            val a = angle(i)
            canvas.drawLine(cx, cy, cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat(), axisPaint)
        }

        // Data polygon
        val dataPath = Path()
        for (i in 0 until n) {
            val a = angle(i); val v = axes[i].value.coerceIn(0f, 1f)
            val x = cx + r * v * cos(a).toFloat(); val y = cy + r * v * sin(a).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        canvas.drawPath(dataPath, fillPaint)
        canvas.drawPath(dataPath, strokePaint)

        // Dots + labels
        for (i in 0 until n) {
            val a = angle(i); val v = axes[i].value.coerceIn(0f, 1f)
            canvas.drawCircle(cx + r * v * cos(a).toFloat(), cy + r * v * sin(a).toFloat(), 7f, dotPaint)
            val labelR = r + labelPaint.textSize * 1.1f
            canvas.drawText(axes[i].label,
                cx + labelR * cos(a).toFloat(),
                cy + labelR * sin(a).toFloat() + labelPaint.textSize * 0.4f,
                labelPaint)
        }
    }
}
