package com.example.deviceinfo.feature.display

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.feature.display.databinding.FragmentDisplayBinding
import kotlin.math.sqrt

class DisplayFragment : Fragment(), ShareableFragment {
    private var _b: FragmentDisplayBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentDisplayBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    private fun loadData() {
        if (_b == null) return
        val items = mutableListOf<InfoItem>()

        // ── Resolution & Size ─────────────────────────────────────────
        items.add(InfoItem("── Resolution & Size ──", "", true))

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

        items.add(InfoItem("Screen Resolution", "$widthPx × $heightPx px", true))
        items.add(InfoItem("Screen Size",       String.format("%.2f inches", inches)))
        items.add(InfoItem("Density",           "${dm.densityDpi} dpi"))
        items.add(InfoItem("Density Class",     densityClass(dm.densityDpi)))
        items.add(InfoItem("Logical Density",   "×${dm.density}"))
        items.add(InfoItem("Font Scale",        "×${dm.scaledDensity / dm.density}"))

        // ── Refresh Rate ──────────────────────────────────────────────
        items.add(InfoItem("── Refresh Rate ──", "", true))
        items.add(InfoItem("Current Rate",      "${refreshRate.toInt()} Hz", true))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val displayManager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            val supportedModes = display?.supportedModes
            if (!supportedModes.isNullOrEmpty()) {
                val rates = supportedModes.map { it.refreshRate.toInt() }.distinct().sorted()
                items.add(InfoItem("Supported Rates", rates.joinToString(", ") { "$it Hz" }))
                val maxRate = rates.maxOrNull() ?: 0
                items.add(InfoItem("Peak Refresh Rate", "$maxRate Hz"))
            }
        }

        // ── Color Capabilities ────────────────────────────────────────
        items.add(InfoItem("── Color ──", "", true))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayManager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                items.add(InfoItem("Wide Color Gamut",
                    if (display?.isWideColorGamut == true) "Yes (P3 / BT.2020)" else "No (sRGB)", true))
            }

            val hdrCaps = display?.hdrCapabilities
            val hdrTypes = hdrCaps?.supportedHdrTypes ?: intArrayOf()
            if (hdrTypes.isNotEmpty()) {
                val labels = hdrTypes.map { type ->
                    when (type) {
                        android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HDR10         -> "HDR10"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HLG           -> "HLG"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS    -> "HDR10+"
                        else -> "Type $type"
                    }
                }
                items.add(InfoItem("HDR Support",    labels.joinToString(", "), true))
                items.add(InfoItem("Max Luminance",  "${hdrCaps?.desiredMaxLuminance?.toInt()} nits"))
                items.add(InfoItem("Min Luminance",  "${hdrCaps?.desiredMinLuminance} nits"))
                items.add(InfoItem("Max Avg Lum",    "${hdrCaps?.desiredMaxAverageLuminance?.toInt()} nits"))
            } else {
                items.add(InfoItem("HDR Support", "Not Supported"))
            }
        }

        // ── Brightness ────────────────────────────────────────────────
        items.add(InfoItem("── Brightness ──", "", true))
        try {
            val brightness = android.provider.Settings.System.getInt(
                requireContext().contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )
            val pct = (brightness * 100 / 255)
            items.add(InfoItem("Current Brightness", "$pct% ($brightness/255)"))
            val autoBrightness = android.provider.Settings.System.getInt(
                requireContext().contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            items.add(InfoItem("Auto Brightness",
                if (autoBrightness == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    "Enabled" else "Disabled"))
        } catch (e: Exception) {
            items.add(InfoItem("Brightness", "Permission needed"))
        }

        // ── Touch ────────────────────────────────────────────────────
        items.add(InfoItem("── Touch ──", "", true))
        val hasTouchscreen = requireContext().packageManager
            .hasSystemFeature("android.hardware.touchscreen")
        val hasMultitouch = requireContext().packageManager
            .hasSystemFeature("android.hardware.touchscreen.multitouch.jazzhand")
        items.add(InfoItem("Touchscreen",  if (hasTouchscreen) "Yes" else "No"))
        items.add(InfoItem("Multi-touch",  if (hasMultitouch) "5+ fingers" else "Limited"))

        latestItems = items
        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter
    }

    private fun densityClass(dpi: Int) = when {
        dpi <= 120 -> "LDPI (Low)"
        dpi <= 160 -> "MDPI (Medium)"
        dpi <= 240 -> "HDPI (High)"
        dpi <= 320 -> "XHDPI (X-High)"
        dpi <= 480 -> "XXHDPI (XX-High)"
        else       -> "XXXHDPI (XXX-High)"
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🖥️ Display Info")
        sb.appendLine("─────────────────")
        latestItems.filter { it.value.isNotEmpty() }.forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
