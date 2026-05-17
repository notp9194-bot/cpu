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
import com.example.deviceinfo.feature.camera.databinding.FragmentCameraBinding

class CameraFragment : Fragment() {
    private var _b: FragmentCameraBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentCameraBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val items = mutableListOf<InfoItem>()
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

                // Megapixels
                val configs = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val jpegSizes = configs?.getOutputSizes(ImageFormat.JPEG)
                val largest = jpegSizes?.maxByOrNull { it.width.toLong() * it.height }
                if (largest != null) {
                    val mp = (largest.width.toLong() * largest.height) / 1000000.0
                    items.add(InfoItem("Resolution", "${largest.width} x ${largest.height} (%.1f MP)".format(mp)))
                }

                // Aperture — FloatArray? null-safe
                val apertures: FloatArray? = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                if (apertures != null && apertures.isNotEmpty()) {
                    items.add(InfoItem("Aperture", apertures.joinToString(", ") { "f/%.1f".format(it) }))
                }

                // Focal length — FloatArray? null-safe
                val focals: FloatArray? = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                if (focals != null && focals.isNotEmpty()) {
                    items.add(InfoItem("Focal Length", focals.joinToString(", ") { "%.1f mm".format(it) }))
                }

                // Sensor size
                val sensorSize: SizeF? = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                if (sensorSize != null) {
                    items.add(InfoItem("Sensor Size", "%.2f x %.2f mm".format(sensorSize.width, sensorSize.height)))
                }

                // OIS
                val oisModes: IntArray? = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                val hasOis = oisModes?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
                items.add(InfoItem("OIS", if (hasOis) "Yes" else "No"))

                // AF
                val afModes: IntArray? = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                val hasAF = afModes != null && afModes.size > 1
                items.add(InfoItem("Auto Focus", if (hasAF) "Yes" else "Fixed Focus"))

                // Flash
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                items.add(InfoItem("Flash", if (hasFlash) "Yes" else "No"))

                // RAW support (API 21+)
                val caps: IntArray? = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val hasRaw = caps?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
                items.add(InfoItem("RAW Capture", if (hasRaw) "Supported" else "Not Supported"))
            }
        } catch (e: Exception) {
            items.add(InfoItem("Error", "Could not read camera info"))
        }
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = InfoAdapter(items)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
