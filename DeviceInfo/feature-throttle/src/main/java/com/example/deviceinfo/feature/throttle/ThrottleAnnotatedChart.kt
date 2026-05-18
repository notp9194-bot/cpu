package com.example.deviceinfo.feature.throttle

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class ThrottleAnnotatedChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    data class Sample(val tempC: Float, val freqPct: Float, val throttled: Boolean)
    private val samples = ArrayDeque<Sample>()
    private val MAX = 60
    private val bg    = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val tempP = Paint().apply { color = 0xFFFF9800.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val freqP = Paint().apply { color = 0xFF2196F3.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val thtP  = Paint().apply { color = 0x44F44336; style = Paint.Style.FILL }
    private val dotP  = Paint().apply { color = 0xFFF44336.toInt(); isAntiAlias = true }
    private val grid  = Paint().apply { color = 0x22FFFFFF; strokeWidth = 1f }
    private val tp    = Paint().apply { color = 0xFFCCCCCC.toInt(); textSize = 22f; isAntiAlias = true }
    private var throttleCount = 0

    fun add(tempC: Float, freqPct: Float, throttled: Boolean) {
        if (samples.size >= MAX) samples.removeFirst()
        if (throttled && (samples.isEmpty() || !samples.last().throttled)) throttleCount++
        samples.addLast(Sample(tempC, freqPct, throttled))
        invalidate()
    }
    fun getThrottleCount() = throttleCount

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bg)
        if (samples.size < 2) { tp.textAlign = Paint.Align.CENTER; canvas.drawText("Collecting data…", w/2, h/2, tp); tp.textAlign = Paint.Align.LEFT; return }
        val step = w / (MAX - 1).toFloat()
        // Draw throttle regions
        samples.forEachIndexed { i, s ->
            if (s.throttled) canvas.drawRect(i * step, 0f, (i + 1) * step, h, thtP)
        }
        for (g in listOf(25f, 50f, 75f, 100f)) {
            val y = h - (g / 100f) * (h - 30f) - 15f; canvas.drawLine(0f, y, w, y, grid)
        }
        fun drawLine(getter: (Sample) -> Float, paint: Paint) {
            val path = Path()
            samples.forEachIndexed { i, s ->
                val x = i * step; val y = h - (getter(s) / 100f) * (h - 30f) - 15f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, paint)
        }
        // Temp normalized 0-100 assuming 80°C max
        drawLine({ (it.tempC / 80f * 100f).coerceIn(0f, 100f) }, tempP)
        drawLine({ it.freqPct }, freqP)
        // Mark throttle events
        samples.forEachIndexed { i, s ->
            if (s.throttled) canvas.drawCircle(i * step, h * 0.2f, 5f, dotP)
        }
        tp.color = 0xFFFF9800.toInt(); canvas.drawText("🌡 Temp", 8f, 20f, tp)
        tp.color = 0xFF2196F3.toInt(); canvas.drawText("⚡ Freq%", w / 2, 20f, tp)
        tp.color = 0xFFF44336.toInt(); canvas.drawText("● Throttle events: $throttleCount", 8f, h - 4f, tp)
        tp.color = 0xFFCCCCCC.toInt()
    }
}
