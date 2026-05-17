package com.example.deviceinfo.feature.battery

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.feature.battery.databinding.FragmentBatteryBinding

class BatteryFragment : Fragment(), ShareableFragment {
    private var _b: FragmentBatteryBinding? = null
    private val b get() = _b!!
    private var receiver: BroadcastReceiver? = null
    private var latestData: Map<String, String> = emptyMap()
    private var adapter: InfoAdapter? = null

    private val handler = Handler(Looper.getMainLooper())
    private val ramRunnable = object : Runnable {
        override fun run() {
            if (_b == null) return
            val am = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val usedPct = if (mi.totalMem > 0)
                ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt()
            else 0
            b.ramChart.addDataPoint(usedPct)
            handler.postDelayed(this, 2000L)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentBatteryBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()

        // Live battery update via broadcast
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) { loadData() }
        }
        requireContext().registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        b.btnExport.setOnClickListener {
            ExportUtils.exportToFile(requireContext(), "Battery", latestData)
        }

        // Search / filter
        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    override fun onResume() { super.onResume(); handler.post(ramRunnable) }
    override fun onPause()  { super.onPause();  handler.removeCallbacks(ramRunnable) }

    private fun loadData() {
        if (_b == null) return
        latestData = DeviceUtils.getBatteryInfo(requireContext())

        // Add charging wattage to displayed data
        val displayData = LinkedHashMap(latestData)
        val wattage = computeWattage(requireContext())
        if (wattage != null) displayData["Charging Power"] = wattage

        val items = displayData.map { (k, v) -> InfoItem(k, v) }
        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter

        // Re-apply search if user already typed something
        val query = b.searchBar.etSearch.text?.toString() ?: ""
        if (query.isNotBlank()) adapter?.filter(query)

        b.batteryChart.addDataPoint(DeviceUtils.getBatteryPercent(requireContext()))
    }

    /**
     * Wattage = |currentNow (µA)| × voltage (mV) / 1_000_000_000
     * All values from BatteryManager — no permission needed.
     */
    private fun computeWattage(context: Context): String? {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            if (currentUa == Int.MIN_VALUE || voltageMv == 0) return null
            val watts = Math.abs(currentUa.toLong() * voltageMv) / 1_000_000_000.0
            val dir = if (currentUa > 0) "Charging" else "Draining"
            String.format("%.2f W (%s)", watts, dir)
        } catch (e: Exception) { null }
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🔋 Battery Info")
        sb.appendLine("─────────────────")
        latestData.forEach { (k, v) -> sb.appendLine("$k: $v") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() {
        receiver?.let { requireContext().unregisterReceiver(it) }
        receiver = null
        super.onDestroyView()
        _b = null
    }
}
