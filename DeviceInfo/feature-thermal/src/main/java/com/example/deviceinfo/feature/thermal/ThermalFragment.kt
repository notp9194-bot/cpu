package com.example.deviceinfo.feature.thermal

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.feature.thermal.databinding.FragmentThermalBinding

class ThermalFragment : Fragment(), ShareableFragment {
    private var _b: FragmentThermalBinding? = null
    private val b get() = _b!!
    private var latestData: List<Pair<String, Float>> = emptyList()
    private var adapter: ThermalAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentThermalBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        latestData = DeviceUtils.getThermalInfo()
        val thermalItems = if (latestData.isNotEmpty())
            latestData.map { (n, t) -> ThermalItem(n, t) }
        else listOf(ThermalItem("Thermal", 0f))

        adapter = ThermalAdapter(thermalItems)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter

        b.btnExport.setOnClickListener {
            val map = latestData.associate { (k, v) -> k to String.format("%.1f °C", v) }
            ExportUtils.exportToFile(requireContext(), "Thermal", map)
        }

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🌡️ Thermal Info")
        sb.appendLine("─────────────────")
        latestData.forEach { (name, temp) -> sb.appendLine("$name: ${"%.1f".format(temp)} °C") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
