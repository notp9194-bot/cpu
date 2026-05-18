package com.example.deviceinfo.feature.nettraffic

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class NetTrafficDoughnutChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    data class AppTraffic(val name: String, val totalBytes: Long)
    private var entries = listOf<AppTraffic>()
    private val colors = listOf(0xFF4CAF50.toInt(),0xFF2196F3.toInt(),0xFFFF9800.toInt(),0xFFF44336.toInt(),0xFF9C27B0.toInt())
    private val bg = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val tp = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

    fun setEntries(list: List<AppTraffic>) { entries = list.take(5); invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); canvas.drawRect(0f, 0f, w, h, bg)
        if (entries.isEmpty()) { canvas.drawText("Loading…", w/2, h/2, tp); return }
        val total = entries.sumOf { it.totalBytes }.coerceAtLeast(1L).toFloat()
        val cx = w * 0.38f; val cy = h / 2; val r = minOf(cx, cy) - 20f; val inner = r * 0.5f
        var startAngle = -90f
        val paint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
        entries.forEachIndexed { i, e ->
            val sweep = (e.totalBytes / total) * 360f
            paint.color = colors[i % colors.size]
            canvas.drawArc(cx - r, cy - r, cx + r, cy + r, startAngle, sweep, true, paint)
            startAngle += sweep
        }
        paint.color = 0xFF1A1A2E.toInt(); canvas.drawCircle(cx, cy, inner, paint)
        tp.textSize = 22f; canvas.drawText("Data", cx, cy - 10f, tp)
        canvas.drawText("Usage", cx, cy + 16f, tp)
        // Legend
        val lx = w * 0.62f
        entries.forEachIndexed { i, e ->
            val dotP = Paint().apply { color = colors[i % colors.size]; isAntiAlias = true }
            val y = 20f + i * 44f + 20f
            canvas.drawCircle(lx + 10f, y, 8f, dotP)
            tp.textSize = 20f; tp.textAlign = Paint.Align.LEFT
            canvas.drawText(e.name.take(14), lx + 24f, y + 7f, tp)
            tp.color = 0xFFAAAAAA.toInt(); tp.textSize = 18f
            canvas.drawText(formatBytes(e.totalBytes), lx + 24f, y + 24f, tp)
            tp.color = 0xFFFFFFFF.toInt()
        }
        tp.textAlign = Paint.Align.CENTER
    }

    private fun formatBytes(b: Long) = when {
        b > 1073741824 -> "${"%.1f".format(b/1073741824f)} GB"
        b > 1048576    -> "${"%.1f".format(b/1048576f)} MB"
        else           -> "${"%.0f".format(b/1024f)} KB"
    }
}
