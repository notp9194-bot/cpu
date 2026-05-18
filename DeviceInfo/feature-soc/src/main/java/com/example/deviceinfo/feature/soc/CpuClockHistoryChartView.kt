package com.example.deviceinfo.feature.soc

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * CPU Clock History Chart — last 60 seconds, one line per core.
 *
 * Visual features:
 *  • One color-coded line per CPU core (up to 8 cores shown)
 *  • Y-axis: 0 → maxFreq (MHz or GHz adaptive)
 *  • X-axis: "–60s" → "Now" time labels
 *  • Horizontal dashed grid lines at 25 / 50 / 75 % of max
 *  • Per-core legend row showing current MHz
 *  • "No data yet" placeholder on first open
 *
 * No permissions, no root. Play Store safe ✅
 */
class CpuClockHistoryChartView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    // ── Data ──────────────────────────────────────────────────────────
    private var history: List<CpuClockHistoryManager.Snapshot> = emptyList()
    private var maxFreqMhz: Long = 2000L

    fun setData(history: List<CpuClockHistoryManager.Snapshot>, maxFreqMhz: Long) {
        this.history    = history
        this.maxFreqMhz = if (maxFreqMhz > 0) maxFreqMhz else 2000L
        invalidate()
    }

    // ── Palette — 8 distinct core colors ─────────────────────────────
    private val coreColors = intArrayOf(
        0xFF4FC3F7.toInt(),  // 0 sky blue
        0xFF81C784.toInt(),  // 1 green
        0xFFFFB74D.toInt(),  // 2 orange
        0xFFE57373.toInt(),  // 3 red
        0xFFBA68C8.toInt(),  // 4 purple
        0xFF4DD0E1.toInt(),  // 5 cyan
        0xFFF06292.toInt(),  // 6 pink
        0xFFA1887F.toInt(),  // 7 brown
    )

    // ── Paints ────────────────────────────────────────────────────────
    private val linePaints = Array(8) { i ->
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color       = coreColors[i]
            style       = Paint.Style.STROKE
            strokeWidth = dp(1.8f)
            strokeCap   = Paint.Cap.ROUND
            strokeJoin  = Paint.Join.ROUND
        }
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = dp(0.7f)
        color       = 0x22FFFFFF
        pathEffect  = DashPathEffect(floatArrayOf(dp(5f), dp(4f)), 0f)
    }

    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = 0xFF888888.toInt()
        textSize  = dp(9f)
        textAlign = Paint.Align.RIGHT
    }

    private val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = 0xFF888888.toInt()
        textSize  = dp(9f)
        textAlign = Paint.Align.CENTER
    }

    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(9f)
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = 0xFF7B2FBE.toInt()
        textSize = dp(11f)
        typeface = Typeface.DEFAULT_BOLD
    }

    private val noDataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = 0xFF888888.toInt()
        textSize  = dp(12f)
        textAlign = Paint.Align.CENTER
    }

    // ── Layout constants ──────────────────────────────────────────────
    private val padL  get() = dp(44f)   // Y-axis labels
    private val padR  get() = dp(8f)
    private val padT  get() = dp(24f)   // title
    private val padB  get() = dp(28f)   // X-axis labels + legend

    // ── Draw ─────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val W = width.toFloat()
        val H = height.toFloat()

        val chartL = padL
        val chartR = W - padR
        val chartT = padT
        val chartB = H - padB
        val chartW = chartR - chartL
        val chartH = chartB - chartT

        // Title
        canvas.drawText("CPU Clock History — 60s", chartL, chartT - dp(6f), titlePaint)

        val coreCount = history.firstOrNull()?.coreFreqsMhz?.size?.coerceAtMost(8) ?: 0

        // No data placeholder
        if (history.isEmpty() || coreCount == 0) {
            canvas.drawText("Collecting data…", W / 2f, chartT + chartH / 2f + dp(4f), noDataPaint)
            return
        }

        // ── Grid lines at 0 / 25 / 50 / 75 / 100 % ──────────────────
        listOf(0, 25, 50, 75, 100).forEach { pct ->
            val y = chartB - pct / 100f * chartH
            canvas.drawLine(chartL, y, chartR, y, gridPaint)

            // Y-axis label
            val freqVal = maxFreqMhz * pct / 100L
            val label   = if (freqVal >= 1000) "${"%.1f".format(freqVal / 1000f)}G"
                          else "${freqVal}M"
            axisLabelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, chartL - dp(3f), y + dp(3f), axisLabelPaint)
        }

        // ── X-axis labels: –60s … Now ────────────────────────────────
        val totalSec = CpuClockHistoryManager.MAX_POINTS * CpuClockHistoryManager.SAMPLE_INTERVAL_MS / 1000
        listOf(0, 25, 50, 75, 100).forEach { pct ->
            val x    = chartL + pct / 100f * chartW
            val secAgo = totalSec - (pct * totalSec / 100)
            val label  = if (secAgo == 0L) "Now" else "–${secAgo}s"
            canvas.drawText(label, x, chartB + dp(12f), xLabelPaint)
        }

        // ── Per-core lines ────────────────────────────────────────────
        val n = history.size
        for (coreIdx in 0 until coreCount) {
            val path = Path()
            var started = false

            history.forEachIndexed { snapIdx, snap ->
                val freq = snap.coreFreqsMhz.getOrElse(coreIdx) { 0L }
                val x    = chartL + snapIdx.toFloat() / (CpuClockHistoryManager.MAX_POINTS - 1).coerceAtLeast(1) * chartW
                val y    = chartB - (freq.toFloat() / maxFreqMhz).coerceIn(0f, 1f) * chartH

                if (!started) { path.moveTo(x, y); started = true }
                else          path.lineTo(x, y)
            }
            canvas.drawPath(path, linePaints[coreIdx % 8])
        }

        // ── Legend row (compact, 4 per row if > 4 cores) ─────────────
        val latestSnap = history.last()
        val dotR       = dp(4f)
        val legendY    = chartB + dp(22f)
        val colW       = chartW / coreCount.coerceAtLeast(1)

        for (coreIdx in 0 until coreCount) {
            val color = coreColors[coreIdx % 8]
            val freq  = latestSnap.coreFreqsMhz.getOrElse(coreIdx) { 0L }
            val label = if (freq >= 1000) "${"%.1f".format(freq / 1000f)}G"
                        else "${freq}M"

            val x = chartL + coreIdx * colW + colW / 2f

            // Dot
            legendPaint.color = color
            legendPaint.style = Paint.Style.FILL
            canvas.drawCircle(x - dp(14f), legendY - dp(3f), dotR, legendPaint)

            // "C0 1.8G" text
            legendPaint.style     = Paint.Style.FILL
            legendPaint.color     = color
            legendPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("C$coreIdx $label", x - dp(10f), legendY, legendPaint)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val defH = dp(200f).toInt()
        val w    = MeasureSpec.getSize(wSpec)
        val h    = when (MeasureSpec.getMode(hSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(hSpec)
            MeasureSpec.AT_MOST -> minOf(defH, MeasureSpec.getSize(hSpec))
            else                 -> defH
        }
        setMeasuredDimension(w, h)
    }
}
