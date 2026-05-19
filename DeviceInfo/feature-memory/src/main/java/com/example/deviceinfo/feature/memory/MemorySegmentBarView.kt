package com.example.deviceinfo.feature.memory

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Horizontal segmented bar: Used (blue) | Cache (orange) | Free (green).
 * Shows breakdown of total RAM at a glance.
 * Pure Canvas. No external library. Play Store safe.
 */
class MemorySegmentBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var usedMb:  Float = 0f
    private var availMb: Float = 0f
    private var totalMb: Float = 1f

    private val usedPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2196F3.toInt(); style = Paint.Style.FILL }
    private val freePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4CAF50.toInt(); style = Paint.Style.FILL }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x18000000; style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); textSize = 26f; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val subPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF546E7A.toInt(); textSize = 24f; textAlign = Paint.Align.CENTER
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); textSize = 28f; typeface = Typeface.DEFAULT_BOLD
    }

    fun update(usedMb: Float, availMb: Float, totalMb: Float) {
        this.usedMb  = usedMb.coerceAtLeast(0f)
        this.availMb = availMb.coerceAtLeast(0f)
        this.totalMb = totalMb.coerceAtLeast(1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 16f; val padR = 16f; val padT = 36f
        val barH = (h - padT - 36f).coerceAtLeast(20f)
        val barW = w - padL - padR
        val rx = barH / 2f

        canvas.drawText("RAM Usage", padL, padT - 6f, titlePaint)

        // Track
        val rect = RectF(padL, padT, padL + barW, padT + barH)
        canvas.drawRoundRect(rect, rx, rx, trackPaint)

        val usedRatio  = (usedMb  / totalMb).coerceIn(0f, 1f)
        val freeRatio  = (availMb / totalMb).coerceIn(0f, 1f - usedRatio)

        // Used segment
        if (usedRatio > 0.01f) {
            val uRect = RectF(padL, padT, padL + barW * usedRatio, padT + barH)
            canvas.drawRoundRect(uRect, rx, rx, usedPaint)
            if (usedRatio > 0.12f) {
                val usedStr = if (usedMb >= 1024f) "${"%.1f".format(usedMb / 1024f)}G" else "${"%.0f".format(usedMb)}M"
                canvas.drawText(usedStr, padL + barW * usedRatio / 2, padT + barH / 2 + 9f, labelPaint)
            }
        }

        // Free segment
        if (freeRatio > 0.01f) {
            val fStart = padL + barW * usedRatio
            val fRect  = RectF(fStart, padT, fStart + barW * freeRatio, padT + barH)
            canvas.drawRoundRect(fRect, rx, rx, freePaint)
            if (freeRatio > 0.12f) {
                val freeStr = if (availMb >= 1024f) "${"%.1f".format(availMb / 1024f)}G" else "${"%.0f".format(availMb)}M"
                canvas.drawText(freeStr, fStart + barW * freeRatio / 2, padT + barH / 2 + 9f, labelPaint)
            }
        }

        // Legend below bar
        val legY = padT + barH + 22f
        subPaint.textAlign = Paint.Align.LEFT
        subPaint.color = 0xFF2196F3.toInt()
        canvas.drawText("● Used", padL, legY, subPaint)
        subPaint.color = 0xFF4CAF50.toInt()
        canvas.drawText("● Free", padL + barW / 2f, legY, subPaint)
        subPaint.color = 0xFF546E7A.toInt()
        subPaint.textAlign = Paint.Align.RIGHT
        val totalStr = if (totalMb >= 1024f) "${"%.1f".format(totalMb / 1024f)} GB total" else "${"%.0f".format(totalMb)} MB total"
        canvas.drawText(totalStr, padL + barW, legY, subPaint)
    }
}
