package com.example.deviceinfo.feature.wifichannel

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class WifiSpectrumChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    data class Network(val ssid: String, val channel: Int, val rssi: Int, val freq: Int, val isMine: Boolean)
    private var networks = listOf<Network>()
    private val bg      = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val grid    = Paint().apply { color = 0x22FFFFFF; strokeWidth = 1f }
    private val tp      = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize = 22f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val colors  = listOf(0xFF4CAF50.toInt(), 0xFF2196F3.toInt(), 0xFFFF9800.toInt(), 0xFFF44336.toInt(),
        0xFF9C27B0.toInt(), 0xFF00BCD4.toInt(), 0xFFFFEB3B.toInt(), 0xFFFF5722.toInt())

    fun setNetworks(nets: List<Network>) { networks = nets; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); canvas.drawRect(0f, 0f, w, h, bg)
        val nets24 = networks.filter { it.freq < 3000 }.sortedBy { it.channel }
        val nets5  = networks.filter { it.freq >= 3000 }.sortedBy { it.channel }
        fun drawBand(nets: List<Network>, title: String, x0: Float, x1: Float) {
            val bw = x1 - x0
            canvas.drawLine(x0, 0f, x0, h, grid)
            tp.textSize = 24f; tp.color = 0xFFCCCCCC.toInt()
            canvas.drawText(title, x0 + bw / 2, 24f, tp)
            if (nets.isEmpty()) { canvas.drawText("No networks", x0 + bw / 2, h / 2, tp); return }
            val maxRssi = -30f; val minRssi = -100f
            nets.forEachIndexed { i, n ->
                val norm = ((n.rssi - minRssi) / (maxRssi - minRssi)).coerceIn(0f, 1f)
                val barH = norm * (h - 60f)
                val x = x0 + (i + 0.5f) * (bw / nets.size.toFloat())
                val barW = (bw / nets.size.toFloat() - 4f).coerceAtLeast(8f)
                val paint = Paint().apply { color = if (n.isMine) 0xFF4CAF50.toInt() else colors[i % colors.size]; alpha = 200; isAntiAlias = true }
                canvas.drawRoundRect(x - barW / 2, h - 40f - barH, x + barW / 2, h - 40f, 6f, 6f, paint)
                tp.textSize = 19f; tp.color = 0xFFFFFFFF.toInt()
                canvas.drawText("Ch${n.channel}", x, h - 26f, tp)
                canvas.drawText("${n.rssi}dB", x, h - 40f - barH - 4f, tp)
                tp.textSize = 17f; tp.color = 0xFFCCCCCC.toInt()
                canvas.drawText(n.ssid.take(8), x, h - 8f, tp)
            }
        }
        drawBand(nets24, "2.4 GHz", 0f, w * 0.5f)
        drawBand(nets5,  "5 GHz",  w * 0.5f, w)
        tp.textAlign = Paint.Align.LEFT
    }
}
