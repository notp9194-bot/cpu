package com.example.deviceinfo.feature.cpucore

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CpuCoreMultiLineChart @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val MAX_POINTS = 60
    private val coreData = mutableListOf<ArrayDeque<Float>>()
    private var coreCount = 0

    private val COLORS = intArrayOf(
        0xFF4CAF50.toInt(), 0xFF2196F3.toInt(), 0xFFFF9800.toInt(), 0xFFF44336.toInt(),
        0xFF9C27B0.toInt(), 0xFF00BCD4.toInt(), 0xFFFFEB3B.toInt(), 0xFFFF5722.toInt()
    )

    private val gridPaint  = Paint().apply { color = 0x22FFFFFF; strokeWidth = 1f; style = Paint.Style.STROKE }
    private val textPaint  = Paint().apply { color = 0xFFCCCCCC.toInt(); textSize = 26f; isAntiAlias = true }
    private val bgPaint    = Paint().apply { color = 0xFF1A1A2E.toInt() }

    fun initCores(count: Int) {
        coreCount = count
        coreData.clear()
        repeat(count) { coreData.add(ArrayDeque()) }
        invalidate()
    }

    fun addSample(usages: List<Float>) {
        usages.forEachIndexed { i, v ->
            if (i < coreData.size) {
                if (coreData[i].size >= MAX_POINTS) coreData[i].removeFirst()
                coreData[i].addLast(v.coerceIn(0f, 100f))
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Grid lines at 25, 50, 75, 100%
        for (p in listOf(25f, 50f, 75f, 100f)) {
            val y = h - (p / 100f) * h * 0.85f - h * 0.05f
            canvas.drawLine(0f, y, w, y, gridPaint)
            canvas.drawText("${p.toInt()}%", 4f, y - 4f, textPaint)
        }

        if (coreData.isEmpty() || coreData[0].isEmpty()) return
        val pts = coreData[0].size
        val stepX = if (pts > 1) w / (pts - 1).toFloat() else w

        coreData.forEachIndexed { idx, data ->
            if (data.size < 2) return@forEachIndexed
            val paint = Paint().apply {
                color = COLORS[idx % COLORS.size]
                strokeWidth = 2.5f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            val path = Path()
            data.forEachIndexed { i, v ->
                val x = i * stepX
                val y = h - (v / 100f) * h * 0.85f - h * 0.05f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, paint)
            // Label last point
            val lastV = data.last()
            val lx = (data.size - 1) * stepX
            val ly = h - (lastV / 100f) * h * 0.85f - h * 0.05f
            textPaint.color = COLORS[idx % COLORS.size]
            canvas.drawText("C$idx ${lastV.toInt()}%", (lx - 70f).coerceAtLeast(0f), (ly - 6f).coerceAtLeast(20f), textPaint)
            textPaint.color = 0xFFCCCCCC.toInt()
        }
    }
}
