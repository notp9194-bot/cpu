package com.example.deviceinfo.feature.device

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.util.DisplayMetrics
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.feature.device.databinding.FragmentDeviceBinding
import kotlin.math.sqrt

class DeviceFragment : Fragment() {
    private var _b: FragmentDeviceBinding? = null
    private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentDeviceBinding.inflate(i, c, false).also { _b = it }.root
    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val m = DisplayMetrics().also { requireActivity().windowManager.defaultDisplay.getMetrics(it) }
        val am = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalRam = mi.totalMem / (1024 * 1024)
        val availRam = mi.availMem / (1024 * 1024)
        val sf = StatFs(Environment.getDataDirectory().path)
        val totalSt = sf.totalBytes / (1024 * 1024 * 1024)
        val availSt = sf.availableBytes / (1024 * 1024 * 1024)
        val inches = sqrt((m.widthPixels / m.xdpi).toDouble().let { it * it } + (m.heightPixels / m.ydpi).toDouble().let { it * it })
        val items = listOf(
            InfoItem("Model", "${Build.MODEL} (${Build.DEVICE})"),
            InfoItem("Manufacturer", Build.MANUFACTURER),
            InfoItem("Board", Build.BOARD),
            InfoItem("Hardware", Build.HARDWARE),
            InfoItem("Screen Size", String.format("%.2f inches", inches)),
            InfoItem("Screen Resolution", "${m.widthPixels} x ${m.heightPixels} pixels"),
            InfoItem("Screen Density", "${m.densityDpi} dpi"),
            InfoItem("Total RAM", "$totalRam MB"),
            InfoItem("Available RAM", "$availRam MB (${availRam * 100 / totalRam}%)", true),
            InfoItem("Internal Storage", "$totalSt GB"),
            InfoItem("Available Storage", "$availSt GB (${availSt * 100 / (if(totalSt>0) totalSt else 1)}%)", true)
        )
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = DeviceAdapter(items)
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
