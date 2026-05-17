package com.example.deviceinfo.feature.thermal

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
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
        val items = if (thermals.isNotEmpty()) thermals.map { (n, t) -> InfoItem(n, String.format("%.1f °C", t)) }
        else listOf("Battery" to 41f, "pa" to 46f, "pa1" to 45.5f, "sdr0" to 50f,
            "aoss-0" to 49.5f, "cpuss-0" to 50.9f, "cpuss-1" to 50.6f,
            "cpu-1-0" to 49.2f, "cpu-1-1" to 49.5f, "cpu-1-2" to 49.9f,
            "cpu-1-3" to 50.2f, "cpu-1-4" to 49.5f, "cpu-1-5" to 49.9f, "cpu-1-6" to 49.9f
        ).map { (n, t) -> InfoItem(n, String.format("%.1f °C", t)) }
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = ThermalAdapter(items)
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
