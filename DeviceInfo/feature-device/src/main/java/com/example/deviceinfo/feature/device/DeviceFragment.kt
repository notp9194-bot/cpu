package com.example.deviceinfo.feature.device

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.feature.device.databinding.FragmentDeviceBinding

class DeviceFragment : Fragment(), ShareableFragment {
    private var _b: FragmentDeviceBinding? = null
    private val b get() = _b!!
    private var latestData: LinkedHashMap<String, String> = linkedMapOf()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentDeviceBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)

        val am = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalRam = mi.totalMem / (1024 * 1024)
        val availRam = mi.availMem / (1024 * 1024)
        val sf = StatFs(Environment.getDataDirectory().path)
        val totalSt = sf.totalBytes / (1024 * 1024 * 1024)
        val availSt = sf.availableBytes / (1024 * 1024 * 1024)

        val items = mutableListOf<InfoItem>()

        // ── Identity ──────────────────────────────────────────────────
        items.add(InfoItem("── Identity ──", "", true))
        items.add(InfoItem("Model",        "${Build.MODEL} (${Build.DEVICE})", true))
        items.add(InfoItem("Brand",        Build.BRAND))
        items.add(InfoItem("Manufacturer", Build.MANUFACTURER))
        items.add(InfoItem("Board",        Build.BOARD))
        items.add(InfoItem("Hardware",     Build.HARDWARE))

        // ── Memory ───────────────────────────────────────────────────
        items.add(InfoItem("── Memory ──", "", true))
        items.add(InfoItem("Total RAM",         "$totalRam MB", true))
        items.add(InfoItem("Available RAM",     "$availRam MB (${availRam * 100 / totalRam.coerceAtLeast(1)}%)", true))
        items.add(InfoItem("Low Memory",        if (mi.lowMemory) "Yes ⚠️" else "No"))

        // ── Storage ──────────────────────────────────────────────────
        items.add(InfoItem("── Storage ──", "", true))
        items.add(InfoItem("Internal Storage",  "$totalSt GB"))
        items.add(InfoItem("Available Storage", "$availSt GB (${availSt * 100 / totalSt.coerceAtLeast(1)}%)", true))

        // Check for SD card
        val extDirs = requireContext().getExternalFilesDirs(null)
        if (extDirs.size > 1 && extDirs[1] != null) {
            val sdSf = StatFs(extDirs[1].absolutePath)
            val sdTotal = sdSf.totalBytes / (1024 * 1024 * 1024)
            val sdAvail = sdSf.availableBytes / (1024 * 1024 * 1024)
            items.add(InfoItem("SD Card Total",   "$sdTotal GB"))
            items.add(InfoItem("SD Card Free",    "$sdAvail GB"))
        } else {
            items.add(InfoItem("SD Card", "Not present"))
        }

        // ── CPU ABIs ─────────────────────────────────────────────────
        items.add(InfoItem("── Supported ABIs ──", "", true))
        Build.SUPPORTED_ABIS.forEachIndexed { i, abi ->
            items.add(InfoItem("ABI ${i + 1}", abi))
        }

        latestData = linkedMapOf<String, String>().also { map ->
            items.filter { it.value.isNotEmpty() }.forEach { map[it.label] = it.value }
        }

        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("📱 Device Info")
        sb.appendLine("─────────────────")
        latestData.forEach { (k, v) -> sb.appendLine("$k: $v") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
