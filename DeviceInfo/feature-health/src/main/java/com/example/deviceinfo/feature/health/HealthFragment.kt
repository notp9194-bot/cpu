package com.cpua.deviceinfo.feature.health

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import com.cpua.deviceinfo.core.ui.ShareableFragment
import com.cpua.deviceinfo.feature.health.databinding.FragmentHealthBinding
import java.util.concurrent.Executors

class HealthFragment : Fragment(), ShareableFragment {

    private var _b: FragmentHealthBinding? = null
    private val b get() = _b!!

    private val executor = Executors.newSingleThreadExecutor()
    private val handler  = Handler(Looper.getMainLooper())

    private var latestScore: DeviceHealthScorer.ScoreBreakdown? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentHealthBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        b.btnRefresh.setOnClickListener { computeScore() }
        b.btnShare.setOnClickListener  { shareScore() }
        computeScore()
        loadTrendChart()
    }

    // onResume does NOT re-trigger computeScore to avoid duplicate calls
    override fun onResume() { super.onResume() }

    private fun computeScore() {
        val ctx = context?.applicationContext ?: return
        executor.execute {
            val score = DeviceHealthScorer.compute(ctx)
            HealthScoreHistory.record(ctx, score.total, score.grade)
            handler.post {
                if (_b == null) return@post
                latestScore = score
                applyScore(score)
                loadTrendChart()
            }
        }
    }

    private fun applyScore(s: DeviceHealthScorer.ScoreBreakdown) {
        b.healthGauge.setScore(s.total, s.gradeColor, s.grade)
        b.tvTip.text = s.tip
        b.breakdownBars.setItems(listOf(
            HealthBreakdownBarView.BarItem("🔋 Battery Level",  s.batteryLevel,  30, 0xFF4CAF50.toInt()),
            HealthBreakdownBarView.BarItem("❤️ Battery Health", s.batteryHealth, 20, 0xFF8BC34A.toInt()),
            HealthBreakdownBarView.BarItem("🌡 Temperature",    s.batteryTemp,   15, 0xFF03A9F4.toInt()),
            HealthBreakdownBarView.BarItem("💾 Free Storage",   s.freeStorage,   20, 0xFFFF9800.toInt()),
            HealthBreakdownBarView.BarItem("💡 Free RAM",       s.freeRam,       15, 0xFF9C27B0.toInt()),
        ))
        b.tvBattPct.text    = "${s.batteryPct}%"
        b.tvTemp.text       = "${"%.1f".format(s.batteryTempC)}°C"
        b.tvBattHealth.text = s.healthLabel
        b.tvStorage.text    = "${"%.1f".format(s.storageFreeGb)} GB"
        b.tvRam.text        = "${"%.1f".format(s.ramFreeGb)} GB"
        b.tvScoreMini.text  = "${s.total}/100"
        val col = s.gradeColor
        b.tvBattPct.setTextColor(col)
        b.tvTemp.setTextColor(if (s.batteryTempC > 45f) 0xFFF44336.toInt() else 0xFF4CAF50.toInt())
        b.tvBattHealth.setTextColor(if (s.healthLabel == "Good") 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
        b.tvScoreMini.setTextColor(col)
    }

    private fun loadTrendChart() {
        val ctx = context?.applicationContext ?: return
        val entries = HealthScoreHistory.load(ctx)
        b.trendChart.setEntries(entries)
        val avg = HealthScoreHistory.average(ctx)
        if (avg != null) {
            b.tvTrendAvg.text = "7-Day Avg: $avg / 100"
            b.tvTrendAvg.visibility = android.view.View.VISIBLE
        } else {
            b.tvTrendAvg.visibility = android.view.View.GONE
        }
    }

    private fun shareScore() {
        val s = latestScore ?: return
        val avg = context?.applicationContext?.let { HealthScoreHistory.average(it) }
        val text = buildString {
            appendLine("📱 My Device Health Score")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Overall Score : ${s.total}/100 — ${s.grade}")
            if (avg != null) appendLine("7-Day Average : $avg / 100")
            appendLine()
            appendLine("🔋 Battery    : ${s.batteryPct}%  (${s.healthLabel})")
            appendLine("🌡 Temp       : ${"%.1f".format(s.batteryTempC)} °C")
            appendLine("💾 Storage    : ${"%.1f".format(s.storageFreeGb)} / ${"%.1f".format(s.storageTotalGb)} GB free")
            appendLine("💡 RAM        : ${"%.1f".format(s.ramFreeGb)} / ${"%.1f".format(s.ramTotalGb)} GB free")
            appendLine()
            appendLine(s.tip)
            appendLine()
            appendLine("Checked with CPU-A Device Info app")
        }
        startActivity(
            Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "My Device Health Score: ${s.total}/100")
            }, "Share Score via")
        )
    }

    override fun getShareText(): String {
        val s = latestScore ?: return "Device Health — not yet computed"
        return "Device Health Score: ${s.total}/100 (${s.grade})\n" +
               "Battery: ${s.batteryPct}% | Temp: ${"%.1f".format(s.batteryTempC)}°C | " +
               "Storage free: ${"%.1f".format(s.storageFreeGb)} GB | RAM free: ${"%.1f".format(s.ramFreeGb)} GB"
    }

    override fun getExportData(): Map<String, String> {
        val s   = latestScore ?: return emptyMap()
        val avg = context?.applicationContext?.let { HealthScoreHistory.average(it) }
        return linkedMapOf(
            "Health Score"   to "${s.total}/100",
            "Grade"          to s.grade,
            "7-Day Average"  to (avg?.let { "$it/100" } ?: "—"),
            "Battery Level"  to "${s.batteryPct}%",
            "Battery Health" to s.healthLabel,
            "Temperature"    to "${"%.1f".format(s.batteryTempC)} °C",
            "Storage Free"   to "${"%.1f".format(s.storageFreeGb)} / ${"%.1f".format(s.storageTotalGb)} GB",
            "RAM Free"       to "${"%.1f".format(s.ramFreeGb)} / ${"%.1f".format(s.ramTotalGb)} GB",
            "Tip"            to s.tip
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executor.shutdownNow()
        handler.removeCallbacksAndMessages(null)
        _b = null
    }
}
