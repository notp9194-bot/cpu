package com.example.deviceinfo.feature.thermal

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.feature.thermal.databinding.FragmentThermalBinding

class ThermalFragment : Fragment() {
    private var _b: FragmentThermalBinding? = null
    private val b get() = _b!!
    private var latestData: List<Pair<String, Float>> = emptyList()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentThermalBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        latestData = DeviceUtils.getThermalInfo()
        val thermalItems = if (latestData.isNotEmpty())
            latestData.map { (n, t) -> ThermalItem(n, t) }
        else
            listOf(ThermalItem("Thermal", 0f))   // handled in adapter label

        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = ThermalAdapter(thermalItems)

        b.btnExport.setOnClickListener {
            val map = latestData.associate { (k, v) -> k to String.format("%.1f °C", v) }
            ExportUtils.exportToFile(requireContext(), "Thermal", map)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
