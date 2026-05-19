package com.example.deviceinfo.feature.display

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Arc gauge showing display refresh rate + resolution side card.
 * Uses DisplayManager — no special permission needed.
 * Pure Canvas — no external library. Play Store safe.
 */
class RefreshRateGaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var refreshRateHz: Float = 60f
    private var maxRateHz: Float = 120f
    private var resolutionStr: String = ""
    private var densityStr: String = ""
    private var sizeInch: String = ""

    private val arcBg   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x15000000; strokeCap = Paint.Cap.ROUND }
    private val arcFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xFF1E88E5.toInt(); strokeCap = Paint.Cap.ROUND }
    private val arcGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x441E88E5; strokeCap = Paint.Cap.ROUND }
    private val hzPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; color = 0xFF1E88E5.toInt()
    }
    private val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = 0xFF78909C.toInt()
    }
    private val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF546E7A.toInt(); textSize = 26f; typeface = Typeface.DEFAULT_BOLD
    }
    private val infoSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF78909C.toInt(); textSize = 24f
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1565C0.toInt(); textSize = 28f; typeface = Typeface.DEFAULT_BOLD
    }

    fun update(
        refreshRateHz: Float, maxRateHz: Float,
        resolutionStr: String, densityStr: String, sizeInch: String
    ) {
        this.refreshRateHz = refreshRateHz
        this.maxRateHz = maxRateHz.coerceAtLeast(refreshRateHz).coerceAtLeast(60f)
        this.resolutionStr = resolutionStr
        this.densityStr = densityStr
        this.sizeInch = sizeInch
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()

        // Split: left = gauge, right = info cards
        val gaugeW = w * 0.5f
        val cx = gaugeW / 2f
        val cy = h / 2f + 8f
        val r  = (minOf(cx - 16f, cy - 32f)).coerceAtLeast(30f)
        val sw = r * 0.26f

        canvas.drawText("Display Info", 16f, 30f, titlePaint)

        // Gauge arcs
        val startAngle = 150f; val sweepTotal = 240f
        arcBg.strokeWidth = sw; arcGlow.strokeWidth = sw + 6f; arcFill.strokeWidth = sw
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, startAngle, sweepTotal, false, arcBg)

        val ratio = (refreshRateHz / maxRateHz).coerceIn(0f, 1f)
        val sweep = ratio * sweepTotal
        if (sweep > 0f) {
            canvas.drawArc(oval, startAngle, sweep, false, arcGlow)
            canvas.drawArc(oval, startAngle, sweep, false, arcFill)
        }

        // Center Hz text
        hzPaint.textSize = r * 0.52f
        canvas.drawText("${"%.0f".format(refreshRateHz)}", cx, cy + r * 0.18f, hzPaint)
        lblPaint.textSize = r * 0.26f
        canvas.drawText("Hz", cx, cy + r * 0.46f, lblPaint)
        lblPaint.textSize = r * 0.22f
        canvas.drawText("/ ${"%.0f".format(maxRateHz)} max", cx, cy + r * 0.68f, lblPaint)

        // Min / max labels on arc
        lblPaint.textSize = r * 0.22f; lblPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("0", cx - r * 0.82f, cy + r * 0.72f, lblPaint)
        canvas.drawText("${"%.0f".format(maxRateHz)}", cx + r * 0.82f, cy + r * 0.72f, lblPaint)

        // Right panel — info cards
        val rx = gaugeW + 12f
        val cardW = w - rx - 12f
        val cardH = (h - 44f) / 3f
        drawInfoCard(canvas, rx, 44f, cardW, cardH, "\ud83d\udcf1 Resolution", resolutionStr)
        drawInfoCard(canvas, rx, 44f + cardH, cardW, cardH, "\u29bf Density", densityStr)
        drawInfoCard(canvas, rx, 44f + cardH * 2, cardW, cardH, "\ud83d\udcf0 Screen Size", sizeInch)
    }

    private fun drawInfoCard(canvas: Canvas, x: Float, y: Float, w: Float, h: Float,
                              title: String, value: String) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x0A1E88E5; style = Paint.Style.FILL }
        canvas.drawRoundRect(x, y + 4f, x + w, y + h - 4f, 12f, 12f, bgPaint)
        infoPaint.textSize = 22f
        canvas.drawText(title, x + 10f, y + h * 0.38f, infoPaint)
        infoSubPaint.textSize = 22f
        canvas.drawText(value, x + 10f, y + h * 0.72f, infoSubPaint)
    }
}
