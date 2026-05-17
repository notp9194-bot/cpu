package com.example.deviceinfo.feature.display

  import android.content.Context
  import android.graphics.Point
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
  import com.example.deviceinfo.core.util.ExportUtils
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
          b.btnExport.setOnClickListener {
              val dataMap = latestItems.filter { it.value.isNotEmpty() }
                  .associate { it.label to it.value }
              ExportUtils.exportToFile(requireContext(), "Display", dataMap)
          }
          b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
              override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
              override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
              override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
          })
      }

      private fun loadData() {
          val items = mutableListOf<InfoItem>()
          val ctx = requireContext()
          val wm = requireActivity().windowManager

          items.add(InfoItem("DISPLAY", "", true))

          val metrics = DisplayMetrics()
          @Suppress("DEPRECATION")
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              ctx.display?.getRealMetrics(metrics)
          } else {
              wm.defaultDisplay.getRealMetrics(metrics)
          }

          val widthPx  = metrics.widthPixels
          val heightPx = metrics.heightPixels
          val xdpi     = metrics.xdpi
          val ydpi     = metrics.ydpi
          val density  = metrics.density
          val densityDpi = metrics.densityDpi

          // Physical screen size
          val widthInch  = widthPx / xdpi
          val heightInch = heightPx / ydpi
          val diagInch   = sqrt((widthInch * widthInch + heightInch * heightInch).toDouble())

          items.add(InfoItem("Resolution", "${widthPx} × ${heightPx} px", true))
          items.add(InfoItem("Screen Size", "%.2f inches (diagonal)".format(diagInch)))
          items.add(InfoItem("Pixel Density", "$densityDpi dpi", true))
          items.add(InfoItem("X DPI", "%.1f".format(xdpi)))
          items.add(InfoItem("Y DPI", "%.1f".format(ydpi)))

          val densityBucket = when {
              densityDpi <= 120 -> "LDPI (Low)"
              densityDpi <= 160 -> "MDPI (Medium)"
              densityDpi <= 240 -> "HDPI (High)"
              densityDpi <= 320 -> "XHDPI (Extra-High)"
              densityDpi <= 480 -> "XXHDPI (Extra-Extra-High)"
              else -> "XXXHDPI (Extra-Extra-Extra-High)"
          }
          items.add(InfoItem("Density Class", densityBucket, true))
          items.add(InfoItem("Scale Factor", "%.2f×".format(density)))

          // Refresh rate
          val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              ctx.display?.refreshRate ?: 60f
          } else {
              @Suppress("DEPRECATION")
              wm.defaultDisplay.refreshRate
          }
          items.add(InfoItem("Refresh Rate", "%.1f Hz".format(refreshRate), true))

          // HDR support
          items.add(InfoItem("DISPLAY CAPABILITIES", "", true))
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
              val display = dm.getDisplay(android.view.Display.DEFAULT_DISPLAY)
              val hdrTypes = display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
              val hdrLabels = hdrTypes.map { type ->
                  when (type) {
                      android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                      android.view.Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                      android.view.Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                      android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                      else -> "Type $type"
                  }
              }
              items.add(InfoItem("HDR Support", if (hdrLabels.isEmpty()) "Not Supported" else hdrLabels.joinToString(", "), true))
          } else {
              items.add(InfoItem("HDR Support", "Not Available (API < 26)"))
          }

          // Wide color gamut
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
              val display = dm.getDisplay(android.view.Display.DEFAULT_DISPLAY)
              items.add(InfoItem("Wide Color Gamut", if (display.isWideColorGamut) "Supported" else "Not Supported"))
          }

          // Orientation
          val orientation = when (ctx.resources.configuration.orientation) {
              android.content.res.Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
              android.content.res.Configuration.ORIENTATION_PORTRAIT  -> "Portrait"
              else -> "Unknown"
          }
          items.add(InfoItem("Current Orientation", orientation))

          // Font scale
          val fontScale = ctx.resources.configuration.fontScale
          items.add(InfoItem("Font Scale", "%.2f×".format(fontScale)))

          latestItems = items
          adapter = InfoAdapter(items)
          b.recyclerView.layoutManager = LinearLayoutManager(ctx)
          b.recyclerView.adapter = adapter
      }

      override fun getShareText(): String {
          val sb = StringBuilder()
          sb.appendLine("\uD83D\uDCF1 Display Info")
          sb.appendLine("─────────────────")
          latestItems.filter { it.value.isNotEmpty() && !it.isHighlighted }
              .forEach { sb.appendLine("${it.label}: ${it.value}") }
          sb.appendLine("\nShared from CPU-A Device Info app")
          return sb.toString()
      }

      override fun onDestroyView() { super.onDestroyView(); _b = null }
  }