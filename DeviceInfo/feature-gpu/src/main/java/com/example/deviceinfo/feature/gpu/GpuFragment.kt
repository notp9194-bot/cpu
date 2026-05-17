package com.example.deviceinfo.feature.gpu

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.opengl.GLES30
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
import com.example.deviceinfo.feature.gpu.databinding.FragmentGpuBinding

class GpuFragment : Fragment(), ShareableFragment {

    private var _b: FragmentGpuBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentGpuBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        loadData()
        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun loadData() {
        val ctx = requireContext()
        val pm  = ctx.packageManager

        // ── OpenGL ES version via EGL ─────────────────────────────────
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val majorV = IntArray(1); val minorV = IntArray(1)
        EGL14.eglInitialize(eglDisplay, majorV, 0, minorV, 0)

        val attribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, numConfigs, 0)

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val eglCtx = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        val pbuf = EGL14.eglCreatePbufferSurface(eglDisplay,
            configs[0], intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
        EGL14.eglMakeCurrent(eglDisplay, pbuf, pbuf, eglCtx)

        val renderer   = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
        val vendor     = GLES20.glGetString(GLES20.GL_VENDOR)   ?: "Unknown"
        val glVerStr   = GLES20.glGetString(GLES20.GL_VERSION)  ?: "Unknown"
        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: ""
        val extList    = extensions.split(" ").filter { it.isNotBlank() }
        val extCount   = extList.size

        val maxTexArr = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTexArr, 0)
        val maxTexSize = maxTexArr[0]

