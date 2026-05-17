package com.example.deviceinfo.feature.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.InputDevice
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.feature.input.databinding.FragmentInputBinding

class InputFragment : Fragment(), ShareableFragment {

    private var _b: FragmentInputBinding? = null
    private val b get() = _b!!
    private var latestItems: List<InfoItem> = emptyList()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentInputBinding.inflate(i, c, false).also { _b = it }.root

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

        // ── Touchscreen ───────────────────────────────────────────────
        val hasTouch       = pm.hasSystemFeature("android.hardware.touchscreen")
        val hasMultiTouch  = pm.hasSystemFeature("android.hardware.touchscreen.multitouch")
        val hasDistinct    = pm.hasSystemFeature("android.hardware.touchscreen.multitouch.distinct")
        val hasJazzHands   = pm.hasSystemFeature("android.hardware.touchscreen.multitouch.jazzhand") // 5+ points
        val hasFakeTouch   = pm.hasSystemFeature("android.hardware.faketouch")

        // Max touch points from InputManager
        val inputMgr = ctx.getSystemService(Context.INPUT_SERVICE) as InputManager
        var maxTouchPoints = 0
        var hasStylus      = false
        var hasMouse       = false
        var hasGamepad     = false
        var hasKeyboard    = false
        var hasTrackball   = false
        var hasExternalKb  = false

        inputMgr.inputDeviceIds.forEach { id ->
            val dev = inputMgr.getInputDevice(id) ?: return@forEach
            if (!dev.isVirtual) {
                val src = dev.sources
                if (src and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN) {
                    val motionRange = dev.getMotionRange(MotionEvent.AXIS_PRESSURE, InputDevice.SOURCE_TOUCHSCREEN)
                    // estimate max points from tool type
                    val pts = dev.getMotionRange(MotionEvent.AXIS_X, InputDevice.SOURCE_TOUCHSCREEN)?.let { 5 } ?: 0
                    if (pts > maxTouchPoints) maxTouchPoints = pts
                }
                if (src and InputDevice.SOURCE_STYLUS == InputDevice.SOURCE_STYLUS) hasStylus = true
                if (src and InputDevice.SOURCE_MOUSE  == InputDevice.SOURCE_MOUSE)  hasMouse  = true
                if (src and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) hasGamepad = true
                if (src and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD) {
                    if (dev.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
                        if (dev.isExternal) hasExternalKb = true else hasKeyboard = true
                    }
                }
                if (src and InputDevice.SOURCE_TRACKBALL == InputDevice.SOURCE_TRACKBALL) hasTrackball = true
            }
        }

        // Jazz hands = 5+ touch points — that means at least 5
        val touchPointsStr = when {
            hasJazzHands -> "5+ (multitouch)"
            hasDistinct  -> "2–4 (distinct)"
            hasMultiTouch-> "2+ (basic)"
            hasTouch     -> "1 (single touch)"
            else         -> "Not Available"
        }

        // ── Biometrics ────────────────────────────────────────────────
        val hasFingerprint = pm.hasSystemFeature("android.hardware.fingerprint")
        val hasFaceUnlock  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            pm.hasSystemFeature("android.hardware.biometrics.face") else false
        val hasIris        = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            pm.hasSystemFeature("android.hardware.biometrics.iris") else false

        // ── Sensors (input-relevant) ──────────────────────────────────
        val hasGyro        = pm.hasSystemFeature("android.hardware.sensor.gyroscope")
        val hasAccel       = pm.hasSystemFeature("android.hardware.sensor.accelerometer")
        val hasMagnet      = pm.hasSystemFeature("android.hardware.sensor.compass")
        val hasProx        = pm.hasSystemFeature("android.hardware.sensor.proximity")
        val hasLight       = pm.hasSystemFeature("android.hardware.sensor.light")
        val hasBaro        = pm.hasSystemFeature("android.hardware.sensor.barometer")
        val hasStep        = pm.hasSystemFeature("android.hardware.sensor.stepcounter")
        val hasHeart       = pm.hasSystemFeature("android.hardware.sensor.heartrate")

        // ── Microphone / Camera ───────────────────────────────────────
        val hasMic         = pm.hasSystemFeature("android.hardware.microphone")
        val hasFrontCam    = pm.hasSystemFeature("android.hardware.camera.front")
        val hasCamera      = pm.hasSystemFeature("android.hardware.camera.any")

