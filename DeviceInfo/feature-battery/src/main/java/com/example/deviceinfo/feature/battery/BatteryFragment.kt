package com.example.deviceinfo.feature.battery

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.feature.battery.databinding.FragmentBatteryBinding

class BatteryFragment : Fragment() {
    private var _b: FragmentBatteryBinding? = null
    private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentBatteryBinding.inflate(i, c, false).also { _b = it }.root
    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val items = DeviceUtils.getBatteryInfo(requireContext()).map { (k, v) -> InfoItem(k, v) }
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = BatteryAdapter(items)
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
