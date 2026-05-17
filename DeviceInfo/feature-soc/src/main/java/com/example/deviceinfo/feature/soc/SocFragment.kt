package com.example.deviceinfo.feature.soc

import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.feature.soc.databinding.FragmentSocBinding

class SocFragment : Fragment() {
    private var _b: FragmentSocBinding? = null
    private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSocBinding.inflate(i, c, false).also { _b = it }.root
    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val freqs = DeviceUtils.getCpuFrequencies()
        val cores = Runtime.getRuntime().availableProcessors()
        val maxFreq = DeviceUtils.getMaxCpuFreq(0).let { if (it > 0) "$it MHz" else "Unknown" }
        val minFreq = if (freqs.isNotEmpty()) "${freqs.minOrNull()} MHz" else "Unknown"
        val cpuInfo = DeviceUtils.getCpuInfo()
        val hw = cpuInfo["Hardware"] ?: Build.HARDWARE
        val items = mutableListOf(
            InfoItem("SoC", hw, true),
            InfoItem("Model", if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else hw),
            InfoItem("Cores", cores.toString()),
            InfoItem("Architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"),
            InfoItem("Processor", cpuInfo["model name"] ?: cpuInfo["Processor"] ?: "Unknown"),
            InfoItem("Clock Speed", "$minFreq - $maxFreq"),
        )
        repeat(cores) { i -> items.add(InfoItem("CPU $i", if (i < freqs.size) "${freqs[i]} MHz" else "Unknown", true)) }
        items += listOf(
            InfoItem("GPU Vendor", "Qualcomm"),
            InfoItem("GPU Renderer", hw),
            InfoItem("Scaling Governor", DeviceUtils.getCpuGovernor())
        )
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = SocAdapter(items)
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
