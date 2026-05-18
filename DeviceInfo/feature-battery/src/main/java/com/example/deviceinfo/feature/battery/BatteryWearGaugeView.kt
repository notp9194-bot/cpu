package com.cpua.deviceinfo.feature.battery

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Battery Wear Level + mAh Live gauge card.
 *
 * Shows:
 *   ① Animated arc gauge — wear level % (0 = new, 100 = dead)
 *   ② Large mAh remaining label (center)
 *   ③ Design capacity vs current capacity bars
 *   ④ Grade label: Excellent / Good / Fair / Poor / Replace
 *
 * Pure Canvas. No library. Play Store safe ✅
 */
class BatteryWearGaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class WearData(
        val chargeMah: Int,       // current charge remaining (mAh) from CHARGE_COUNTER
        val currentMa: Int,       // current now in mA (absolute), 0 if unknown
        val designMah: Int,       // design capacity estimate (mAh)
        val wearPct: Int,         // wear level 0-100
        val isCharging: Boolean,
        val voltageMv: Int,
        val tempC: Float
    )

    private var data: WearData? = null
    private var animWear = 0f

    private val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1100
        interpolator = android.view.animation.DecelerateInterpolator()
        addUpdateListener {
            val d = data ?: return@addUpdateListener
            animWear = d.wearPct * (it.animatedValue as Float)
            invalidate()
        }
    }

    // ── Paints ────────────────────────────────────────────────────────
    private val bgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = 0x1A000000
    }
    private val wearArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val healthArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val mahPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = 0xFF78909C.toInt()
    }
    private val gradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = 0xFF888888.toInt()
    }
    private val barTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = 0x15000000
    }
    private val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val barLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF546E7A.toInt()
    }

    fun update(d: WearData) {
        data = d
        animator.cancel()
        animWear = 0f
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    private fun gradeColor(wearPct: Int): Int = when {
        wearPct <= 15 -> 0xFF4CAF50.toInt()   // green  – Excellent
        wearPct <= 30 -> 0xFF8BC34A.toInt()   // light green – Good
        wearPct <= 50 -> 0xFFFF9800.toInt()   // orange – Fair
        wearPct <= 70 -> 0xFFFF5722.toInt()   // deep orange – Poor
        else          -> 0xFFF44336.toInt()   // red    – Replace
    }

    private fun gradeLabel(wearPct: Int): String = when {
        wearPct <= 15 -> "Excellent"
        wearPct <= 30 -> "Good"
        wearPct <= 50 -> "Fair"
        wearPct <= 70 -> "Poor"
        else          -> "⚠ Replace"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = data ?: run {
            subPaint.textSize = 32f
            canvas.drawText("Loading…", width / 2f, height / 2f, subPaint)
            return
        }

        val w  = width.toFloat()
        val h  = height.toFloat()
        val dp = resources.displayMetrics.density

        // ── Arc gauge (top half) ───────────────────────────────────────
        val arcR  = (minOf(w, h * 0.75f) / 2f - 18f * dp).coerceAtLeast(40f * dp)
        val cx    = w / 2f
        val cy    = h * 0.44f
        val sw    = arcR * 0.14f
        val oval  = RectF(cx - arcR, cy - arcR, cx + arcR, cy + arcR)

        bgArcPaint.strokeWidth = sw

        // Outer arc = wear (red-orange; how much capacity is LOST)
        val wearColor = gradeColor(d.wearPct)
        wearArcPaint.strokeWidth = sw
        wearArcPaint.color = wearColor

        // Inner arc (smaller) = remaining health (green)
        val innerR = arcR - sw - 6f * dp
        val innerOval = RectF(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
        healthArcPaint.strokeWidth = sw * 0.7f
        healthArcPaint.color = 0xFF4CAF50.toInt()

        val startAngle = 150f
        val sweepTotal = 240f

        // Background arcs
        canvas.drawArc(oval, startAngle, sweepTotal, false, bgArcPaint)
        bgArcPaint.strokeWidth = sw * 0.7f
        canvas.drawArc(innerOval, startAngle, sweepTotal, false, bgArcPaint)

        // Wear arc (worn = bad, so fill from start proportional to wear%)
        val wearSweep = (animWear / 100f) * sweepTotal
        if (wearSweep > 0f) canvas.drawArc(oval, startAngle, wearSweep, false, wearArcPaint)

        // Health arc (remaining = 100 - wear)
        val healthPct = (100 - d.wearPct).coerceIn(0, 100)
        val healthSweep = (healthPct / 100f) * sweepTotal
        if (healthSweep > 0f) {
            healthArcPaint.strokeWidth = sw * 0.7f
            canvas.drawArc(innerOval, startAngle, healthSweep, false, healthArcPaint)
        }

        // ── Center mAh ────────────────────────────────────────────────
        mahPaint.textSize  = arcR * 0.40f
        mahPaint.color     = wearColor
        val mahStr = if (d.chargeMah > 0) "${d.chargeMah}" else "---"
        canvas.drawText(mahStr, cx, cy + arcR * 0.12f, mahPaint)

        unitPaint.textSize = arcR * 0.18f
        canvas.drawText("mAh remaining", cx, cy + arcR * 0.34f, unitPaint)

        // ── Grade label ───────────────────────────────────────────────
        gradePaint.textSize = arcR * 0.22f
        gradePaint.color    = wearColor
        canvas.drawText("Wear: ${d.wearPct}%  •  ${gradeLabel(d.wearPct)}", cx, cy - arcR * 0.32f, gradePaint)

        // ── Bottom info bars ──────────────────────────────────────────
        val barTop    = cy + arcR * 0.58f
        val barHeight = 14f * dp
        val barRadius = 7f * dp
        val barLeft   = 20f * dp
        val barRight  = w - 20f * dp
        val barW      = barRight - barLeft
        val lineGap   = barHeight + 22f * dp
        barLabelPaint.textSize = 11f * dp

        // Row 1: Design Capacity
        val designRatio = if (d.designMah > 0) (d.chargeMah.toFloat() / d.designMah).coerceIn(0f, 1f) else 0f
        canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barTop + barHeight), barRadius, barRadius, barTrackPaint)
        if (designRatio > 0f) {
            barFillPaint.color = 0xFF4CAF50.toInt()
            canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + barW * designRatio, barTop + barHeight), barRadius, barRadius, barFillPaint)
        }
        barLabelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Current: ${d.chargeMah} mAh", barLeft, barTop + barHeight + 12f * dp, barLabelPaint)
        barLabelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Design: ~${d.designMah} mAh", barRight, barTop + barHeight + 12f * dp, barLabelPaint)

        // Row 2: Current flow
        val curTop = barTop + lineGap
        val maxCurrent = 5000 // 5A max scale
        val curRatio = (d.currentMa.toFloat() / maxCurrent).coerceIn(0f, 1f)
        canvas.drawRoundRect(RectF(barLeft, curTop, barRight, curTop + barHeight), barRadius, barRadius, barTrackPaint)
        if (curRatio > 0f) {
            barFillPaint.color = if (d.isCharging) 0xFF2196F3.toInt() else 0xFFFF9800.toInt()
            canvas.drawRoundRect(RectF(barLeft, curTop, barLeft + barW * curRatio, curTop + barHeight), barRadius, barRadius, barFillPaint)
        }
        val curStr = if (d.currentMa > 0) "${d.currentMa} mA  •  ${if (d.isCharging) "⚡ Charging" else "🔋 Draining"}" else "Current: Unknown"
        val powerW  = if (d.currentMa > 0 && d.voltageMv > 0) "  •  %.2fW".format(d.currentMa * d.voltageMv / 1_000_000f) else ""
        barLabelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(curStr + powerW, barLeft, curTop + barHeight + 12f * dp, barLabelPaint)
    }
}
