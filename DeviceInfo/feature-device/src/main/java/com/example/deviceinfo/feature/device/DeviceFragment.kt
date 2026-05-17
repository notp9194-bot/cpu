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
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.core.util.DeviceUtils
import com.example.deviceinfo.feature.device.databinding.FragmentDeviceBinding
import kotlin.math.sqrt

class DeviceFragment : Fragment(), ShareableFragment {
    private var _b: FragmentDeviceBinding? = null
    private val b get() = _b!!
    private var latestData: LinkedHashMap<String, String> = linkedMapOf()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentDeviceBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)

        val widthPx: Int; val heightPx: Int
        val xdpi: Float; val ydpi: Float; val refreshRate: Float

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = wm.currentWindowMetrics
            widthPx  = metrics.bounds.width()
            heightPx = metrics.bounds.height()
            val dm = resources.displayMetrics
            xdpi = dm.xdpi; ydpi = dm.ydpi
            refreshRate = requireActivity().display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            val m = DisplayMetrics().also { requireActivity().windowManager.defaultDisplay.getMetrics(it) }
            widthPx = m.widthPixels; heightPx = m.heightPixels
            xdpi = m.xdpi; ydpi = m.ydpi
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

        // NEW: HDR + Wide Color Gamut
        val displayCaps = DeviceUtils.getDisplayCapabilities(requireContext())

        latestData = linkedMapOf(
            "Model"             to "${Build.MODEL} (${Build.DEVICE})",
            "Brand"             to Build.BRAND,
            "Manufacturer"      to Build.MANUFACTURER,
            "Board"             to Build.BOARD,
            "Hardware"          to Build.HARDWARE,
            "Screen Size"       to String.format("%.2f inches", inches),
            "Screen Resolution" to "$widthPx x $heightPx px",
            "Screen Density"    to "${dm.densityDpi} dpi",
            "Refresh Rate"      to "${refreshRate.toInt()} Hz",
            "Total RAM"         to "$totalRam MB",
            "Available RAM"     to "$availRam MB (${availRam * 100 / totalRam}%)",
            "Internal Storage"  to "$totalSt GB",
            "Available Storage" to "$availSt GB (${availSt * 100 / (if (totalSt > 0) totalSt else 1)}%)",
            "Supported ABIs"    to Build.SUPPORTED_ABIS.joinToString(", ")
        ).also { it.putAll(displayCaps) }

        val items = latestData.entries.mapIndexed { idx, (k, v) ->
            val highlight = k.contains("Available") || k.contains("HDR") || k.contains("Wide Color")
            InfoItem(k, v, highlight)
        }

        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = InfoAdapter(items)
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
