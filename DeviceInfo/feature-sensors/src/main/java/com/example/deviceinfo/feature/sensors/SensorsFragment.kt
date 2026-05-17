package com.example.deviceinfo.feature.sensors

import android.content.Context
import android.hardware.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.core.ui.InfoAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.feature.sensors.databinding.FragmentSensorsBinding

class SensorsFragment : Fragment(), SensorEventListener, ShareableFragment {
    private var _b: FragmentSensorsBinding? = null
    private val b get() = _b!!
    private lateinit var sm: SensorManager
    private var sensorList: List<Sensor> = emptyList()
    private var adapter: InfoAdapter? = null

    // Live values map: sensorName → formatted value string
    private val liveValues = mutableMapOf<String, String>()
    // Snapshot for share/export
    private var latestItems: List<InfoItem> = emptyList()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSensorsBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        sm = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorList = sm.getSensorList(Sensor.TYPE_ALL)
        buildList()

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        // Register only sensors we want live values for (save battery)
        val liveTypes = setOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_STEP_COUNTER
        )
        sensorList.forEach { sensor ->
            if (sensor.type in liveTypes) {
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    private fun buildList() {
        val items = mutableListOf<InfoItem>()
        // Header: total count
        items.add(InfoItem("Total Sensors", "${sensorList.size}", true))
        items.add(InfoItem("", "", false)) // spacer

        // Group by type for cleaner display
        sensorList.forEach { sensor ->
            val typeLabel = typeLabel(sensor.type)
            val liveVal = liveValues[sensor.name]
            val value = if (liveVal != null) "$typeLabel  •  $liveVal" else typeLabel
            items.add(InfoItem(sensor.name, value, liveVal != null))
        }
        latestItems = items
        val query = b.searchBar.etSearch.text?.toString() ?: ""
        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter
        if (query.isNotBlank()) adapter?.filter(query)
    }

    override fun onSensorChanged(e: SensorEvent?) {
        e ?: return
        if (_b == null) return
        val formatted = formatSensorValue(e.sensor.type, e.values)
        liveValues[e.sensor.name] = formatted
        // Throttle UI update: rebuild only if adapter exists
        adapter?.let { buildList() }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    private fun formatSensorValue(type: Int, values: FloatArray): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION ->
                "X:%.2f  Y:%.2f  Z:%.2f m/s²".format(values[0], values[1], values[2])

            Sensor.TYPE_GYROSCOPE ->
                "X:%.3f  Y:%.3f  Z:%.3f rad/s".format(values[0], values[1], values[2])

            Sensor.TYPE_MAGNETIC_FIELD ->
                "X:%.1f  Y:%.1f  Z:%.1f µT".format(values[0], values[1], values[2])

            Sensor.TYPE_LIGHT ->
                "%.1f lx".format(values[0])

            Sensor.TYPE_PROXIMITY ->
                if (values[0] < 5f) "Near (%.1f cm)".format(values[0])
                else "Far (%.1f cm)".format(values[0])

            Sensor.TYPE_PRESSURE ->
                "%.2f hPa".format(values[0])

            Sensor.TYPE_AMBIENT_TEMPERATURE ->
                "%.1f °C".format(values[0])

            Sensor.TYPE_RELATIVE_HUMIDITY ->
                "%.1f %%".format(values[0])

            Sensor.TYPE_STEP_COUNTER ->
                "${values[0].toLong()} steps"

            else ->
                values.take(3).joinToString("  ") { "%.3f".format(it) }
        }
    }

    private fun typeLabel(t: Int) = when (t) {
        Sensor.TYPE_ACCELEROMETER        -> "Accelerometer"
        Sensor.TYPE_GYROSCOPE            -> "Gyroscope"
        Sensor.TYPE_MAGNETIC_FIELD       -> "Magnetic Field"
        Sensor.TYPE_LIGHT                -> "Ambient Light"
        Sensor.TYPE_PROXIMITY            -> "Proximity"
        Sensor.TYPE_GRAVITY              -> "Gravity"
        Sensor.TYPE_LINEAR_ACCELERATION  -> "Linear Acceleration"
        Sensor.TYPE_ROTATION_VECTOR      -> "Rotation Vector"
        Sensor.TYPE_STEP_DETECTOR        -> "Step Detector"
        Sensor.TYPE_STEP_COUNTER         -> "Step Counter"
        Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game Rotation Vector"
        Sensor.TYPE_PRESSURE             -> "Barometer"
        Sensor.TYPE_TEMPERATURE          -> "Temperature"
        Sensor.TYPE_RELATIVE_HUMIDITY    -> "Relative Humidity"
        Sensor.TYPE_AMBIENT_TEMPERATURE  -> "Ambient Temperature"
        Sensor.TYPE_HEART_RATE           -> "Heart Rate"
        else                             -> "Type $t"
    }

    override fun getShareText(): String {
        val sb = StringBuilder()
        sb.appendLine("📡 Sensors (${sensorList.size} total)")
        sb.appendLine("─────────────────")
        sensorList.forEach { sensor ->
            val live = liveValues[sensor.name]
            val line = if (live != null) "${sensor.name} [${typeLabel(sensor.type)}]: $live"
                       else "${sensor.name} — ${typeLabel(sensor.type)}"
            sb.appendLine(line)
        }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
