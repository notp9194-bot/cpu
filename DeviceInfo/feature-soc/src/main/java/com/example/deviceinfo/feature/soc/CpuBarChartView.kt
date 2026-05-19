package com.example.deviceinfo.feature.soc

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CpuBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class CoreData(val freqMhz: Long, val maxMhz: Long)
    private var cores: List<CoreData> = emptyList()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1A000000; style = Paint.Style.FILL }
    private val barGreen   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4CAF50.toInt(); style = Paint.Style.FILL }
    private val barOrange  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF9800.toInt(); style = Paint.Style.FILL }
    private val barRed     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF44336.toInt(); style = Paint.Style.FILL }
    private val textPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF607D8B.toInt(); textSize = 24f; textAlign = Paint.Align.CENTER
    }
    private val valPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); textSize = 22f; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); textSize = 28f; typeface = Typeface.DEFAULT_BOLD
    }

    fun updateCores(data: List<CoreData>) { cores = data; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 12f; val padR = 12f; val padT = 40f; val padB = 38f
        canvas.drawText("CPU Cores \u2014 Live", padL + 6f, padT - 8f, titlePaint)
        if (cores.isEmpty()) { canvas.drawText("Loading\u2026", w / 2, h / 2, textPaint); return }
        val n = cores.size
        val slotW = (w - padL - padR) / n
        val barW  = (slotW - 8f).coerceAtLeast(10f)
        val chartH = h - padT - padB
        cores.forEachIndexed { i, core ->
            val cx = padL + i * slotW + slotW / 2f
            val ratio = if (core.maxMhz > 0)
                (core.freqMhz.toFloat() / core.maxMhz).coerceIn(0f, 1f) else 0f
            val barH = ratio * chartH
            val rx = 8f
            canvas.drawRoundRect(cx - barW / 2, padT, cx + barW / 2, padT + chartH, rx, rx, trackPaint)
            val paint = when { ratio > 0.75f -> barRed; ratio > 0.45f -> barOrange; else -> barGreen }
            if (barH > 2f)
                canvas.drawRoundRect(
                    cx - barW / 2, padT + chartH - barH,
                    cx + barW / 2, padT + chartH, rx, rx, paint
                )
            canvas.drawText("C$i", cx, padT + chartH + 26f, textPaint)
            if (barH > 46f) {
                val label = if (core.freqMhz >= 1000)
                    "${"%.1f".format(core.freqMhz / 1000.0)}G"
                else "${core.freqMhz}M"
                canvas.drawText(label, cx, padT + chartH - barH + 20f, valPaint)
            }
        }
    }
}
