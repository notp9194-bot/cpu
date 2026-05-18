package com.cpua.deviceinfo.feature.audio

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Horizontal bar chart showing Media / Ring / Alarm / Notification volume.
 * Uses AudioManager.getStreamVolume — no special permission needed.
 * Pure Canvas — no external library. Play Store safe.
 */
class VolumeBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class VolumeStream(val label: String, val current: Int, val max: Int, val color: Int)

    private var streams: List<VolumeStream> = emptyList()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0x12000000 }
    private val barPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val lblPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF546E7A.toInt(); textSize = 26f; textAlign = Paint.Align.RIGHT
    }
    private val valPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f; typeface = Typeface.DEFAULT_BOLD
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6A1B9A.toInt(); textSize = 28f; typeface = Typeface.DEFAULT_BOLD
    }

    fun update(data: List<VolumeStream>) { streams = data; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (streams.isEmpty()) return
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 148f; val padR = 56f; val padT = 40f; val padB = 8f
        val chartW = w - padL - padR
        val rowH   = ((h - padT - padB) / streams.size).coerceAtLeast(22f)
        val barH   = (rowH * 0.52f).coerceAtLeast(10f)

        canvas.drawText("Volume Levels", padL, padT - 8f, titlePaint)

        streams.forEachIndexed { i, s ->
            val y    = padT + i * rowH + rowH / 2f
            val ratio = if (s.max > 0) s.current.toFloat() / s.max.toFloat() else 0f
            val barW  = ratio * chartW

            // label
            canvas.drawText(s.label, padL - 8f, y + 9f, lblPaint)

            // track
            canvas.drawRoundRect(padL, y - barH / 2, padL + chartW, y + barH / 2,
                barH / 2, barH / 2, trackPaint)

            // filled bar
            if (barW > 2f) {
                barPaint.color = s.color
                canvas.drawRoundRect(padL, y - barH / 2, padL + barW, y + barH / 2,
                    barH / 2, barH / 2, barPaint)
            }

            // value label to the right
            valPaint.color = s.color
            canvas.drawText("${s.current}/${s.max}", padL + chartW + 6f, y + 9f, valPaint)
        }
    }
}
