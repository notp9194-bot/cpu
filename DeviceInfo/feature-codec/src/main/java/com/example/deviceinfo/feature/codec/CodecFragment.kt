package com.example.deviceinfo.feature.codec

  import android.content.Context
  import android.media.MediaCodecList
  import android.media.MediaCodecInfo
  import android.opengl.EGL14
  import android.opengl.EGLConfig
  import android.opengl.GLES20
  import android.os.Build
  import android.os.Bundle
  import android.text.Editable
  import android.text.TextWatcher
  import android.view.*
  import androidx.fragment.app.Fragment
  import androidx.recyclerview.widget.LinearLayoutManager
  import com.example.deviceinfo.core.model.InfoItem
  import com.example.deviceinfo.core.ui.InfoAdapter
  import com.example.deviceinfo.core.ui.ShareableFragment
  import com.example.deviceinfo.core.util.ExportUtils
  import com.example.deviceinfo.feature.codec.databinding.FragmentCodecBinding

  class CodecFragment : Fragment(), ShareableFragment {
      private var _b: FragmentCodecBinding? = null
      private val b get() = _b!!
      private var latestItems: List<InfoItem> = emptyList()
      private var adapter: InfoAdapter? = null

      override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
          FragmentCodecBinding.inflate(i, c, false).also { _b = it }.root

      override fun onViewCreated(view: View, s: Bundle?) {
          super.onViewCreated(view, s)
          loadData()
          b.btnExport.setOnClickListener {
              val dataMap = latestItems.filter { it.value.isNotEmpty() }.associate { it.label to it.value }
              ExportUtils.exportToFile(requireContext(), "Codec_GPU", dataMap)
          }
          b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
              override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
              override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
              override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
          })
      }

      private fun loadData() {
          val items = mutableListOf<InfoItem>()

          // ── GPU INFO ─────────────────────────────────────────────
          items.add(InfoItem("GPU / GRAPHICS", "", true))
          val gpu = getGpuInfo()
          items.add(InfoItem("GPU Vendor",   gpu["vendor"]   ?: "Unknown", true))
          items.add(InfoItem("GPU Renderer", gpu["renderer"] ?: "Unknown"))
          items.add(InfoItem("OpenGL ES",    gpu["version"]  ?: "Unknown", true))

          // Vulkan
          val hasVulkan = requireContext().packageManager
              .hasSystemFeature("android.hardware.vulkan.level")
          val vulkanVer = when {
              requireContext().packageManager.hasSystemFeature("android.hardware.vulkan.version") ->
                  if (Build.VERSION.SDK_INT >= 28) "Vulkan 1.1+" else "Vulkan 1.0"
              hasVulkan -> "Supported"
              else -> "Not Supported"
          }
          items.add(InfoItem("Vulkan", vulkanVer, true))

          // ── VIDEO CODECS ─────────────────────────────────────────
          items.add(InfoItem("VIDEO CODECS", "", true))
          val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
          val allCodecs = codecList.codecInfos

          data class CodecResult(val hwDec: Boolean, val hwEnc: Boolean, val swDec: Boolean, val swEnc: Boolean)

          fun findCodec(vararg mimeTypes: String): CodecResult {
              var hwDec=false; var hwEnc=false; var swDec=false; var swEnc=false
              for (codec in allCodecs) {
                  val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                      codec.isHardwareAccelerated else !codec.name.startsWith("OMX.google")
                  val supported = codec.supportedTypes.any { t -> mimeTypes.any { m -> t.equals(m, true) } }
                  if (!supported) continue
                  if (codec.isEncoder) { if (isHw) hwEnc=true else swEnc=true }
                  else { if (isHw) hwDec=true else swDec=true }
              }
              return CodecResult(hwDec, hwEnc, swDec, swEnc)
          }

          fun support(r: CodecResult): String {
              val parts = mutableListOf<String>()
              if (r.hwDec) parts.add("HW Decode")
              if (r.hwEnc) parts.add("HW Encode")
              if (r.swDec) parts.add("SW Decode")
              if (r.swEnc) parts.add("SW Encode")
              return if (parts.isEmpty()) "Not Supported" else parts.joinToString(", ")
          }

          val videoCodecs = listOf(
              "H.264 (AVC)"  to findCodec("video/avc"),
              "H.265 (HEVC)" to findCodec("video/hevc"),
              "VP8"          to findCodec("video/x-vnd.on2.vp8"),
              "VP9"          to findCodec("video/x-vnd.on2.vp9"),
              "AV1"          to findCodec("video/av01"),
              "MPEG-4"       to findCodec("video/mp4v-es"),
              "H.263"        to findCodec("video/3gpp"),
          )
          videoCodecs.forEach { (name, r) ->
              items.add(InfoItem(name, support(r), r.hwDec || r.hwEnc))
          }

          // ── AUDIO CODECS ─────────────────────────────────────────
          items.add(InfoItem("AUDIO CODECS", "", true))
          val audioCodecs = listOf(
              "AAC"       to findCodec("audio/mp4a-latm"),
              "MP3"       to findCodec("audio/mpeg"),
              "FLAC"      to findCodec("audio/flac"),
              "Opus"      to findCodec("audio/opus"),
              "Vorbis"    to findCodec("audio/vorbis"),
              "AAC-ELD"   to findCodec("audio/mp4a-latm"),
              "AMRWB"     to findCodec("audio/amr-wb"),
          )
          audioCodecs.forEach { (name, r) ->
              items.add(InfoItem(name, support(r), r.hwDec))
          }

          // ── CODEC COUNT ───────────────────────────────────────────
          items.add(InfoItem("CODEC SUMMARY", "", true))
          val hwCount = allCodecs.count { c ->
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) c.isHardwareAccelerated
              else !c.name.startsWith("OMX.google")
          }
          items.add(InfoItem("Total Codecs", "${allCodecs.size}", true))
          items.add(InfoItem("Hardware Codecs", "$hwCount"))
          items.add(InfoItem("Software Codecs", "${allCodecs.size - hwCount}"))

          latestItems = items
          adapter = InfoAdapter(items, "CODEC")
          b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
          b.recyclerView.adapter = adapter
      }

      private fun getGpuInfo(): Map<String, String> {
          return try {
              val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
              val major = IntArray(1); val minor = IntArray(1)
              EGL14.eglInitialize(display, major, 0, minor, 0)
              val attribs = intArrayOf(EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE)
              val configs = arrayOfNulls<EGLConfig>(1); val numConfigs = IntArray(1)
              EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfigs, 0)
              val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
              val ctx = EGL14.eglCreateContext(display, configs[0]!!, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
              val surfAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
              val surf = EGL14.eglCreatePbufferSurface(display, configs[0]!!, surfAttribs, 0)
              EGL14.eglMakeCurrent(display, surf, surf, ctx)
              val vendor   = GLES20.glGetString(GLES20.GL_VENDOR)   ?: "Unknown"
              val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
              val version  = GLES20.glGetString(GLES20.GL_VERSION)  ?: "Unknown"
              EGL14.eglDestroyContext(display, ctx)
              EGL14.eglDestroySurface(display, surf)
              EGL14.eglTerminate(display)
              mapOf("vendor" to vendor, "renderer" to renderer, "version" to version)
          } catch (e: Exception) {
              mapOf("vendor" to "Unknown", "renderer" to "Unknown", "version" to "Unknown")
          }
      }

      override fun getShareText(): String {
          val sb = StringBuilder()
          sb.appendLine("\uD83C\uDFAE GPU & Codec Info")
          sb.appendLine("─────────────────")
          latestItems.filter { it.value.isNotEmpty() && !it.isHighlighted }
              .forEach { sb.appendLine("${it.label}: ${it.value}") }
          sb.appendLine("\nShared from CPU-A Device Info app")
          return sb.toString()
      }

      override fun onDestroyView() { super.onDestroyView(); _b = null }
  }