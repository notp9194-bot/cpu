package com.example.deviceinfo.feature.device

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.util.DisplayMetrics
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.feature.device.databinding.FragmentDeviceBinding
import kotlin.math.sqrt

class DeviceFragment : Fragment() {
    private var _b: FragmentDeviceBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentDeviceBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)

        // BUG FIX #4: Use WindowMetrics (API 30+) instead of deprecated defaultDisplay.getMetrics()
        val widthPx: Int
        val heightPx: Int
        val xdpi: Float
        val ydpi: Float
        val refreshRate: Float

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = wm.currentWindowMetrics
            widthPx  = metrics.bounds.width()
            heightPx = metrics.bounds.height()
            val dm = resources.displayMetrics
            xdpi = dm.xdpi
            ydpi = dm.ydpi
            refreshRate = requireActivity().display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            val m = DisplayMetrics().also { requireActivity().windowManager.defaultDisplay.getMetrics(it) }
            widthPx  = m.widthPixels
            heightPx = m.heightPixels
            xdpi = m.xdpi
            ydpi = m.ydpi
            @Suppress("DEPRECATION")
            refreshRate = requireActivity().windowManager.defaultDisplay.refreshRate
        }

        val dm = resources.displayMetrics
        val inches = sqrt(
            (widthPx / xdpi).toDouble().let { it * it } +
            (heightPx / ydpi).toDouble().let { it * it }
        )

        val am = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalRam = mi.totalMem / (1024 * 1024)
        val availRam = mi.availMem / (1024 * 1024)
        val sf = StatFs(Environment.getDataDirectory().path)
        val totalSt = sf.totalBytes / (1024 * 1024 * 1024)
        val availSt = sf.availableBytes / (1024 * 1024 * 1024)

        val items = listOf(
            InfoItem("Model",             "${Build.MODEL} (${Build.DEVICE})"),
            InfoItem("Brand",             Build.BRAND),
            InfoItem("Manufacturer",      Build.MANUFACTURER),
            InfoItem("Board",             Build.BOARD),
            InfoItem("Hardware",          Build.HARDWARE),
            InfoItem("Screen Size",       String.format("%.2f inches", inches)),
            InfoItem("Screen Resolution", "$widthPx x $heightPx px"),
            InfoItem("Screen Density",    "${dm.densityDpi} dpi"),
            InfoItem("Refresh Rate",      "${refreshRate.toInt()} Hz"),
            InfoItem("Total RAM",         "$totalRam MB"),
            InfoItem("Available RAM",     "$availRam MB (${availRam * 100 / totalRam}%)", true),
            InfoItem("Internal Storage",  "$totalSt GB"),
            InfoItem("Available Storage", "$availSt GB (${availSt * 100 / (if(totalSt > 0) totalSt else 1)}%)", true),
            InfoItem("Supported ABIs",    Build.SUPPORTED_ABIS.joinToString(", ")),
        )
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = InfoAdapter(items)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
