package com.cpua.deviceinfo.feature.sensors

import android.content.Context
import android.hardware.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cpua.deviceinfo.core.model.InfoItem
import com.cpua.deviceinfo.core.ui.InfoAdapter
import com.cpua.deviceinfo.core.ui.ShareableFragment
import com.cpua.deviceinfo.feature.sensors.databinding.FragmentSensorsBinding

class SensorsFragment : Fragment(), SensorEventListener, ShareableFragment {
    private var _b: FragmentSensorsBinding? = null
    private val b get() = _b!!
    private lateinit var sm: SensorManager
    private var sensorList: List<Sensor> = emptyList()
    private var adapter: InfoAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSensorsBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        sm = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorList = sm.getSensorList(Sensor.TYPE_ALL)

        val items = sensorList.map { InfoItem(it.name, typeLabel(it.type), it.isWakeUpSensor) }
        adapter = InfoAdapter(items, "SENSORS")
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = adapter

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, cnt: Int) {}
        })

        // Build sensor category bar chart
        updateSensorBarChart()
    }

    private fun updateSensorBarChart() {
        if (_b == null) return
        // Group sensors into categories
        val motionSensors = listOf(
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_STEP_DETECTOR, Sensor.TYPE_STEP_COUNTER
        )
        val positionSensors = listOf(
            Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_PROXIMITY
        )
        val envSensors = listOf(
            Sensor.TYPE_LIGHT, Sensor.TYPE_PRESSURE,
            Sensor.TYPE_TEMPERATURE, Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY
        )
        val bioSensors = listOf(Sensor.TYPE_HEART_RATE)

        val countMotion   = sensorList.count { it.type in motionSensors }
        val countPosition = sensorList.count { it.type in positionSensors }
        val countEnv      = sensorList.count { it.type in envSensors }
        val countBio      = sensorList.count { it.type in bioSensors }
        val countOther    = sensorList.size - countMotion - countPosition - countEnv - countBio

        val cats = mutableListOf<SensorBarChartView.SensorCategory>()
        if (countMotion   > 0) cats.add(SensorBarChartView.SensorCategory("Motion",   countMotion,   0xFF1E88E5.toInt()))
        if (countPosition > 0) cats.add(SensorBarChartView.SensorCategory("Position", countPosition, 0xFF00897B.toInt()))
        if (countEnv      > 0) cats.add(SensorBarChartView.SensorCategory("Environ",  countEnv,      0xFF43A047.toInt()))
        if (countBio      > 0) cats.add(SensorBarChartView.SensorCategory("Bio",      countBio,      0xFFE53935.toInt()))
        if (countOther    > 0) cats.add(SensorBarChartView.SensorCategory("Other",    countOther,    0xFF8E24AA.toInt()))

        b.sensorBarChart.update(cats)
    }

    override fun onResume() {
        super.onResume()
        sensorList.forEach { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    override fun onPause() { super.onPause(); sm.unregisterListener(this) }

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
        sb.appendLine("\uD83D\uDCE1 Sensors (${sensorList.size} total)")
        sb.appendLine("─────────────────")
        sensorList.forEach { sb.appendLine("${it.name} — ${typeLabel(it.type)}") }
        sb.appendLine("\nShared from CPU-A Device Info app")
        return sb.toString()
    }

    override fun onSensorChanged(e: SensorEvent?) {}
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
