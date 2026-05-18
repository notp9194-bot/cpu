package com.example.deviceinfo.feature.security

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Horizontal security patch timeline.
 * Shows security patch age in months on a color-coded bar:
 * Green = fresh (< 3 months), Orange = aging (3–6), Red = old (> 6 months).
 * Pure Canvas. No external library. Play Store safe.
 */
class SecurityTimelineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var patchDateStr: String = ""
    private var ageMonths: Int = 0
    private var patchLabel: String = ""

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x18000000; style = Paint.Style.FILL }
    private val fillPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7B2FBE.toInt(); typeface = Typeface.DEFAULT_BOLD; textSize = 28f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF546E7A.toInt(); textSize = 24f; textAlign = Paint.Align.CENTER
    }
    private val agePaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD; textSize = 26f; textAlign = Paint.Align.LEFT
    }
    private val tickPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44000000; strokeWidth = 1.5f }

    fun update(patchDate: String) {
        patchDateStr = patchDate
        ageMonths = computeAgeMonths(patchDate)
        patchLabel = patchDate
        invalidate()
    }

    private fun computeAgeMonths(dateStr: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val patch = sdf.parse(dateStr) ?: return -1
            val now   = Calendar.getInstance().time
            val diff  = abs(now.time - patch.time)
            (diff / (1000L * 60 * 60 * 24 * 30)).toInt()
        } catch (_: Exception) { -1 }
    }

    private fun colorForAge(months: Int): Int = when {
        months < 0  -> 0xFF9E9E9E.toInt()  // unknown
        months <= 3 -> 0xFF4CAF50.toInt()  // green — fresh
        months <= 6 -> 0xFFFF9800.toInt()  // orange — aging
        else        -> 0xFFF44336.toInt()  // red — old
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 16f; val padR = 16f; val padT = 40f
        val barH = 28f; val barW = w - padL - padR
        val rx = barH / 2f

        canvas.drawText("Security Patch Timeline", padL, padT - 8f, titlePaint)

        // Track bar (12 months = full width)
        val trackRect = RectF(padL, padT, padL + barW, padT + barH)
        canvas.drawRoundRect(trackRect, rx, rx, trackPaint)

        // Tick marks at 3, 6, 9 months
        val tickY1 = padT - 4f; val tickY2 = padT + barH + 4f
        for (m in listOf(3, 6, 9, 12)) {
            val tx = padL + barW * (m / 12f)
            canvas.drawLine(tx, tickY1, tx, tickY2, tickPaint)
            canvas.drawText("${m}mo", tx, padT + barH + 22f, labelPaint)
        }

        // Filled bar
        val maxMonths = 12
        val fillRatio = if (ageMonths >= 0)
            (ageMonths.toFloat() / maxMonths).coerceIn(0f, 1f)
        else 0f

        val color = colorForAge(ageMonths)
        fillPaint.color = color
        if (fillRatio > 0.01f) {
            val fillRect = RectF(padL, padT, padL + barW * fillRatio, padT + barH)
            canvas.drawRoundRect(fillRect, rx, rx, fillPaint)
        }

        // Dot at current position
        dotPaint.color = color
        val dotX = padL + barW * fillRatio
        canvas.drawCircle(dotX, padT + barH / 2f, barH * 0.7f, dotPaint)

        // Age label on the right
        val ageStr = when {
            ageMonths < 0    -> "Unknown"
            ageMonths == 0   -> "This month ✅"
            ageMonths == 1   -> "1 month ago"
            ageMonths <= 3   -> "$ageMonths months ago ✅"
            ageMonths <= 6   -> "$ageMonths months ago ⚠️"
            else             -> "$ageMonths months ago ❌"
        }
        agePaint.color = color
        canvas.drawText(ageStr, padL, padT + barH + 44f, agePaint)

        // Date label right-aligned
        agePaint.textAlign = Paint.Align.RIGHT
        agePaint.color = 0xFF546E7A.toInt()
        canvas.drawText(patchLabel, padL + barW, padT + barH + 44f, agePaint)
        agePaint.textAlign = Paint.Align.LEFT
    }
}
