package com.example.deviceinfo.feature.system

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Storage Category Breakdown — Stacked Horizontal Bar Chart
 *
 * Shows internal storage split by category:
 *   Apps · Images · Videos · Audio · Downloads · Other · Free
 *
 * Data source: Environment.getExternalStorageDirectory() sub-dirs
 * No root, no special permissions, Play Store safe ✅
 *
 * API: call setCategories(list) then the view redraws automatically.
 */
class StorageCategoryBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class CategoryItem(
        val label: String,
        val bytes: Long,
        val color: Int
    )

    private var categories: List<CategoryItem> = emptyList()
    private var totalBytes: Long = 1L

    // Paints
    private val segPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }
    private val subPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        color = 0xFF90A4AE.toInt()
    }
    private val dotPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x14000000
        style = Paint.Style.FILL
    }
    private val roundRect  = RectF()

    fun setCategories(cats: List<CategoryItem>, total: Long) {
        categories = cats
        totalBytes = if (total > 0) total else 1L
        invalidate()
    }

    private fun fmtBytes(b: Long): String = when {
        b >= 1_073_741_824L -> "${"%.1f".format(b / 1_073_741_824f)} GB"
        b >= 1_048_576L     -> "${"%.0f".format(b / 1_048_576f)} MB"
        b >= 1_024L         -> "${"%.0f".format(b / 1_024f)} KB"
        else                -> "$b B"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (categories.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val dp = resources.displayMetrics.density

        val barH    = 22f * dp
        val barTop  = 14f * dp
        val barBot  = barTop + barH
        val radius  = barH / 2f
        val padH    = 16f * dp
        val barW    = w - padH * 2f

        // Background track
        roundRect.set(padH, barTop, w - padH, barBot)
        canvas.drawRoundRect(roundRect, radius, radius, bgPaint)

        // Draw stacked segments
        var x = padH
        val totalUsed = categories.sumOf { it.bytes }
        categories.forEach { cat ->
            if (cat.bytes <= 0L) return@forEach
            val ratio = cat.bytes.toFloat() / totalBytes.toFloat()
            val segW  = (ratio * barW).coerceAtLeast(0f)

            segPaint.color = cat.color
            roundRect.set(x, barTop, x + segW, barBot)
            canvas.drawRect(roundRect, segPaint)
            x += segW
        }

        // Round caps on left and right edges
        // Left cap
        if (categories.isNotEmpty()) {
            segPaint.color = categories.first().color
            roundRect.set(padH, barTop, padH + radius * 2, barBot)
            canvas.drawRoundRect(roundRect, radius, radius, segPaint)
        }
        // Right cap (free space = last item)
        if (categories.size > 1) {
            segPaint.color = categories.last().color
            roundRect.set(w - padH - radius * 2, barTop, w - padH, barBot)
            canvas.drawRoundRect(roundRect, radius, radius, segPaint)
        }

        // Legend rows — 2 columns
        val legendTop    = barBot + 14f * dp
        val rowH         = 26f * dp
        val colW         = w / 2f
        val dotR         = 5f * dp
        val dotPadding   = 10f * dp
        textPaint.textSize = 11f * dp
        subPaint.textSize  = 10f * dp

        categories.forEachIndexed { idx, cat ->
            val col   = idx % 2
            val row   = idx / 2
            val lx    = padH + col * colW
            val ly    = legendTop + row * rowH

            // Dot
            dotPaint.color = cat.color
            canvas.drawCircle(lx + dotR, ly + dotR, dotR, dotPaint)

            // Label
            textPaint.color = 0xFF37474F.toInt()
            canvas.drawText(cat.label, lx + dotR * 2 + dotPadding, ly + rowH * 0.52f, textPaint)

            // Size
            subPaint.color = 0xFF78909C.toInt()
            val pct = if (totalBytes > 0) (cat.bytes * 100 / totalBytes).toInt() else 0
            canvas.drawText("${fmtBytes(cat.bytes)}  $pct%",
                lx + dotR * 2 + dotPadding, ly + rowH * 0.88f, subPaint)
        }

        // Total used label centered below bar
        val usedPct = if (totalBytes > 0) (totalUsed * 100 / totalBytes).toInt() else 0
        textPaint.textSize  = 10f * dp
        textPaint.color     = 0xFF90A4AE.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Used: ${fmtBytes(totalUsed)} / ${fmtBytes(totalBytes)}  ($usedPct%)",
            w / 2f, barTop - 4f * dp, textPaint
        )
        textPaint.textAlign = Paint.Align.LEFT // reset
    }
}
