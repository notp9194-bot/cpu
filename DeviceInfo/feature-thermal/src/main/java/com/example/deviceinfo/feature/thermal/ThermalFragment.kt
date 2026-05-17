package com.example.deviceinfo.feature.thermal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.feature.thermal.databinding.FragmentThermalBinding

class ThermalFragment : Fragment() {

    private var _binding: FragmentThermalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThermalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val thermals = DeviceUtils.getThermalInfo()
        val items = if (thermals.isNotEmpty()) {
            thermals.map { (name, temp) -> InfoItem(name, String.format("%.1f °C", temp)) }
        } else {
            listOf(
                InfoItem("Battery", "41.0 °C"),
                InfoItem("pa", "46.0 °C"),
                InfoItem("pa1", "45.5 °C"),
                InfoItem("sdr0", "50.0 °C"),
                InfoItem("aoss-0", "49.5 °C"),
                InfoItem("cpuss-0", "50.9 °C"),
                InfoItem("cpuss-1", "50.6 °C"),
                InfoItem("cpu-1-0", "49.2 °C"),
                InfoItem("cpu-1-1", "49.5 °C"),
                InfoItem("cpu-1-2", "49.9 °C"),
                InfoItem("cpu-1-3", "50.2 °C"),
                InfoItem("cpu-1-4", "49.5 °C"),
                InfoItem("cpu-1-5", "49.9 °C"),
                InfoItem("cpu-1-6", "49.9 °C")
            )
        }
        val adapter = ThermalAdapter(items)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
