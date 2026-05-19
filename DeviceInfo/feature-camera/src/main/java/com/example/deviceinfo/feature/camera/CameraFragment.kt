package com.example.deviceinfo.feature.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
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
import com.example.deviceinfo.feature.camera.databinding.FragmentCameraBinding

class CameraFragment : Fragment(), ShareableFragment {
    private var _b: FragmentCameraBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentCameraBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val items = mutableListOf<InfoItem>()
        val chartBars = mutableListOf<CameraMpBarChartView.CameraBar>()

        val barColors = listOf(
            0xFF1565C0.toInt(), 0xFF00897B.toInt(), 0xFF6A1B9A.toInt(), 0xFFE53935.toInt()
        )
        try {
            val cm = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val ids = cm.cameraIdList
            items.add(InfoItem("Total Cameras", ids.size.toString(), true))
            ids.forEachIndexed { index, id ->
                val chars = cm.getCameraCharacteristics(id)
                val facing = when (chars.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT    -> "Front"
                    CameraCharacteristics.LENS_FACING_BACK     -> "Back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Unknown"
                }
                items.add(InfoItem("Camera $index", "[$facing]", true))
                val configs = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val largest = configs?.getOutputSizes(ImageFormat.JPEG)
                    ?.maxByOrNull { it.width.toLong() * it.height }
                if (largest != null) {
                    val mp = (largest.width.toLong() * largest.height) / 1_000_000.0
                    items.add(InfoItem("Resolution",
                        "${largest.width} x ${largest.height} (${"%.1f".format(mp)} MP)"))
                    chartBars.add(
                        CameraMpBarChartView.CameraBar(
                            label = "Cam$index\n$facing",
                            mp    = mp,
                            color = barColors[index % barColors.size]
                        )
                    )
                }
                chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { items.add(InfoItem("Aperture",
                        it.joinToString(", ") { a -> "f/${"%.1f".format(a)}" })) }
                chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { items.add(InfoItem("Focal Length",
                        it.joinToString(", ") { f -> "${"%.1f".format(f)} mm" })) }
                chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    ?.let { items.add(InfoItem("Sensor Size",
                        "${"%.2f".format(it.width)} x ${"%.2f".format(it.height)} mm")) }
                val oisOn = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                    ?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
                items.add(InfoItem("OIS", if (oisOn) "Yes" else "No"))
                val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                items.add(InfoItem("Auto Focus",
                    if (afModes != null && afModes.size > 1) "Yes" else "Fixed Focus"))
                items.add(InfoItem("Flash",
                    if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) "Yes" else "No"))
                val hasRaw = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
                items.add(InfoItem("RAW Capture", if (hasRaw) "Supported" else "Not Supported"))
            }
        } catch (e: Exception) {
            items.add(InfoItem("Error", "Could not read camera info"))
        }

        latestItems = items
        adapter = InfoAdapter(items, "CAMERA")
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter

        // Feed megapixel bar chart
        if (chartBars.isNotEmpty()) b.cameraMpChart.update(chartBars)

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("\uD83D\uDCF8 Camera Info")
        sb.appendLine("─────────────────")
        latestItems.forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
