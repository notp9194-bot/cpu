package com.example.deviceinfo.feature.soc

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.feature.soc.databinding.FragmentSocBinding

class SocFragment : Fragment(), ShareableFragment {
    private var _b: FragmentSocBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var latestMap: LinkedHashMap<String, String> = linkedMapOf()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_b != null) { loadData(); handler.postDelayed(this, REFRESH_MS) }
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSocBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()
        b.btnExportTxt.setOnClickListener  { ExportUtils.exportToFile(requireContext(), "SOC", latestMap) }
        b.btnExportJson.setOnClickListener { ExportUtils.exportToJson(requireContext(), "SOC", latestMap) }
    }

    override fun onResume() { super.onResume(); handler.postDelayed(refreshRunnable, REFRESH_MS) }
    override fun onPause()  { super.onPause();  handler.removeCallbacks(refreshRunnable) }

    private fun loadData() {
        if (_b == null) return
        val freqs   = DeviceUtils.getCpuFrequencies()
        val cores   = Runtime.getRuntime().availableProcessors()
        val maxFreq = DeviceUtils.getMaxCpuFreq(0).let { if (it > 0) "$it MHz" else "Unknown" }
        val minFreq = if (freqs.isNotEmpty()) "${freqs.minOrNull()} MHz" else "Unknown"
        val cpuInfo = DeviceUtils.getCpuInfo()
        val hw      = cpuInfo["Hardware"] ?: Build.HARDWARE
        val gpuInfo = DeviceUtils.getGpuInfo()

        latestMap = linkedMapOf(
            "SoC"              to hw,
            "Model"            to if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else hw,
            "Cores"            to cores.toString(),
            "Architecture"     to (Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"),
            "Processor"        to (cpuInfo["model name"] ?: cpuInfo["Processor"] ?: "Unknown"),
            "Clock Speed"      to "$minFreq - $maxFreq",
        )
        repeat(cores) { i -> latestMap["CPU $i"] = if (i < freqs.size) "${freqs[i]} MHz" else "Unknown" }
        latestMap["GPU Vendor"]        = gpuInfo["GPU Vendor"]        ?: "Unknown"
        latestMap["GPU Renderer"]      = gpuInfo["GPU Renderer"]      ?: "Unknown"
        latestMap["OpenGL ES Version"] = gpuInfo["OpenGL ES Version"] ?: "Unknown"
        latestMap["Scaling Governor"]  = DeviceUtils.getCpuGovernor()
        latestMap["Vulkan Support"]    = if (Build.VERSION.SDK_INT >= 24) "Yes (API ${Build.VERSION.SDK_INT})" else "No"

        latestItems = latestMap.map { (k, v) ->
            val highlight = k.startsWith("CPU ") || k == "SoC"
            InfoItem(k, v, highlight)
        }
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = InfoAdapter(latestItems)
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("⚡ SOC / CPU Info")
        sb.appendLine("─────────────────")
        latestMap.forEach { (k, v) -> sb.appendLine("$k: $v") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { handler.removeCallbacks(refreshRunnable); super.onDestroyView(); _b = null }
    companion object { private const val REFRESH_MS = 2000L }
}
