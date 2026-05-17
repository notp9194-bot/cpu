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
import java.util.concurrent.Executors

class SocFragment : Fragment(), ShareableFragment {
    private var _b: FragmentSocBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null
    private var cpuUsages: List<Int> = emptyList()

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var running = false

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_b == null || !running) return
            // CPU usage needs blocking read — do on background thread
            executor.execute {
                val usages = try { DeviceUtils.getCpuUsagePercent() } catch (e: Exception) { emptyList() }
                handler.post {
                    if (_b != null) { cpuUsages = usages; loadData() }
                }
            }
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSocBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()
        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        running = true
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS)
    }

    override fun onPause() {
        super.onPause()
        running = false
        handler.removeCallbacks(refreshRunnable)
    }

    private fun loadData() {
        if (_b == null) return
        val freqs  = DeviceUtils.getCpuFrequencies()
        val cores  = Runtime.getRuntime().availableProcessors()
        val maxFreq= DeviceUtils.getMaxCpuFreq(0).let { if (it > 0) "$it MHz" else "Unknown" }
        val minFreq= if (freqs.isNotEmpty()) "${freqs.minOrNull()} MHz" else "Unknown"
        val cpuInfo= DeviceUtils.getCpuInfo()
        val hw     = cpuInfo["Hardware"] ?: Build.HARDWARE
        val gpuInfo= DeviceUtils.getGpuInfo()

        val items = mutableListOf(
            InfoItem("SoC",            hw, true),
            InfoItem("Model",          if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else hw),
            InfoItem("Cores",          cores.toString()),
            InfoItem("Architecture",   Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"),
            InfoItem("Processor",      cpuInfo["model name"] ?: cpuInfo["Processor"] ?: "Unknown"),
            InfoItem("Clock Speed",    "$minFreq - $maxFreq"),
        )

        // ── Per-core: freq + usage ────────────────────────────────────
        items.add(InfoItem("── Per Core ──", "", true))
        repeat(cores) { i ->
            val freqStr  = if (i < freqs.size) "${freqs[i]} MHz" else "Unknown"
            val usagePct = if (i < cpuUsages.size) cpuUsages[i] else -1
            val usageBar = if (usagePct >= 0) buildUsageBar(usagePct) else ""
            val value    = if (usagePct >= 0) "$freqStr  $usageBar $usagePct%" else freqStr
            items.add(InfoItem("CPU $i", value, usagePct > 70))
        }

        items += listOf(
            InfoItem("── GPU ──",          "", true),
            InfoItem("GPU Vendor",         gpuInfo["GPU Vendor"]        ?: "Unknown"),
            InfoItem("GPU Renderer",       gpuInfo["GPU Renderer"]      ?: "Unknown"),
            InfoItem("OpenGL ES Version",  gpuInfo["OpenGL ES Version"] ?: "Unknown"),
            InfoItem("Vulkan Support",     if (Build.VERSION.SDK_INT >= 24) "Yes (API ${Build.VERSION.SDK_INT})" else "No"),
            InfoItem("── Misc ──",         "", true),
            InfoItem("Scaling Governor",   DeviceUtils.getCpuGovernor()),
        )

        latestItems = items
        val query = b.searchBar.etSearch.text?.toString() ?: ""
        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter
        if (query.isNotBlank()) adapter?.filter(query)
    }

    /** Build a compact ASCII-style usage bar: ▓▓▓▓░░░░ */
    private fun buildUsageBar(pct: Int): String {
        val filled = (pct / 10).coerceIn(0, 10)
        return "▓".repeat(filled) + "░".repeat(10 - filled)
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("⚡ SOC / CPU Info")
        sb.appendLine("─────────────────")
        latestItems.forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() {
        running = false
        handler.removeCallbacks(refreshRunnable)
        super.onDestroyView()
        _b = null
    }

    companion object { private const val REFRESH_INTERVAL_MS = 2000L }
}
