package com.example.deviceinfo.feature.thermal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.feature.thermal.databinding.FragmentThermalBinding

class ThermalFragment : Fragment(), ShareableFragment {
    private var _b: FragmentThermalBinding? = null
    private val b get() = _b!!
    private var latestData: List<Pair<String, Float>> = emptyList()
    private var adapter: ThermalAdapter? = null
    private var lastThermalAlertTemp = -1f

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_b == null) return
            loadData()
            handler.postDelayed(this, 5000L)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentThermalBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()

        b.btnExport.setOnClickListener {
            val map = latestData.associate { (k, v) -> k to "${"%.1f".format(v)} \u00b0C" }
            ExportUtils.exportToFile(requireContext(), "Thermal", map)
        }

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    override fun onResume() { super.onResume(); handler.post(refreshRunnable) }
    override fun onPause()  { super.onPause();  handler.removeCallbacks(refreshRunnable) }

    private fun loadData() {
        latestData = DeviceUtils.getThermalInfo()

        // ── Feed primary (max) temp to line chart ──────────────────────
        val maxTemp = latestData.maxOfOrNull { it.second } ?: 0f
        b.tempChart.addDataPoint(maxTemp)
        // ───────────────────────────────────────────────────────────────

        val thermalItems = if (latestData.isNotEmpty())
            latestData.map { (n, t) -> ThermalItem(n, t) }
        else listOf(ThermalItem("Thermal", 0f))

        adapter = ThermalAdapter(thermalItems)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter

        checkThermalAlert(maxTemp)
    }

    private fun checkThermalAlert(maxTemp: Float) {
        val ctx = context ?: return
        val threshold = 50f
        if (maxTemp >= threshold && lastThermalAlertTemp < threshold) {
            lastThermalAlertTemp = maxTemp
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel("thermal_alert", "Thermal Alerts", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Alerts when device temperature is too high" }
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(ch)
            }
            val n = NotificationCompat.Builder(ctx, "thermal_alert")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("\ud83c\udf21\ufe0f Device Overheating")
                .setContentText("Temperature is ${"%.1f".format(maxTemp)}\u00b0C. Let your device cool down.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            try { NotificationManagerCompat.from(ctx).notify(3001, n) }
            catch (e: SecurityException) { /* permission not granted */ }
        }
        if (maxTemp < threshold - 5f) lastThermalAlertTemp = -1f
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("\ud83c\udf21\ufe0f Thermal Info")
        sb.appendLine("\u2500".repeat(17))
        latestData.forEach { (name, temp) -> sb.appendLine("$name: ${"%.1f".format(temp)} \u00b0C") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestData.associate { (k, v) -> k to "${"%.1f".format(v)} \u00b0C" }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