        // ── Radar chart ───────────────────────────────────────────────
        val radarAxes = listOf(
            InputRadarChartView.InputAxis("Touch",   if (hasTouch) 1f else 0f),
            InputRadarChartView.InputAxis("Multi",   if (hasJazzHands) 1f else if (hasDistinct) 0.6f else if (hasMultiTouch) 0.3f else 0f),
            InputRadarChartView.InputAxis("FP",      if (hasFingerprint) 1f else 0f),
            InputRadarChartView.InputAxis("Face",    if (hasFaceUnlock) 1f else 0f),
            InputRadarChartView.InputAxis("Gyro",    if (hasGyro) 1f else 0f),
            InputRadarChartView.InputAxis("Stylus",  if (hasStylus) 1f else 0f),
            InputRadarChartView.InputAxis("Gamepad", if (hasGamepad) 1f else 0f),
            InputRadarChartView.InputAxis("Mic",     if (hasMic) 1f else 0f),
        )
        b.inputRadarChart.update(radarAxes)

        // ── Build list ────────────────────────────────────────────────
        val items = mutableListOf<InfoItem>()

        items.add(InfoItem("TOUCHSCREEN", "", true))
        items.add(InfoItem("Touchscreen",    if (hasTouch) "Available" else "Not Available", true))
        items.add(InfoItem("Touch Points",   touchPointsStr))
        items.add(InfoItem("Multitouch",     if (hasMultiTouch) "Yes" else "No", true))
        items.add(InfoItem("5+ Point Touch", if (hasJazzHands) "Yes ✅" else "No"))
        items.add(InfoItem("Stylus / Pen",   if (hasStylus) "Connected ✅" else "Not Detected", true))

        items.add(InfoItem("BIOMETRICS", "", true))
        items.add(InfoItem("Fingerprint",    if (hasFingerprint) "Supported ✅" else "Not Available", true))
        items.add(InfoItem("Face Unlock",    if (hasFaceUnlock) "Supported ✅" else "Not Available"))
        items.add(InfoItem("Iris Scanner",   if (hasIris) "Supported ✅" else "Not Available", true))

        items.add(InfoItem("CONNECTED DEVICES", "", true))
        items.add(InfoItem("External Keyboard", if (hasExternalKb) "Connected ✅" else "None"))
        items.add(InfoItem("Built-in Keyboard", when {
            hasKeyboard  -> "Physical ✅"
            else         -> "Software only"
        }, true))
        items.add(InfoItem("Mouse",          if (hasMouse) "Connected ✅" else "None"))
        items.add(InfoItem("Gamepad",        if (hasGamepad) "Connected ✅" else "None", true))
        items.add(InfoItem("Trackball",      if (hasTrackball) "Yes" else "No"))

        items.add(InfoItem("MOTION SENSORS", "", true))
        items.add(InfoItem("Gyroscope",      if (hasGyro) "Present ✅" else "Not Available", true))
        items.add(InfoItem("Accelerometer",  if (hasAccel) "Present ✅" else "Not Available"))
        items.add(InfoItem("Magnetometer",   if (hasMagnet) "Present ✅" else "Not Available", true))
        items.add(InfoItem("Proximity",      if (hasProx) "Present ✅" else "Not Available"))
        items.add(InfoItem("Ambient Light",  if (hasLight) "Present ✅" else "Not Available", true))
        items.add(InfoItem("Barometer",      if (hasBaro) "Present ✅" else "Not Available"))
        items.add(InfoItem("Step Counter",   if (hasStep) "Present ✅" else "Not Available", true))
        items.add(InfoItem("Heart Rate",     if (hasHeart) "Present ✅" else "Not Available"))

        items.add(InfoItem("MEDIA INPUT", "", true))
        items.add(InfoItem("Microphone",     if (hasMic) "Present ✅" else "Not Available", true))
        items.add(InfoItem("Front Camera",   if (hasFrontCam) "Present ✅" else "Not Available"))
        items.add(InfoItem("Camera",         if (hasCamera) "Present ✅" else "Not Available", true))

        latestItems = items
        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(ctx)
        b.recyclerView.adapter = adapter
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("🎮 Input & Biometrics Info")
        sb.appendLine("─────────────────")
        latestItems.filter { it.value.isNotEmpty() && !(it.isHighlighted && it.value.isEmpty()) }
            .forEach { sb.appendLine("${it.label}: ${it.value}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun getExportData(): Map<String, String> =
        latestItems.filter { it.value.isNotEmpty() }.associate { it.label to it.value }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
