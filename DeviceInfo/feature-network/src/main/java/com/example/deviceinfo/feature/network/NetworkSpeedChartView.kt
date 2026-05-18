package com.cpua.deviceinfo.feature.network

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class NetworkSpeedChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val rxData = mutableListOf<Float>()
    private val txData = mutableListOf<Float>()

    private val rxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2196F3.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val txPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF9800.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val rxFill     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x332196F3; style = Paint.Style.FILL }
    private val txFill     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FF9800; style = Paint.Style.FILL }
    private val textPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF607D8B.toInt(); textSize = 24f }
    private val axisPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000; strokeWidth = 1f }
    private val legendPaint= Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 24f; typeface = Typeface.DEFAULT_BOLD }

    fun addDataPoint(rxKbps: Float, txKbps: Float) {
        rxData.add(rxKbps); txData.add(txKbps)
        if (rxData.size > MAX_POINTS) rxData.removeAt(0)
        if (txData.size > MAX_POINTS) txData.removeAt(0)
        invalidate()
    }

    private fun fmt(kbps: Float) =
        if (kbps >= 1024f) "${"%.1f".format(kbps / 1024f)} MB/s"
        else "${"%.0f".format(kbps)} KB/s"

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 56f; val padR = 12f; val padT = 40f; val padB = 28f
        val chartW = w - padL - padR; val chartH = h - padT - padB

        legendPaint.color = 0xFF2196F3.toInt()
        canvas.drawText("\u25bc DL: ${fmt(rxData.lastOrNull() ?: 0f)}", padL, padT - 10f, legendPaint)
        legendPaint.color = 0xFFFF9800.toInt()
        canvas.drawText("\u25b2 UL: ${fmt(txData.lastOrNull() ?: 0f)}", padL + chartW / 2f, padT - 10f, legendPaint)

        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        val maxVal = ((rxData + txData).maxOrNull() ?: 0f).coerceAtLeast(64f)
        for (i in 0..4) {
            val v = maxVal * i / 4f
            val y = padT + chartH - (i.toFloat() / 4f) * chartH
            val label = if (v >= 1024f) "${"%.0f".format(v / 1024f)}M" else "${"%.0f".format(v)}K"
            canvas.drawText(label, 0f, y + 9f, textPaint)
            axisPaint.alpha = 20
            canvas.drawLine(padL, y, padL + chartW, y, axisPaint)
            axisPaint.alpha = 255
        }

        if (rxData.size < 2) {
            canvas.drawText("Collecting\u2026", padL + 20f, padT + chartH / 2, textPaint)
            return
        }

        fun drawLine(list: List<Float>, lp: Paint, fp: Paint) {
            val path = Path(); val fill = Path()
            list.forEachIndexed { i, v ->
                val x = padL + i * (chartW / (MAX_POINTS - 1).toFloat())
                val y = padT + chartH - (v / maxVal).coerceIn(0f, 1f) * chartH
                if (i == 0) { path.moveTo(x, y); fill.moveTo(x, padT + chartH); fill.lineTo(x, y) }
                else         { path.lineTo(x, y); fill.lineTo(x, y) }
            }
            val lx = padL + (list.size - 1) * (chartW / (MAX_POINTS - 1).toFloat())
            fill.lineTo(lx, padT + chartH); fill.close()
            canvas.drawPath(fill, fp); canvas.drawPath(path, lp)
        }
        drawLine(rxData, rxPaint, rxFill)
        drawLine(txData, txPaint, txFill)
    }

    companion object { private const val MAX_POINTS = 60 }
}
