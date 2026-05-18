package com.example.deviceinfo.feature.pingmonitor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class PingLineChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    data class HostData(val name: String, val color: Int, val data: ArrayDeque<Float> = ArrayDeque())

    private val hosts = listOf(
        HostData("8.8.8.8",    0xFF4CAF50.toInt()),
        HostData("1.1.1.1",    0xFF2196F3.toInt()),
        HostData("google.com", 0xFFFF9800.toInt())
    )
    private val MAX = 30
    private val bgPaint   = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val gridPaint = Paint().apply { color = 0x22FFFFFF; strokeWidth = 1f }
    private val textPaint = Paint().apply { color = 0xFFCCCCCC.toInt(); textSize = 24f; isAntiAlias = true }

    fun addSample(host: Int, ms: Float) {
        val d = hosts[host].data
        if (d.size >= MAX) d.removeFirst()
        d.addLast(ms)
        invalidate()
    }

    fun getHosts() = hosts.map { it.name }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        val maxVal = hosts.flatMap { it.data }.maxOrNull()?.coerceAtLeast(100f) ?: 100f
        for (g in listOf(25f, 50f, 75f, 100f)) {
            val y = h - (g / 100f) * (h - 40f) - 20f
            canvas.drawLine(0f, y, w, y, gridPaint)
        }
        hosts.forEach { host ->
            if (host.data.size < 2) return@forEach
            val paint = Paint().apply { color = host.color; strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
            val path = Path()
            val step = w / (MAX - 1).toFloat()
            host.data.forEachIndexed { i, v ->
                val x = i * step; val y = h - (v / maxVal) * (h - 40f) - 20f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, paint)
            textPaint.color = host.color
            val lv = host.data.last()
            canvas.drawText("${host.name}: ${lv.toInt()}ms", 8f, (hosts.indexOf(host) + 1) * 28f + 4f, textPaint)
        }
        textPaint.color = 0xFFCCCCCC.toInt()
    }
}
