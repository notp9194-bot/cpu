package com.example.deviceinfo.feature.connectivity

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Spider/Radar chart for connectivity capabilities.
 * Each axis = one connectivity feature, value 0.0–1.0.
 * Pure Canvas. No external library. Play Store safe.
 */
class RadarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class RadarAxis(val label: String, val value: Float)  // value 0f..1f

    private var axes: List<RadarAxis> = emptyList()

    private val webPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22000000; strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000; strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x557B2FBE; style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF37474F.toInt()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); typeface = Typeface.DEFAULT_BOLD
    }

    fun update(newAxes: List<RadarAxis>) {
        axes = newAxes
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()

        titlePaint.textSize = 28f
        canvas.drawText("Connectivity Radar", 16f, 36f, titlePaint)

        if (axes.isEmpty()) return

        val n = axes.size
        val cx = w / 2f
        val cy = h * 0.52f
        val r  = min(cx, cy - 36f) * 0.62f

        labelPaint.textSize  = (r * 0.20f).coerceIn(20f, 34f)
        valuePaint.textSize  = (r * 0.16f).coerceIn(16f, 28f)

        // Angle for each axis: start from top (-90°)
        fun angle(i: Int) = (Math.toRadians((-90.0 + 360.0 / n * i)))

        // Draw web rings (5 rings = 20%, 40%, 60%, 80%, 100%)
        for (ring in 1..5) {
            val ringR = r * ring / 5f
            val path = Path()
            for (i in 0 until n) {
                val a = angle(i)
                val x = cx + ringR * cos(a).toFloat()
                val y = cy + ringR * sin(a).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, webPaint)
        }

        // Draw axis lines from center to edge
        for (i in 0 until n) {
            val a = angle(i)
            canvas.drawLine(cx, cy,
                cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat(), axisPaint)
        }

        // Draw data polygon
        val dataPath = Path()
        for (i in 0 until n) {
            val a = angle(i)
            val v = axes[i].value.coerceIn(0f, 1f)
            val x = cx + r * v * cos(a).toFloat()
            val y = cy + r * v * sin(a).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        canvas.drawPath(dataPath, fillPaint)
        canvas.drawPath(dataPath, strokePaint)

        // Draw dots and labels
        for (i in 0 until n) {
            val a = angle(i)
            val v = axes[i].value.coerceIn(0f, 1f)
            val dx = cx + r * v * cos(a).toFloat()
            val dy = cy + r * v * sin(a).toFloat()
            canvas.drawCircle(dx, dy, 7f, dotPaint)

            // Label at edge + small padding
            val labelR = r + labelPaint.textSize * 1.1f
            val lx = cx + labelR * cos(a).toFloat()
            val ly = cy + labelR * sin(a).toFloat() + labelPaint.textSize * 0.4f
            canvas.drawText(axes[i].label, lx, ly, labelPaint)
        }
    }
}
