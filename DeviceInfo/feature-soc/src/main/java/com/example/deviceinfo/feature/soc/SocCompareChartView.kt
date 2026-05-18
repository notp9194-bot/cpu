package com.cpua.deviceinfo.feature.soc

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * SoC Comparison Bar Chart.
 * Shows 4 metrics side-by-side for Your Device vs Selected Chip.
 * Pure Canvas. No external library. Play Store safe ✅
 *
 *  Metrics: Single-Thread · Multi-Thread · GPU · AI/NPU
 *  Each metric = 2 bars (your device in purple, reference in blue-grey)
 */
class SocCompareChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class CompareData(
        val yourChip: SocDatabase.SocEntry?,
        val refChip: SocDatabase.SocEntry
    )

    private var data: CompareData? = null

    // Paints
    private val yourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); style = Paint.Style.FILL
    }
    private val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF546E7A.toInt(); style = Paint.Style.FILL
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x18000000; style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF546E7A.toInt()
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.DEFAULT_BOLD
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.DEFAULT_BOLD
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.DEFAULT_BOLD
    }
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
    }
    private val noDataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt(); textAlign = Paint.Align.CENTER; textSize = 32f
    }

    fun update(d: CompareData) { data = d; invalidate() }

    private data class Metric(val label: String, val yourVal: Int, val refVal: Int, val accentColor: Int)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = data ?: run {
            canvas.drawText("Select a chip to compare", width / 2f, height / 2f, noDataPaint)
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val density = resources.displayMetrics.density

        val padTop    = 48f * density
        val padBottom = 56f * density
        val padLeft   = 16f * density
        val padRight  = 16f * density

        val metrics = listOf(
            Metric("Single\nThread", d.yourChip?.singleScore ?: 0, d.refChip.singleScore, 0xFFE91E63.toInt()),
            Metric("Multi\nThread",  d.yourChip?.multiScore  ?: 0, d.refChip.multiScore,  0xFF2196F3.toInt()),
            Metric("GPU",            d.yourChip?.gpuScore    ?: 0, d.refChip.gpuScore,    0xFF4CAF50.toInt()),
            Metric("AI / NPU",       d.yourChip?.aiScore     ?: 0, d.refChip.aiScore,     0xFFFF9800.toInt())
        )

        val groupCount = metrics.size
        val groupW = (w - padLeft - padRight) / groupCount
        val barW   = groupW * 0.28f
        val gap    = groupW * 0.06f
        val chartH = h - padTop - padBottom

        // Legend
        val legendY  = 30f * density
        val dotR     = 8f * density
        val legendX1 = w / 2 - 120f * density
        val legendX2 = w / 2 + 20f * density

        legendPaint.color = 0xFF7B2FBE.toInt()
        canvas.drawCircle(legendX1, legendY, dotR, legendPaint)
        legendPaint.color = 0xFF333333.toInt()
        canvas.drawText("Your Device", legendX1 + dotR + 6f * density, legendY + dotR * 0.4f, legendPaint)

        legendPaint.color = 0xFF546E7A.toInt()
        canvas.drawCircle(legendX2, legendY, dotR, legendPaint)
        legendPaint.color = 0xFF333333.toInt()
        canvas.drawText(d.refChip.name.take(18), legendX2 + dotR + 6f * density, legendY + dotR * 0.4f, legendPaint)

        // Metric label text size
        labelPaint.textSize = (groupW * 0.19f).coerceIn(20f * density, 28f * density)
        scorePaint.textSize = (groupW * 0.18f).coerceIn(18f * density, 26f * density)

        metrics.forEachIndexed { i, metric ->
            val groupLeft = padLeft + i * groupW
            val cx = groupLeft + groupW / 2f

            // Track (background)
            val trackLeft1 = cx - gap / 2 - barW
            val trackLeft2 = cx + gap / 2
            val trackTop   = padTop
            val trackBot   = padTop + chartH

            val r = barW * 0.25f
            canvas.drawRoundRect(RectF(trackLeft1, trackTop, trackLeft1 + barW, trackBot), r, r, trackPaint)
            canvas.drawRoundRect(RectF(trackLeft2, trackTop, trackLeft2 + barW, trackBot), r, r, trackPaint)

            // Your device bar (purple)
            val yourRatio = metric.yourVal / 100f
            val yourBarH  = chartH * yourRatio
            if (yourRatio > 0f) {
                val rect = RectF(trackLeft1, trackBot - yourBarH, trackLeft1 + barW, trackBot)
                yourPaint.color = 0xFF7B2FBE.toInt()
                canvas.drawRoundRect(rect, r, r, yourPaint)
            }

            // Reference bar (grey-blue)
            val refRatio = metric.refVal / 100f
            val refBarH  = chartH * refRatio
            if (refRatio > 0f) {
                val rect = RectF(trackLeft2, trackBot - refBarH, trackLeft2 + barW, trackBot)
                refPaint.color = metric.accentColor
                refPaint.alpha = 180
                canvas.drawRoundRect(rect, r, r, refPaint)
                refPaint.alpha = 255
            }

            // Score labels on top of bars
            scorePaint.color = 0xFF7B2FBE.toInt()
            if (metric.yourVal > 0) {
                canvas.drawText(
                    "${metric.yourVal}",
                    trackLeft1 + barW / 2,
                    trackBot - yourBarH - 4f * density,
                    scorePaint
                )
            }
            scorePaint.color = metric.accentColor
            canvas.drawText(
                "${metric.refVal}",
                trackLeft2 + barW / 2,
                trackBot - refBarH - 4f * density,
                scorePaint
            )

            // Metric label below chart
            val lineH = labelPaint.textSize * 1.3f
            val lines = metric.label.split("\n")
            lines.forEachIndexed { li, line ->
                canvas.drawText(
                    line,
                    cx,
                    trackBot + labelPaint.textSize + li * lineH,
                    labelPaint
                )
            }
        }
    }
}