        val maxCubeArr = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_CUBE_MAP_TEXTURE_SIZE, maxCubeArr, 0)

        val maxVpArr = IntArray(2)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, maxVpArr, 0)

        val maxAttrArr = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, maxAttrArr, 0)

        val maxVaryArr = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_VECTORS, maxVaryArr, 0)

        val maxUniVArr = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_UNIFORM_VECTORS, maxUniVArr, 0)

        val maxUniFArr = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS, maxUniFArr, 0)

        // Parse GL ES version float (e.g. "OpenGL ES 3.2 ...")
        val glVersionFloat = Regex("""OpenGL ES (\d+\.\d+)""")
            .find(glVerStr)?.groupValues?.get(1)?.toFloatOrNull() ?: 2.0f

        // GL ES 3.0 specific
        var maxColorAtt = 0
        var maxDrawBuf  = 0
        var maxUboSize  = 0
        if (glVersionFloat >= 3.0f) {
            val a1 = IntArray(1); GLES30.glGetIntegerv(GLES30.GL_MAX_COLOR_ATTACHMENTS, a1, 0);   maxColorAtt = a1[0]
            val a2 = IntArray(1); GLES30.glGetIntegerv(GLES30.GL_MAX_DRAW_BUFFERS, a2, 0);         maxDrawBuf  = a2[0]
            val a3 = IntArray(1); GLES30.glGetIntegerv(GLES30.GL_MAX_UNIFORM_BLOCK_SIZE, a3, 0);   maxUboSize  = a3[0]
        }

        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroyContext(eglDisplay, eglCtx)
        EGL14.eglDestroySurface(eglDisplay, pbuf)

        // ── Vulkan detection ─────────────────────────────────────────
        val hasVulkan   = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            pm.hasSystemFeature("android.hardware.vulkan.version") else false
        val hasVulkan11 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            pm.hasSystemFeature("android.hardware.vulkan.level", 1) else false
        val hasVulkan12 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            pm.hasSystemFeature("android.software.vulkan.deqp.level") else false

        // ── Update charts ─────────────────────────────────────────────
        b.gpuDoughnut.update(glVersionFloat, maxTexSize, extCount, renderer)

        val capBars = mutableListOf<GpuBarChartView.GpuBar>()
        capBars.add(GpuBarChartView.GpuBar("OpenGL ES Version", glVersionFloat, 4.0f))
        capBars.add(GpuBarChartView.GpuBar("Max Texture (px)", maxTexSize.toFloat(), 16384f, "px"))
        capBars.add(GpuBarChartView.GpuBar("Extensions", extCount.toFloat(), 200f, ""))
        capBars.add(GpuBarChartView.GpuBar("Vertex Attribs", maxAttrArr[0].toFloat(), 32f, ""))
        b.gpuBarChart.update(capBars)

        // ── Build info list ───────────────────────────────────────────
        val items = mutableListOf<InfoItem>()

        items.add(InfoItem("GPU", "", true))
        items.add(InfoItem("Renderer", renderer, true))
        items.add(InfoItem("Vendor", vendor))
        items.add(InfoItem("GL Version", glVerStr, true))

        items.add(InfoItem("OPENGL ES LIMITS", "", true))
        items.add(InfoItem("OpenGL ES Version", "%.1f".format(glVersionFloat), true))
        items.add(InfoItem("Max Texture Size", "${maxTexSize}×${maxTexSize} px"))
        items.add(InfoItem("Max Cubemap Size", "${maxCubeArr[0]} px", true))
        items.add(InfoItem("Max Viewport", "${maxVpArr[0]}×${maxVpArr[1]} px"))
        items.add(InfoItem("Vertex Attribs", maxAttrArr[0].toString(), true))
        items.add(InfoItem("Varying Vectors", maxVaryArr[0].toString()))
        items.add(InfoItem("Vertex Uniforms", maxUniVArr[0].toString()))
        items.add(InfoItem("Fragment Uniforms", maxUniFArr[0].toString()))

        if (glVersionFloat >= 3.0f) {
            items.add(InfoItem("OPENGL ES 3.0+ FEATURES", "", true))
            items.add(InfoItem("Color Attachments", maxColorAtt.toString(), true))
            items.add(InfoItem("Draw Buffers", maxDrawBuf.toString()))
            items.add(InfoItem("UBO Max Size", if (maxUboSize > 0) "${maxUboSize / 1024} KB" else "N/A", true))
        }

        items.add(InfoItem("VULKAN", "", true))
        items.add(InfoItem("Vulkan", if (hasVulkan) "Supported ✅" else "Not Available", true))
        if (hasVulkan) {
            items.add(InfoItem("Vulkan 1.1", if (hasVulkan11) "Supported" else "Not Detected"))
            items.add(InfoItem("Vulkan 1.2", if (hasVulkan12) "Likely" else "Check device specs"))
        }

        items.add(InfoItem("EXTENSIONS ($extCount)", "", true))
        // Show notable extensions
        val notable = listOf(
            "EXT_texture_compression_s3tc" to "S3TC Texture Compression",
            "OES_texture_float"             to "Float Texture",
            "EXT_texture_filter_anisotropic" to "Anisotropic Filtering",
            "OES_depth_texture"             to "Depth Texture",
            "EXT_color_buffer_float"        to "Float Color Buffer",
            "OES_vertex_array_object"       to "Vertex Array Object",
            "EXT_disjoint_timer_query"      to "GPU Timer Query",
            "OES_texture_compression_astc"  to "ASTC Compression",
            "KHR_texture_compression_astc_ldr" to "ASTC LDR",
        )
        notable.forEach { (ext, friendlyName) ->
            val supported = extList.any { it.contains(ext, ignoreCase = true) }
            items.add(InfoItem(friendlyName, if (supported) "✅ Supported" else "Not Available"))
        }
        items.add(InfoItem("Total Extensions", extCount.toString(), true))

        latestItems = items
        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(ctx)
        b.recyclerView.adapter = adapter
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🎮 GPU Info")
        sb.appendLine("─────────────────")
        latestItems.filter { it.value.isNotEmpty() && !it.isHighlighted }
            .forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestItems.filter { it.value.isNotEmpty() }.associate { it.label to it.value }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
