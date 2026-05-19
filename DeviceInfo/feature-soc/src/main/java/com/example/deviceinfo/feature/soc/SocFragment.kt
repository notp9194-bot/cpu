package com.example.deviceinfo.feature.soc

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
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.feature.soc.databinding.FragmentSocBinding

class SocFragment : Fragment(), ShareableFragment {
    private var _b: FragmentSocBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null
    private var maxFreqMhz: Long = 2000L

    private val handler = Handler(Looper.getMainLooper())

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_b == null) return
            loadData()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSocBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        maxFreqMhz = DeviceUtils.getMaxCpuFreq(0).let { if (it > 0) it else 2000L }
        loadData()
        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
        // Compare button hidden — SoC comparison used static hardcoded scores
        b.btnCompare.visibility = View.GONE
    }

    override fun onResume()  { super.onResume();  handler.post(refreshRunnable) }
    override fun onPause()   { super.onPause();   handler.removeCallbacks(refreshRunnable) }
    override fun onDestroyView() { handler.removeCallbacks(refreshRunnable); super.onDestroyView(); _b = null }

    private fun loadData() {
        if (_b == null) return
        val freqs = DeviceUtils.getCpuFrequencies()
        val cores = Runtime.getRuntime().availableProcessors()

        val snapshot = CpuClockHistoryManager.Snapshot(
            coreFreqsMhz = (0 until cores).map { i -> if (i < freqs.size) freqs[i] else 0L }
        )
        CpuClockHistoryManager.push(snapshot)

        val coreDataList = (0 until cores).map { i ->
            val freq = if (i < freqs.size) freqs[i] else 0L
            CpuBarChartView.CoreData(freq, maxFreqMhz)
        }
        b.cpuBarChart.updateCores(coreDataList)
        b.cpuHistoryChart.setData(CpuClockHistoryManager.getHistory(), maxFreqMhz)

        val maxFreqLabel = if (maxFreqMhz > 0) "$maxFreqMhz MHz" else "Unknown"
        val minFreqLabel = if (freqs.isNotEmpty()) "${freqs.minOrNull()} MHz" else "Unknown"
        val cpuInfo  = DeviceUtils.getCpuInfo()
        val hw       = cpuInfo["Hardware"] ?: Build.HARDWARE
        val gpuInfo  = DeviceUtils.getGpuInfo()

        val items = mutableListOf(
            InfoItem("SoC",          hw, true),
            InfoItem("Model",        if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else hw),
            InfoItem("Cores",        cores.toString()),
            InfoItem("Architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"),
            InfoItem("Processor",    cpuInfo["model name"] ?: cpuInfo["Processor"] ?: "Unknown"),
            InfoItem("Clock Speed",  "$minFreqLabel – $maxFreqLabel"),
        )
        repeat(cores) { i ->
            items.add(InfoItem("CPU $i", if (i < freqs.size) "${freqs[i]} MHz" else "Unknown", true))
        }
        items += listOf(
            InfoItem("GPU Vendor",        gpuInfo["GPU Vendor"]        ?: "Unknown"),
            InfoItem("GPU Renderer",      gpuInfo["GPU Renderer"]      ?: "Unknown"),
            InfoItem("OpenGL ES Version", gpuInfo["OpenGL ES Version"] ?: "Unknown"),
            InfoItem("Scaling Governor",  DeviceUtils.getCpuGovernor()),
            InfoItem("Vulkan Support",    if (Build.VERSION.SDK_INT >= 24) "Yes (API ${Build.VERSION.SDK_INT})" else "No")
        )
        latestItems = items

        val query = b.searchBar.etSearch.text?.toString() ?: ""
        adapter = InfoAdapter(items, "SOC")
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter
        if (query.isNotBlank()) adapter?.filter(query)
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("⚡ SOC / CPU Info")
        sb.appendLine("─".repeat(17))
        latestItems.forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestItems.associate { it.label to it.value }

    companion object { private const val REFRESH_INTERVAL_MS = 2000L }
}
