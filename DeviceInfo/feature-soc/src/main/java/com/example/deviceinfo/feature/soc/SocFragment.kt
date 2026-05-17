package com.example.deviceinfo.feature.soc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.feature.soc.databinding.FragmentSocBinding
import android.os.Build

class SocFragment : Fragment() {

    private var _binding: FragmentSocBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSocBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val items = buildSocInfo()
        val adapter = InfoAdapter(items)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun buildSocInfo(): List<InfoItem> {
        val items = mutableListOf<InfoItem>()
        val cpuInfo = DeviceUtils.getCpuInfo()
        val numCores = Runtime.getRuntime().availableProcessors()
        val freqs = DeviceUtils.getCpuFrequencies()
        val governor = DeviceUtils.getCpuGovernor()

        val hardware = cpuInfo["Hardware"] ?: Build.HARDWARE
        val processor = cpuInfo["model name"] ?: cpuInfo["Processor"] ?: Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"

        items.add(InfoItem("SoC", hardware, true))
        items.add(InfoItem("Model", Build.SOC_MODEL.ifBlank { hardware }))
        items.add(InfoItem("Cores", numCores.toString()))
        items.add(InfoItem("Architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"))
        items.add(InfoItem("Processor", processor))

        val minFreq = if (freqs.isNotEmpty()) "${freqs.minOrNull()} MHz" else "Unknown"
        val maxFreq = DeviceUtils.getMaxCpuFreq(0).let { if (it > 0) "$it MHz" else "Unknown" }
        items.add(InfoItem("Clock Speed", "$minFreq - $maxFreq"))

        for (i in 0 until numCores) {
            val curFreq = if (i < freqs.size) "${freqs[i]} MHz" else "Unknown"
            items.add(InfoItem("CPU $i", curFreq, true))
        }

        items.add(InfoItem("GPU Vendor", "Qualcomm"))
        items.add(InfoItem("GPU Renderer", Build.HARDWARE))
        items.add(InfoItem("Scaling Governor", governor))

        return items
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
