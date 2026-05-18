package com.cpua.deviceinfo.feature.gpu

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Horizontal multi-bar showing GPU capability scores:
 * Texture size score, Extension coverage, Shader model, Compute support.
 * Pure Canvas. No external library. Play Store safe.
 */
class GpuBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class GpuBar(val label: String, val value: Float, val maxValue: Float, val unit: String = "")

    private var bars: List<GpuBar> = emptyList()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1A000000; style = Paint.Style.FILL
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF546E7A.toInt(); textSize = 26f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF212121.toInt(); textSize = 24f; typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); textSize = 28f; typeface = Typeface.DEFAULT_BOLD
    }

    private val barColors = listOf(
        0xFF4CAF50.toInt(), // green
         0xFF2196F3.toInt(), // blue
        0xFFFF9800.toInt(), // orange
        0xFFE91E63.toInt()  // pink
    )

    fun update(newBars: List<GpuBar>) { bars = newBars; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padL = 16f; val padR = 80f; val padT = 40f; val padB = 10f

        canvas.drawText("GPU Capabilities", padL, padT - 8f, titlePaint)

        if (bars.isEmpty()) { canvas.drawText("Loading…", w / 2, h / 2, labelPaint); return }

        val rowH = (h - padT - padB) / bars.size
        val barH = (rowH * 0.46f).coerceIn(14f, 30f)
        val trackW = w - padL - padR

        bars.forEachIndexed { i, bar ->
            val cy = padT + i * rowH + rowH / 2f
            val ratio = if (bar.maxValue > 0) (bar.value / bar.maxValue).coerceIn(0f, 1f) else 0f

            // Track
            val trackRect = RectF(padL, cy - barH / 2, padL + trackW, cy + barH / 2)
            canvas.drawRoundRect(trackRect, barH / 2, barH / 2, trackPaint)

            // Filled bar
            if (ratio > 0f) {
                barPaint.color = barColors[i % barColors.size]
                val fillRect = RectF(padL, cy - barH / 2, padL + trackW * ratio, cy + barH / 2)
                canvas.drawRoundRect(fillRect, barH / 2, barH / 2, barPaint)
            }

            // Label (above bar, left)
            labelPaint.textSize = (rowH * 0.28f).coerceIn(20f, 28f)
            canvas.drawText(bar.label, padL, cy - barH / 2 - 4f, labelPaint)

            // Value (right side)
            valuePaint.textSize = (rowH * 0.28f).coerceIn(20f, 28f)
            val valStr = if (bar.unit.isNotBlank()) "${bar.value.toInt()}${bar.unit}" else "%.1f".format(bar.value)
            canvas.drawText(valStr, w - padR + 70f, cy + valuePaint.textSize * 0.38f, valuePaint)
        }
    }
}
