package com.example.deviceinfo.feature.sensors

import android.content.Context
import android.hardware.*
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.feature.sensors.databinding.FragmentSensorsBinding

class SensorsFragment : Fragment(), SensorEventListener {
    private var _b: FragmentSensorsBinding? = null
    private val b get() = _b!!
    private lateinit var sm: SensorManager
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSensorsBinding.inflate(i, c, false).also { _b = it }.root
    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        sm = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        val items = sensors.map { InfoItem(it.name, typeLabel(it.type)) }
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerView.adapter = SensorsAdapter(items)
        sensors.forEach { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    private fun typeLabel(t: Int) = when(t) {
        Sensor.TYPE_ACCELEROMETER -> "Accelerometer  Non-wakeup"
        Sensor.TYPE_GYROSCOPE -> "Gyroscope  Non-wakeup"
        Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field  Non-wakeup"
        Sensor.TYPE_LIGHT -> "Ambient Light Sensor  Non-wakeup"
        Sensor.TYPE_PROXIMITY -> "Proximity Sensor  Wakeup"
        Sensor.TYPE_GRAVITY -> "Gravity  Non-wakeup"
        Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration  Non-wakeup"
        Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector  Non-wakeup"
        Sensor.TYPE_STEP_DETECTOR -> "Step Detector  Non-wakeup"
        Sensor.TYPE_STEP_COUNTER -> "Step Counter  Non-wakeup"
        Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game Rotation Vector  Non-wakeup"
        else -> "Non-wakeup"
    }
    override fun onSensorChanged(e: SensorEvent?) {}
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    override fun onPause() { super.onPause(); sm.unregisterListener(this) }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
