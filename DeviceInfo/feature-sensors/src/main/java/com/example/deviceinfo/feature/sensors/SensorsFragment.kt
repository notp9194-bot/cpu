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

    // Live values map: sensor type → formatted string
    private val liveValues = mutableMapOf<Int, String>()

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

    private fun buildList() {
        if (_b == null) return
        val items = mutableListOf<InfoItem>()
        items.add(InfoItem("── Live Sensor Values ──", "", true))

        // Priority live sensors first
        val liveSensorTypes = listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_HEART_RATE
        )

        liveSensorTypes.forEach { type ->
            val sensor = sm.getDefaultSensor(type)
            if (sensor != null) {
                val liveVal = liveValues[type] ?: "Waiting…"
                items.add(InfoItem(sensor.name, liveVal, liveVal != "Waiting…"))
            }
        }

        items.add(InfoItem("── All Sensors (${sensorList.size}) ──", "", true))
        sensorList.forEach { sensor ->
            val info = buildString {
                append(typeLabel(sensor.type))
                append("  •  max: ${String.format("%.2f", sensor.maximumRange)} ${unitFor(sensor.type)}")
                append("  •  res: ${sensor.resolution}")
            }
            items.add(InfoItem(sensor.name, info, sensor.isWakeUpSensor))
        }

        adapter = InfoAdapter(items)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter
        val query = b.searchBar.etSearch.text?.toString() ?: ""
        if (query.isNotBlank()) adapter?.filter(query)
    }

    override fun onResume() {
        super.onResume()
        sensorList.forEach { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent?) {
        e ?: return
        val formatted = when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION ->
                "X:${fmt(e.values[0])}  Y:${fmt(e.values[1])}  Z:${fmt(e.values[2])} m/s²"
            Sensor.TYPE_GYROSCOPE ->
                "X:${fmt(e.values[0])}  Y:${fmt(e.values[1])}  Z:${fmt(e.values[2])} rad/s"
            Sensor.TYPE_MAGNETIC_FIELD ->
                "X:${fmt(e.values[0])}  Y:${fmt(e.values[1])}  Z:${fmt(e.values[2])} µT"
            Sensor.TYPE_ROTATION_VECTOR ->
                "X:${fmt(e.values[0])}  Y:${fmt(e.values[1])}  Z:${fmt(e.values[2])}"
            Sensor.TYPE_LIGHT ->
                "${fmt(e.values[0])} lux"
            Sensor.TYPE_PROXIMITY ->
                "${fmt(e.values[0])} cm"
            Sensor.TYPE_PRESSURE ->
                "${fmt(e.values[0])} hPa"
            Sensor.TYPE_AMBIENT_TEMPERATURE ->
                "${fmt(e.values[0])} °C"
            Sensor.TYPE_RELATIVE_HUMIDITY ->
                "${fmt(e.values[0])} %RH"
            Sensor.TYPE_STEP_COUNTER ->
                "${e.values[0].toLong()} steps"
            Sensor.TYPE_HEART_RATE ->
                "${fmt(e.values[0])} bpm"
            else -> e.values.take(3).joinToString("  ") { fmt(it) }
        }
        liveValues[e.sensor.type] = formatted
        // Throttle UI rebuild to avoid jank — rebuild only for priority sensors
        if (e.sensor.type in listOf(
                Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_LIGHT, Sensor.TYPE_PROXIMITY, Sensor.TYPE_PRESSURE,
                Sensor.TYPE_AMBIENT_TEMPERATURE, Sensor.TYPE_RELATIVE_HUMIDITY,
                Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION,
                Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_HEART_RATE
            )) {
            buildList()
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    private fun fmt(v: Float) = String.format("%.2f", v)

    private fun unitFor(type: Int) = when (type) {
        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY,
        Sensor.TYPE_LINEAR_ACCELERATION -> "m/s²"
        Sensor.TYPE_GYROSCOPE -> "rad/s"
        Sensor.TYPE_MAGNETIC_FIELD -> "µT"
        Sensor.TYPE_LIGHT -> "lux"
        Sensor.TYPE_PROXIMITY -> "cm"
        Sensor.TYPE_PRESSURE -> "hPa"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "°C"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
        else -> ""
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
        liveValues.forEach { (type, value) ->
            val sensor = sm.getDefaultSensor(type)
            if (sensor != null) sb.appendLine("${sensor.name}: $value")
        }
        sb.appendLine()
        sensorList.forEach { sb.appendLine("${it.name} — ${typeLabel(it.type)}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
