package com.cpua.deviceinfo.feature.gpu

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * GPU capability doughnut — shows OpenGL ES version as a filled arc,
 * with max texture size and extension count displayed in the center.
 * Pure Canvas. No external library. Play Store safe.
 */
class GpuDoughnutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var glVersion: Float = 0f          // e.g. 3.2
    private var maxGlVersion: Float = 4.0f     // reference max (OpenGL ES 3.2)
    private var maxTexSize: Int = 0
    private var extCount: Int = 0
    private var gpuName: String = "GPU"

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x1A000000
        strokeCap = Paint.Cap.ROUND
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        color = 0xFF212121.toInt()
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFF78909C.toInt()
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun update(glVersionFloat: Float, maxTextureSizePx: Int, extensionCount: Int, rendererName: String) {
        glVersion    = glVersionFloat
        maxTexSize   = maxTextureSizePx
        extCount     = extensionCount
        gpuName      = rendererName.take(28)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.52f
        val r  = (minOf(cx, cy) - 12f).coerceAtLeast(30f)
        val sw = r * 0.28f

        trackPaint.strokeWidth = sw
        arcPaint.strokeWidth   = sw

        // Title
        titlePaint.textSize = (r * 0.22f).coerceIn(22f, 34f)
        canvas.drawText("OpenGL ES", cx, titlePaint.textSize + 6f, titlePaint)

        // Track (full circle)
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, -90f, 360f, false, trackPaint)

        // Arc — colored by version
        val ratio = if (maxGlVersion > 0f) (glVersion / maxGlVersion).coerceIn(0f, 1f) else 0f
        val sweep = ratio * 360f
        val arcColor = when {
            glVersion >= 3.1f -> 0xFF4CAF50.toInt()   // Green — modern
            glVersion >= 2.0f -> 0xFF2196F3.toInt()   // Blue  — good
            else              -> 0xFFFF9800.toInt()   // Orange — old
        }
        arcPaint.color = arcColor
        if (sweep > 0f) canvas.drawArc(oval, -90f, sweep, false, arcPaint)

        // Center: version number
        centerTextPaint.textSize = (r * 0.46f).coerceIn(28f, 56f)
        centerTextPaint.color = arcColor
        val versionStr = if (glVersion > 0f) "%.1f".format(glVersion) else "?"
        canvas.drawText(versionStr, cx, cy + centerTextPaint.textSize * 0.38f, centerTextPaint)

        // Sub-line below center
        subTextPaint.textSize = (r * 0.20f).coerceIn(16f, 28f)
        canvas.drawText("OpenGL ES", cx, cy + centerTextPaint.textSize * 0.38f + subTextPaint.textSize + 4f, subTextPaint)

        // Bottom badges: Tex size + Ext count
        val badgeY = cy + r + sw / 2f + (r * 0.22f)
        subTextPaint.textSize = (r * 0.18f).coerceIn(14f, 24f)
        val texStr = if (maxTexSize > 0) "Tex ${maxTexSize}px" else ""
        val extStr = if (extCount > 0) "$extCount ext" else ""
        canvas.drawText("$texStr  •  $extStr", cx, badgeY, subTextPaint)
    }
}
