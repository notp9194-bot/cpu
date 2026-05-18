package com.example.deviceinfo.feature.chargesession

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ChargeSessionBarChart @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private var sessions = listOf<ChargeSession>()
    private val bgPaint    = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val startPaint = Paint().apply { color = 0xFF2196F3.toInt(); isAntiAlias = true }
    private val gainPaint  = Paint().apply { color = 0xFF4CAF50.toInt(); isAntiAlias = true }
    private val textPaint  = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize = 24f; isAntiAlias = true }
    private val subPaint   = Paint().apply { color = 0xFFAAAAAA.toInt(); textSize = 20f; isAntiAlias = true }

    fun setSessions(list: List<ChargeSession>) { sessions = list.take(10).reversed(); invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        if (sessions.isEmpty()) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("No sessions yet — plug in to start", w / 2, h / 2, textPaint)
            textPaint.textAlign = Paint.Align.LEFT; return
        }
        val n = sessions.size
        val barW = (w - 40f) / n - 8f
        val maxH = h - 60f
        sessions.forEachIndexed { i, s ->
            val x = 20f + i * (barW + 8f)
            val startH = (s.startPct / 100f) * maxH
            val gainH  = ((s.endPct - s.startPct).coerceAtLeast(0) / 100f) * maxH
            // Start% bar (blue)
            canvas.drawRect(x, h - 30f - startH, x + barW, h - 30f, startPaint)
            // Gain bar (green on top)
            canvas.drawRect(x, h - 30f - startH - gainH, x + barW, h - 30f - startH, gainPaint)
            // Label
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("${s.endPct}%", x + barW/2, h - 30f - startH - gainH - 6f, textPaint)
            subPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(s.date.takeLast(5), x + barW/2, h - 8f, subPaint)
        }
        textPaint.textAlign = Paint.Align.LEFT; subPaint.textAlign = Paint.Align.LEFT
    }
}
