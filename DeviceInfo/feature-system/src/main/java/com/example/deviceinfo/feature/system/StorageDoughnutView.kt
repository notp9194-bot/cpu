package com.example.deviceinfo.feature.system

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class StorageDoughnutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var storageTotalGb: Float = 0f; var storageUsedGb: Float = 0f
    var ramTotalGb: Float = 0f;     var ramUsedGb: Float = 0f

    private val arcUsed  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val arcFree  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x18000000 }
    private val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = 0xFF78909C.toInt() }

    fun update(storageTotalGb: Float, storageUsedGb: Float, ramTotalGb: Float, ramUsedGb: Float) {
        this.storageTotalGb = storageTotalGb; this.storageUsedGb = storageUsedGb
        this.ramTotalGb = ramTotalGb;         this.ramUsedGb = ramUsedGb
        invalidate()
    }

    private fun drawDonut(
        canvas: Canvas, cx: Float, cy: Float, r: Float,
        ratio: Float, color: Int,
        label: String, usedStr: String, freeStr: String
    ) {
        val sw = r * 0.36f
        arcFree.strokeWidth = sw; arcUsed.strokeWidth = sw
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, -90f, 360f, false, arcFree)
        val sweep = ratio.coerceIn(0f, 1f) * 360f
        arcUsed.color = color
        if (sweep > 1f) canvas.drawArc(oval, -90f, sweep, false, arcUsed)

        val pct = (ratio * 100).toInt()
        pctPaint.textSize = r * 0.44f; pctPaint.color = color
        canvas.drawText("$pct%", cx, cy + r * 0.16f, pctPaint)

        val below = cy + r + sw / 2 + 4f
        lblPaint.textSize = r * 0.26f; lblPaint.color = 0xFF37474F.toInt()
        canvas.drawText(label, cx, below + r * 0.28f, lblPaint)
        subPaint.textSize = r * 0.20f
        canvas.drawText(usedStr, cx, below + r * 0.54f, subPaint)
        canvas.drawText(freeStr, cx, below + r * 0.78f, subPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val half = w / 2f
        val r = minOf(half * 0.7f, h * 0.36f).coerceAtLeast(40f)
        val cy = r + 14f

        if (storageTotalGb > 0f) {
            val ratio = (storageUsedGb / storageTotalGb).coerceIn(0f, 1f)
            drawDonut(
                canvas, half / 2f, cy, r, ratio, 0xFF7B2FBE.toInt(), "Storage",
                "Used: ${"%.1f".format(storageUsedGb)} GB",
                "Free: ${"%.1f".format((storageTotalGb - storageUsedGb).coerceAtLeast(0f))} GB"
            )
        }
        if (ramTotalGb > 0f) {
            val ratio = (ramUsedGb / ramTotalGb).coerceIn(0f, 1f)
            val col = if (ratio > 0.85f) 0xFFF44336.toInt() else 0xFF2196F3.toInt()
            drawDonut(
                canvas, half + half / 2f, cy, r, ratio, col, "RAM",
                "Used: ${"%.1f".format(ramUsedGb)} GB",
                "Free: ${"%.1f".format((ramTotalGb - ramUsedGb).coerceAtLeast(0f))} GB"
            )
        }
    }
}
