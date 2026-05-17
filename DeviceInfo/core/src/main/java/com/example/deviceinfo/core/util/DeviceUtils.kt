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
            val reader = BufferedReader(FileReader("/proc/cpuinfo"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains(":")) {
                    val parts = line!!.split(":", limit = 2)
                    if (parts.size == 2) {
                        info[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return info
    }

    fun getCpuFrequencies(): List<Long> {
        val freqs = mutableListOf<Long>()
        var cpu = 0
        while (true) {
            val path = "/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_cur_freq"
            try {
                val reader = BufferedReader(FileReader(path))
                val freq = reader.readLine()?.toLongOrNull() ?: break
                freqs.add(freq / 1000) // Convert to MHz
                reader.close()
                cpu++
            } catch (e: Exception) {
                break
            }
        }
        return freqs
    }

    fun getMaxCpuFreq(cpuIndex: Int): Long {
        return try {
            val reader = BufferedReader(FileReader("/sys/devices/system/cpu/cpu$cpuIndex/cpufreq/cpuinfo_max_freq"))
            val freq = reader.readLine()?.toLongOrNull() ?: 0L
            reader.close()
            freq / 1000
        } catch (e: Exception) { 0L }
    }

    fun getCpuGovernor(): String {
        return try {
            val reader = BufferedReader(FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"))
            val gov = reader.readLine() ?: "unknown"
            reader.close()
            gov
        } catch (e: Exception) { "unknown" }
    }

    fun getMemInfo(): Map<String, Long> {
        val info = mutableMapOf<String, Long>()
        try {
            val reader = BufferedReader(FileReader("/proc/meminfo"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split("\\s+".toRegex())
                if (parts.size >= 2) {
                    val key = parts[0].trimEnd(':')
                    val value = parts[1].toLongOrNull() ?: continue
                    info[key] = value
                }
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return info
    }

    fun getThermalInfo(): List<Pair<String, Float>> {
        val thermals = mutableListOf<Pair<String, Float>>()
        var zone = 0
        while (zone < 30) {
            val basePath = "/sys/class/thermal/thermal_zone$zone"
            try {
                val typeReader = BufferedReader(FileReader("$basePath/type"))
                val type = typeReader.readLine() ?: break
                typeReader.close()

                val tempReader = BufferedReader(FileReader("$basePath/temp"))
                val tempRaw = tempReader.readLine()?.toLongOrNull() ?: 0L
                tempReader.close()

                val temp = if (tempRaw > 1000) tempRaw / 1000f else tempRaw.toFloat()
                thermals.add(Pair(type, temp))
            } catch (e: Exception) {
                // Skip this zone
            }
            zone++
        }
        return thermals
    }

    fun getBatteryInfo(context: Context): Map<String, String> {
        val info = mutableMapOf<String, String>()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) level * 100 / scale else 0

        val status = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val health = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Unknown"
        }

        val plugged = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }

        val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        info["Health"] = health
        info["Level"] = "$batteryPct %"
        info["Power Source"] = plugged
        info["Status"] = status
        info["Technology"] = technology
        info["Temperature"] = "$temp °C"
        info["Voltage"] = "$voltage mV"

        return info
    }
}
