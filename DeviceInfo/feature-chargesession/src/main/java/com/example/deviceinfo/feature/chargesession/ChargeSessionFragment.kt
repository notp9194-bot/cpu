package com.example.deviceinfo.feature.chargesession

import android.content.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.deviceinfo.core.ui.ShareableFragment

class ChargeSessionFragment : Fragment(), ShareableFragment {

    private var rootView: View? = null
    private var chart: ChargeSessionBarChart? = null
    private var tvSummary: TextView? = null
    private var receiver: BroadcastReceiver? = null

    // Session tracking
    private var sessionStart = -1
    private var sessionStartTime = 0L
    private var maxTemp = 0f
    private var peakCurrent = 0
    private var tempSum = 0f; private var tempCount = 0
    private val handler = Handler(Looper.getMainLooper())

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (rootView == null) return
            pollBatteryStats()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val scroll = ScrollView(requireContext())
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; val p = dp(12); setPadding(p, p, p, p)
        }
        scroll.addView(root); rootView = scroll

        root.addView(TextView(requireContext()).apply {
            text = "🔌 Charge Session Logger"; textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, dp(6))
        })

        root.addView(TextView(requireContext()).apply {
            text = "Blue = Start %, Green = Charge Gained"; textSize = 11f; alpha = 0.7f
            setPadding(0, 0, 0, dp(4))
        })

        chart = ChargeSessionBarChart(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(200))
        }
        root.addView(chart)

        tvSummary = TextView(requireContext()).apply {
            textSize = 12f; setPadding(0, dp(10), 0, 0); setTextColor(0xFFCCCCCC.toInt())
        }
        root.addView(tvSummary)

        val btnClear = Button(requireContext()).apply {
            text = "Clear History"
            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Clear session history?")
                    .setPositiveButton("Clear") { _, _ ->
                        ChargeSessionManager.clear(requireContext()); loadSessions()
                    }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        root.addView(btnClear)

        loadSessions()
        return scroll
    }

    override fun onResume() {
        super.onResume()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) { handleBattery(intent) }
        }
        requireContext().registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        handler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(pollRunnable)
        try { receiver?.let { requireContext().unregisterReceiver(it) } } catch (_: Exception) {}
    }

    override fun onDestroyView() { super.onDestroyView(); rootView = null; chart = null }

    private fun handleBattery(intent: Intent?) {
        val status  = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: return
        val level   = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
        val scale   = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
        val pct     = if (level >= 0 && scale > 0) level * 100 / scale else return
        val tempRaw = intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempC   = tempRaw / 10f
        val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == android.os.BatteryManager.BATTERY_STATUS_FULL

        if (isCharging) {
            if (sessionStart < 0) { sessionStart = pct; sessionStartTime = System.currentTimeMillis() }
            if (tempC > maxTemp) maxTemp = tempC
            tempSum += tempC; tempCount++
            val bm = requireContext().getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
            val cur = kotlin.math.abs(bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000)
            if (cur > peakCurrent) peakCurrent = cur
        } else if (sessionStart >= 0) {
            val durationMin = ((System.currentTimeMillis() - sessionStartTime) / 60_000).toInt()
            if (durationMin >= 2) {
                ChargeSessionManager.save(requireContext(), ChargeSession(
                    startPct = sessionStart, endPct = pct,
                    durationMin = durationMin,
                    avgTempC = if (tempCount > 0) tempSum / tempCount else tempC,
                    peakCurrentMa = peakCurrent,
                    date = ChargeSessionManager.today()
                ))
                loadSessions()
            }
            sessionStart = -1; tempSum = 0f; tempCount = 0; peakCurrent = 0; maxTemp = 0f
        }
    }

    private fun pollBatteryStats() {
        val intent = requireContext().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        handleBattery(intent)
    }

    private fun loadSessions() {
        val sessions = ChargeSessionManager.getAll(requireContext())
        chart?.setSessions(sessions)
        if (sessions.isEmpty()) {
            tvSummary?.text = "Plug in your device to start recording sessions."
        } else {
            val avgGain = sessions.map { it.endPct - it.startPct }.average()
            val avgDur  = sessions.map { it.durationMin }.average()
            tvSummary?.text = "📊 ${sessions.size} sessions recorded\n" +
                "Avg charge gain: ${"%.1f".format(avgGain)}%  |  Avg duration: ${"%.0f".format(avgDur)} min\n" +
                "Best session: ${sessions.maxByOrNull { it.endPct - it.startPct }?.let { "+${it.endPct - it.startPct}% in ${it.durationMin}min" } ?: "—"}"
        }
    }

    override fun getShareText() = "Charge Session Logger — ${ChargeSessionManager.getAll(requireContext()).size} sessions\nGenerated by CPU-A"
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
