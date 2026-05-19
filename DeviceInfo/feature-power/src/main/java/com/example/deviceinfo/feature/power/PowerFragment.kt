package com.example.deviceinfo.feature.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
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
import com.example.deviceinfo.feature.power.databinding.FragmentPowerBinding
import kotlin.math.abs

class PowerFragment : Fragment(), ShareableFragment {
    private var _b: FragmentPowerBinding? = null
    private val b get() = _b!!
    private var receiver: BroadcastReceiver? = null
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_b == null) return
            loadData()
            handler.postDelayed(this, 3000L)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentPowerBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) { loadData() }
        }
        requireContext().registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    override fun onResume()  { super.onResume();  handler.post(refreshRunnable) }
    override fun onPause()   { super.onPause();   handler.removeCallbacks(refreshRunnable) }

    private fun loadData() {
        val ctx = context ?: return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = ctx.registerReceiver(null, filter) ?: return

        // ── Raw battery data ──────────────────────────────────────────
        val level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val pct     = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val status  = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val voltageV = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0  // mV → V
        val tempC    = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentNowUa  = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)   // µA
        val chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) // µAh
        val energyCounter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) else -1L

        // ── Watt calculation (real: V × I from actual API values) ─────
        val currentMa = if (currentNowUa != Int.MIN_VALUE) currentNowUa / 1000.0 else 0.0
        val currentA  = abs(currentMa) / 1000.0
        val watts     = (voltageV * currentA).toFloat().coerceAtLeast(0f)

        // ── Charging source ───────────────────────────────────────────
        val chargingTypeStr = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC       -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB      -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Unknown" else ""
        }

        // ── Time estimate (from real current + charge counter) ────────
        val chargeMah = if (chargeCounter > 0) chargeCounter / 1000 else 0
        val timeStr: String
        if (isCharging && watts > 0.5f && chargeMah > 0) {
            val estimatedCapacityMah = if (pct > 0) (chargeMah * 100 / pct) else 0
            val remainingMah = estimatedCapacityMah - chargeMah
            val hoursToFull = if (abs(currentMa) > 0) remainingMah / abs(currentMa) else 0.0
            val minutes = (hoursToFull * 60).toInt()
            timeStr = when {
                minutes <= 0  -> "Calculating…"
                minutes < 60  -> "$minutes min to full"
                else          -> "${minutes / 60}h ${minutes % 60}m to full"
            }
        } else if (!isCharging && abs(currentMa) > 0 && chargeMah > 0) {
            val hoursLeft = chargeMah / abs(currentMa)
            val minutes = (hoursLeft * 60).toInt()
            timeStr = when {
                minutes <= 0  -> "Calculating…"
                minutes < 60  -> "$minutes min remaining"
                else          -> "${minutes / 60}h ${minutes % 60}m remaining"
            }
        } else {
            timeStr = "N/A"
        }

        // ── Health ────────────────────────────────────────────────────
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD         -> "Good ✅"
            BatteryManager.BATTERY_HEALTH_OVERHEAT     -> "Overheat ⚠️"
            BatteryManager.BATTERY_HEALTH_DEAD         -> "Dead ❌"
            BatteryManager.BATTERY_HEALTH_COLD         -> "Too Cold"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage ⚠️"
            else -> "Unknown"
        }

        // ── Update gauge + chart ──────────────────────────────────────
        b.powerGauge.update(if (isCharging) watts else 0f, isCharging, chargingTypeStr)
        b.powerChart.addDataPoint(if (isCharging) watts else 0f)

        // ── Build info list (only real API data, no hardcoded tables) ─
        val items = mutableListOf<InfoItem>()

        items.add(InfoItem("LIVE POWER", "", true))
        items.add(InfoItem("Charging Power",  if (isCharging && watts > 0.1f) "${"%.2f".format(watts)} W" else "Not Charging", true))
        items.add(InfoItem("Voltage",         "${"%.3f".format(voltageV)} V"))
        items.add(InfoItem("Current",         if (currentNowUa != Int.MIN_VALUE) "${"%.0f".format(currentMa)} mA (${if (isCharging) "In" else "Out"})" else "Unavailable", true))
        items.add(InfoItem("Charging Source", chargingTypeStr.ifBlank { "Battery (Unplugged)" }))
        items.add(InfoItem("Temperature",     "${"%.1f".format(tempC)} °C", true))

        items.add(InfoItem("TIME ESTIMATE", "", true))
        items.add(InfoItem("Estimate",       timeStr, true))
        items.add(InfoItem("Battery Level",  "$pct%"))
        items.add(InfoItem("Charge Now",     if (chargeMah > 0) "$chargeMah mAh" else "Unavailable"))

        items.add(InfoItem("BATTERY HEALTH", "", true))
        if (energyCounter > 0 && energyCounter != Long.MIN_VALUE) {
            items.add(InfoItem("Energy Counter", "${"%.1f".format(energyCounter / 1_000_000.0)} mWh"))
        }
        items.add(InfoItem("Health Status",  health, true))

        latestItems = items
        adapter = InfoAdapter(items, "POWER")
        b.recyclerView.layoutManager = LinearLayoutManager(ctx)
        b.recyclerView.adapter = adapter

        val query = b.searchBar.etSearch.text?.toString() ?: ""
        if (query.isNotBlank()) adapter?.filter(query)
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("⚡ Power & Charging Info")
        sb.appendLine("─────────────────")
        latestItems.filter { it.value.isNotEmpty() && !it.isHighlighted }
            .forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestItems.filter { it.value.isNotEmpty() }.associate { it.label to it.value }

    override fun onDestroyView() {
        try { receiver?.let { context?.unregisterReceiver(it) } }
        catch (e: IllegalArgumentException) { /* not registered */ }
        receiver = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _b = null
    }
}
