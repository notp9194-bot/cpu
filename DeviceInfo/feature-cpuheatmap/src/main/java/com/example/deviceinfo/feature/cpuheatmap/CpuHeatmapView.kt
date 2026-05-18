package com.cpua.deviceinfo.feature.cpuheatmap

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View
import java.io.BufferedReader; import java.io.FileReader

class CpuHeatmapView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    // [core][time] grid  — 8 cores × 60 samples
    private val CORES = 8; private val COLS = 60
    private val grid = Array(CORES) { FloatArray(COLS) { 0f } }
    private var maxFreqKhz = 3000000L
    private val tp = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize = 22f; isAntiAlias = true }
    private val bg = Paint().apply { color = 0xFF111111.toInt() }

    fun pushSample(freqsKhz: List<Long>) {
        for (c in 0 until CORES) {
            for (t in 0 until COLS - 1) grid[c][t] = grid[c][t + 1]
            val f = if (c < freqsKhz.size) freqsKhz[c] else 0L
            grid[c][COLS - 1] = if (maxFreqKhz > 0) (f.toFloat() / maxFreqKhz).coerceIn(0f, 1f) else 0f
        }
        invalidate()
    }

    fun setMaxFreq(khz: Long) { maxFreqKhz = khz }

    private fun heatColor(v: Float): Int {
        val r = (v * 255).toInt().coerceIn(0, 255)
        val g = ((1f - kotlin.math.abs(v * 2 - 1f)) * 255).toInt().coerceIn(0, 255)
        val b = ((1f - v) * 200).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
        val labelW = 52f; val availW = width - labelW; val availH = height - 24f
        val cellW = availW / COLS; val cellH = availH / CORES
        val paint = Paint()
        for (c in 0 until CORES) {
            for (t in 0 until COLS) {
                paint.color = heatColor(grid[c][t])
                canvas.drawRect(labelW + t * cellW, c * cellH, labelW + (t + 1) * cellW, (c + 1) * cellH, paint)
            }
            tp.textSize = 20f
            canvas.drawText("C$c", 2f, c * cellH + cellH * 0.7f, tp)
            val lastFreqMhz = (grid[c][COLS - 1] * maxFreqKhz / 1000).toInt()
            canvas.drawText("${lastFreqMhz}MHz", labelW + availW - 120f, c * cellH + cellH * 0.7f, tp)
        }
        // X-axis label
        tp.textSize = 20f; tp.textAlign = Paint.Align.CENTER
        canvas.drawText("← 60 seconds →", labelW + availW / 2, height - 4f, tp)
        tp.textAlign = Paint.Align.LEFT
        // Color scale legend
        val lx = labelW; val ly = (CORES) * cellH + 4f
        val lw = availW / 5f
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEachIndexed { i, v ->
            paint.color = heatColor(v)
            canvas.drawRect(lx + i * lw, ly, lx + (i + 1) * lw - 2f, ly + 12f, paint)
        }
        tp.textSize = 18f; canvas.drawText("Low", lx + 2f, ly + 24f, tp)
        canvas.drawText("High", lx + 4 * lw + 2f, ly + 24f, tp)
    }
}
