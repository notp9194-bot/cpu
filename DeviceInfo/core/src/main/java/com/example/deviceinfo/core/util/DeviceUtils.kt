package com.example.deviceinfo.core.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object DeviceUtils {

    fun getCpuInfo(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        try {
            BufferedReader(FileReader("/proc/cpuinfo")).use { reader ->
                reader.lineSequence().forEach { line ->
                    if (line.contains(":")) {
                        val parts = line.split(":", limit = 2)
                        if (parts.size == 2) info[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        } catch (e: IOException) { /* ignore */ }
        return info
    }

    fun getCpuFrequencies(): List<Long> {
        val freqs = mutableListOf<Long>()
        var cpu = 0
        while (true) {
            try {
                BufferedReader(FileReader("/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_cur_freq")).use {
                    val freq = it.readLine()?.toLongOrNull() ?: return freqs
                    freqs.add(freq / 1000)
                }
                cpu++
            } catch (e: Exception) { return freqs }
        }
    }

    fun getMaxCpuFreq(cpuIndex: Int): Long = try {
        BufferedReader(FileReader("/sys/devices/system/cpu/cpu$cpuIndex/cpufreq/cpuinfo_max_freq")).use {
            (it.readLine()?.toLongOrNull() ?: 0L) / 1000
        }
    } catch (e: Exception) { 0L }

    fun getCpuGovernor(): String = try {
        BufferedReader(FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")).use {
            it.readLine() ?: "unknown"
        }
    } catch (e: Exception) { "unknown" }

    fun getThermalInfo(): List<Pair<String, Float>> {
        val thermals = mutableListOf<Pair<String, Float>>()
        for (zone in 0 until 30) {
            val base = "/sys/class/thermal/thermal_zone$zone"
            try {
                val type = BufferedReader(FileReader("$base/type")).use { it.readLine() } ?: break
                val tempRaw = BufferedReader(FileReader("$base/temp")).use { it.readLine()?.toLongOrNull() ?: 0L }
                thermals.add(type to if (tempRaw > 1000) tempRaw / 1000f else tempRaw.toFloat())
            } catch (e: Exception) { }
        }
        return thermals
    }

    fun getBatteryInfo(context: Context): Map<String, String> {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val status = when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            else -> "Unknown"
        }
        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            else -> "Unknown"
        }
        val plugged = when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        return linkedMapOf(
            "Health" to health, "Level" to "$pct %", "Power Source" to plugged,
            "Status" to status, "Technology" to tech,
            "Temperature" to "$temp °C", "Voltage" to "$voltage mV"
        )
    }
}
