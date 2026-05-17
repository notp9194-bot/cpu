package com.example.deviceinfo.feature.thermal

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.feature.thermal.databinding.FragmentThermalBinding

class ThermalFragment : Fragment() {
    private var _b: FragmentThermalBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentThermalBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val thermals = DeviceUtils.getThermalInfo()
        val items = if (thermals.isNotEmpty())
            thermals.map { (n, t) -> InfoItem(n, String.format("%.1f °C", t)) }
        else
            listOf(InfoItem("Thermal", "Not available on this device"))
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = InfoAdapter(items)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
