package com.example.deviceinfo.feature.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.feature.camera.databinding.FragmentCameraBinding

class CameraFragment : Fragment(), ShareableFragment {
    private var _b: FragmentCameraBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var latestMap: LinkedHashMap<String, String> = linkedMapOf()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentCameraBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val items = mutableListOf<InfoItem>()
        try {
            val cm = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val ids = cm.cameraIdList
            items.add(InfoItem("Total Cameras", ids.size.toString(), true))
            latestMap["Total Cameras"] = ids.size.toString()

            ids.forEachIndexed { index, id ->
                val chars = cm.getCameraCharacteristics(id)
                val facing = when (chars.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT    -> "Front"
                    CameraCharacteristics.LENS_FACING_BACK     -> "Back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Unknown"
                }
                items.add(InfoItem("Camera $index", "[$facing]", true))
                latestMap["Camera $index"] = "[$facing]"

                val configs = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val largest = configs?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height }
                if (largest != null) {
                    val mp = (largest.width.toLong() * largest.height) / 1000000.0
                    val v = "${largest.width} x ${largest.height} (%.1f MP)".format(mp)
                    items.add(InfoItem("Resolution", v)); latestMap["Camera $index Resolution"] = v
                }

                val apertures: FloatArray? = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                if (apertures != null && apertures.isNotEmpty()) {
                    val v = apertures.joinToString(", ") { "f/%.1f".format(it) }
                    items.add(InfoItem("Aperture", v)); latestMap["Camera $index Aperture"] = v
                }

                val focals: FloatArray? = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                if (focals != null && focals.isNotEmpty()) {
                    val v = focals.joinToString(", ") { "%.1f mm".format(it) }
                    items.add(InfoItem("Focal Length", v)); latestMap["Camera $index Focal Length"] = v
                }

                val sensorSize: SizeF? = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                if (sensorSize != null) {
                    val v = "%.2f x %.2f mm".format(sensorSize.width, sensorSize.height)
                    items.add(InfoItem("Sensor Size", v)); latestMap["Camera $index Sensor Size"] = v
                }

                val oisModes: IntArray? = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                val hasOis = oisModes?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
                items.add(InfoItem("OIS", if (hasOis) "Yes" else "No"))
                latestMap["Camera $index OIS"] = if (hasOis) "Yes" else "No"

                val afModes: IntArray? = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                val afVal = if (afModes != null && afModes.size > 1) "Yes" else "Fixed Focus"
                items.add(InfoItem("Auto Focus", afVal)); latestMap["Camera $index Auto Focus"] = afVal

                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                items.add(InfoItem("Flash", if (hasFlash) "Yes" else "No"))
                latestMap["Camera $index Flash"] = if (hasFlash) "Yes" else "No"

                val caps: IntArray? = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val rawVal = if (caps?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true) "Supported" else "Not Supported"
                items.add(InfoItem("RAW Capture", rawVal)); latestMap["Camera $index RAW"] = rawVal
            }
        } catch (e: Exception) {
            items.add(InfoItem("Error", "Could not read camera info"))
        }

        latestItems = items
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = InfoAdapter(items)

        b.btnExportTxt.setOnClickListener  { ExportUtils.exportToFile(requireContext(), "Camera", latestMap) }
        b.btnExportJson.setOnClickListener { ExportUtils.exportToJson(requireContext(), "Camera", latestMap) }
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("📸 Camera Info")
        sb.appendLine("─────────────────")
        latestItems.forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
