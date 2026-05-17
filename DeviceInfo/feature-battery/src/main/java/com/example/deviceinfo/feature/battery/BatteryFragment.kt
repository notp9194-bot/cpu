package com.example.deviceinfo.feature.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.feature.battery.databinding.FragmentBatteryBinding

class BatteryFragment : Fragment() {
    private var _b: FragmentBatteryBinding? = null
    private val b get() = _b!!
    private var receiver: BroadcastReceiver? = null

    // Cache latest data for export
    private var latestData: Map<String, String> = emptyMap()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentBatteryBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()

        // Live update when battery changes
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) { loadData() }
        }
        requireContext().registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Export button
        b.btnExport.setOnClickListener {
            ExportUtils.exportToFile(requireContext(), "Battery", latestData)
        }
    }

    private fun loadData() {
        if (_b == null) return
        latestData = DeviceUtils.getBatteryInfo(requireContext())
        val items = latestData.map { (k, v) -> InfoItem(k, v) }

        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = InfoAdapter(items)

        // Add data point to chart (battery %)
        val pct = DeviceUtils.getBatteryPercent(requireContext())
        b.batteryChart.addDataPoint(pct)
    }

    override fun onDestroyView() {
        receiver?.let { requireContext().unregisterReceiver(it) }
        receiver = null
        super.onDestroyView()
        _b = null
    }
}
