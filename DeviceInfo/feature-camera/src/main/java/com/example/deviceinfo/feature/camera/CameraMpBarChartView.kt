package com.example.deviceinfo.feature.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Bar chart showing each camera's megapixels side by side.
 * Pure Canvas — no external library. No CAMERA permission needed
 * (only CameraManager.getCameraCharacteristics which is read-only).
 */
class CameraMpBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class CameraBar(val label: String, val mp: Double, val color: Int)

    private var bars: List<CameraBar> = emptyList()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0x12000000 }
    private val barPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val lblPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = 0xFF607D8B.toInt(); textSize = 26f
    }
    private val mpPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; color = 0xFFFFFFFF.toInt(); textSize = 24f
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1565C0.toInt(); textSize = 28f; typeface = Typeface.DEFAULT_BOLD
    }

    fun update(cameras: List<CameraBar>) { bars = cameras; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) { return }
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 12f; val padR = 12f; val padT = 40f; val padB = 36f
        val chartH = h - padT - padB
        val n = bars.size
        val slotW = (w - padL - padR) / n
        val barW  = (slotW - 12f).coerceAtLeast(10f)
        val maxMp = bars.maxOf { it.mp }.coerceAtLeast(1.0)

        canvas.drawText("Camera Megapixels", padL + 4f, padT - 8f, titlePaint)

        bars.forEachIndexed { i, cam ->
            val cx   = padL + i * slotW + slotW / 2f
            val ratio = (cam.mp / maxMp).toFloat().coerceIn(0f, 1f)
            val barH  = ratio * chartH
            val rx    = 10f

            // track
            canvas.drawRoundRect(cx - barW / 2, padT, cx + barW / 2, padT + chartH, rx, rx, trackPaint)

            // filled bar
            if (barH > 2f) {
                barPaint.color = cam.color
                canvas.drawRoundRect(
                    cx - barW / 2, padT + chartH - barH,
                    cx + barW / 2, padT + chartH, rx, rx, barPaint
                )
            }

            // MP label inside bar
            if (barH > 44f) {
                canvas.drawText("${"%.0f".format(cam.mp)} MP", cx, padT + chartH - barH + 22f, mpPaint)
            }

            // camera label below
            canvas.drawText(cam.label, cx, padT + chartH + 26f, lblPaint)
        }
    }
}
