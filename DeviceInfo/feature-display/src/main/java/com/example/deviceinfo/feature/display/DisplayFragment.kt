package com.example.deviceinfo.feature.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
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
        val items = buildDisplayInfo()
        latestItems = items
        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    private fun buildDisplayInfo(): List<InfoItem> {
        val items = mutableListOf<InfoItem>()
        val ctx = requireContext()

        // ── Screen basics ────────────────────────────────────
        items.add(InfoItem("── Screen ──", "", true))

        val widthPx: Int; val heightPx: Int
        val xdpi: Float; val ydpi: Float; val refreshRate: Float

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = wm.currentWindowMetrics
            widthPx = metrics.bounds.width(); heightPx = metrics.bounds.height()
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
        val aspectRatio = simplifyRatio(widthPx, heightPx)

        items.add(InfoItem("Resolution",      "$widthPx × $heightPx px"))
        items.add(InfoItem("Screen Size",     String.format("%.2f inches", inches)))
        items.add(InfoItem("Aspect Ratio",    aspectRatio))
        items.add(InfoItem("Density",         "${dm.densityDpi} dpi  (${densityBucket(dm.densityDpi)})"))
        items.add(InfoItem("X DPI",           String.format("%.1f", xdpi)))
        items.add(InfoItem("Y DPI",           String.format("%.1f", ydpi)))
        items.add(InfoItem("Density Scale",   String.format("%.2f×", dm.density)))

        // ── Refresh rates ─────────────────────────────────────
        items.add(InfoItem("── Refresh Rate ──", "", true))
        items.add(InfoItem("Current Rate", "${refreshRate.toInt()} Hz", true))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = requireActivity().display
            if (display != null) {
                try {
                    val modes = display.supportedModes
                    if (modes.size > 1) {
                        modes.forEachIndexed { i, mode ->
                            items.add(InfoItem(
                                "Mode ${i + 1}",
                                "${mode.physicalWidth}×${mode.physicalHeight}  @  ${String.format("%.0f", mode.refreshRate)} Hz"
                            ))
                        }
                    } else {
                        items.add(InfoItem("Supported Modes", "Single mode only"))
                    }
                } catch (e: Exception) { }
            }
        }

        // ── Color / HDR ──────────────────────────────────────
        items.add(InfoItem("── Color & HDR ──", "", true))
        val dm2 = ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display0 = dm2.getDisplay(android.view.Display.DEFAULT_DISPLAY)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && display0 != null) {
            items.add(InfoItem("Wide Color Gamut",
                if (display0.isWideColorGamut) "✅ Yes (P3 / BT.2020)" else "❌ No (sRGB)", display0.isWideColorGamut))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && display0 != null) {
            val hdrCaps = display0.hdrCapabilities
            if (hdrCaps != null && hdrCaps.supportedHdrTypes.isNotEmpty()) {
                val labels = hdrCaps.supportedHdrTypes.map { type ->
                    when (type) {
                        android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HDR10         -> "HDR10"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HLG           -> "HLG"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS    -> "HDR10+"
                        else -> "Type $type"
                    }
                }
                items.add(InfoItem("HDR Support",      labels.joinToString(", "), true))
                items.add(InfoItem("Max Luminance",    "${hdrCaps.desiredMaxLuminance.toInt()} nits"))
                items.add(InfoItem("Min Luminance",    "${hdrCaps.desiredMinLuminance} nits"))
                items.add(InfoItem("Max Avg Luminance","${hdrCaps.desiredMaxAverageLuminance.toInt()} nits"))
            } else {
                items.add(InfoItem("HDR Support", "Not Supported"))
            }
        }

        // Color mode (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && display0 != null) {
            try {
                val colorMode = when (display0.colorMode) {
                    android.view.Display.COLOR_MODE_DEFAULT  -> "Default"
                    android.view.Display.COLOR_MODE_SRGB     -> "sRGB"
                    android.view.Display.COLOR_MODE_DISPLAY_P3 -> "Display P3"
                    else -> "Mode ${display0.colorMode}"
                }
                items.add(InfoItem("Color Mode", colorMode))

                val supportedModes = display0.supportedColorModes
                if (supportedModes.isNotEmpty()) {
                    items.add(InfoItem("Supported Color Modes",
                        supportedModes.joinToString(", ") { m ->
                            when (m) {
                                android.view.Display.COLOR_MODE_DEFAULT    -> "Default"
                                android.view.Display.COLOR_MODE_SRGB       -> "sRGB"
                                android.view.Display.COLOR_MODE_DISPLAY_P3 -> "P3"
                                else -> "Mode $m"
                            }
                        }
                    ))
                }
            } catch (e: Exception) { }
        }

        // ── Orientation & rotation ────────────────────────────
        items.add(InfoItem("── Orientation ──", "", true))
        val rotation = when (requireActivity().windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0   -> "Portrait (0°)"
            Surface.ROTATION_90  -> "Landscape (90°)"
            Surface.ROTATION_180 -> "Reverse Portrait (180°)"
            Surface.ROTATION_270 -> "Reverse Landscape (270°)"
            else -> "Unknown"
        }
        items.add(InfoItem("Current Rotation", rotation))
        items.add(InfoItem("Natural Orientation",
            if (widthPx < heightPx) "Portrait" else "Landscape"))

        // ── Cutout ───────────────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = requireActivity().window.decorView.rootWindowInsets?.displayCutout
            items.add(InfoItem("── Display Cutout ──", "", true))
            if (cutout != null) {
                items.add(InfoItem("Cutout Present", "Yes", true))
                items.add(InfoItem("Safe Area Top",    "${cutout.safeInsetTop} px"))
                items.add(InfoItem("Safe Area Bottom", "${cutout.safeInsetBottom} px"))
                items.add(InfoItem("Safe Area Left",   "${cutout.safeInsetLeft} px"))
                items.add(InfoItem("Safe Area Right",  "${cutout.safeInsetRight} px"))
            } else {
                items.add(InfoItem("Cutout", "No cutout / Fullscreen"))
            }
        }

        return items
    }

    private fun densityBucket(dpi: Int) = when {
        dpi <= 120  -> "ldpi"
        dpi <= 160  -> "mdpi"
        dpi <= 240  -> "hdpi"
        dpi <= 320  -> "xhdpi"
        dpi <= 480  -> "xxhdpi"
        dpi <= 640  -> "xxxhdpi"
        else        -> "Ultra HD"
    }

    private fun simplifyRatio(w: Int, h: Int): String {
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val g = gcd(w, h)
        return "${w / g}:${h / g}"
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🖥️ Display Info")
        sb.appendLine("─────────────────")
        latestItems.filter { it.label.isNotEmpty() && it.value.isNotEmpty() }
            .forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
