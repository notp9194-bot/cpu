package com.example.deviceinfo.core.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
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
            BatteryManager.BATTERY_STATUS_CHARGING    -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL        -> "Full"
            else -> "Unknown"
        }
        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD     -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD     -> "Dead"
            else -> "Unknown"
        }
        val plugged = when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC       -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB      -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery (Unplugged)"
        }
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        val currentMa: String = if (currentNow != Int.MIN_VALUE) {
            val ma = Math.abs(currentNow / 1000)
            val dir = if (currentNow > 0) "Charging" else "Draining"
            "$ma mA ($dir)"
        } else {
            "Unknown"
        }

        val chargeMah: String = if (chargeCounter > 0) {
            val mah = chargeCounter / 1000
            "$mah mAh remaining"
        } else {
            "Unknown"
        }

        return linkedMapOf(
            "Health"           to health,
            "Level"            to "$pct %",
            "Power Source"     to plugged,
            "Status"           to status,
            "Technology"       to tech,
            "Temperature"      to "$temp °C",
            "Voltage"          to "$voltage mV",
            "Current Now"      to currentMa,
            "Charge Remaining" to chargeMah
        )
    }

    fun getBatteryPercent(context: Context): Int {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) level * 100 / scale else 0
    }

    fun getGpuInfo(): Map<String, String> {
        return try {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val major = IntArray(1)
            val minor = IntArray(1)
            EGL14.eglInitialize(display, major, 0, minor, 0)
            val attribs = intArrayOf(EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE)
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfigs, 0)
            val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val ctx = EGL14.eglCreateContext(display, configs[0]!!, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            val surfAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            val surf = EGL14.eglCreatePbufferSurface(display, configs[0]!!, surfAttribs, 0)
            EGL14.eglMakeCurrent(display, surf, surf, ctx)
            val vendor   = GLES20.glGetString(GLES20.GL_VENDOR)   ?: "Unknown"
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
            val version  = GLES20.glGetString(GLES20.GL_VERSION)  ?: "Unknown"
            EGL14.eglDestroyContext(display, ctx)
            EGL14.eglDestroySurface(display, surf)
            EGL14.eglTerminate(display)
            mapOf("GPU Vendor" to vendor, "GPU Renderer" to renderer, "OpenGL ES Version" to version)
        } catch (e: Exception) {
            mapOf("GPU Vendor" to "Unknown", "GPU Renderer" to "Unknown", "OpenGL ES Version" to "Unknown")
        }
    }

    // BUG FIX #3: WifiManager.getConnectionInfo() deprecated on API 31+
    // Use NetworkCapabilities.transportInfo for API 31+, fallback for older
    fun getNetworkInfo(context: Context): Map<String, String> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = linkedMapOf<String, String>()
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        if (caps == null) {
            info["Connection"] = "No Network"
            return info
        }
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)      -> "VPN"
            else -> "Unknown"
        }
        info["Connection Type"] = type
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        info["Internet Access"] = if (hasInternet) "Yes" else "No"

        val dlMbps = caps.linkDownstreamBandwidthKbps / 1000
        val ulMbps = caps.linkUpstreamBandwidthKbps / 1000
        info["Download Speed"] = "$dlMbps Mbps (est.)"
        info["Upload Speed"]   = "$ulMbps Mbps (est.)"

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ — use transportInfo (non-deprecated)
                val wifiInfo = caps.transportInfo as? WifiInfo
                if (wifiInfo != null) {
                    val ssid = wifiInfo.ssid
                    info["WiFi SSID"]       = if (ssid.isNullOrBlank() || ssid == "<unknown ssid>") "Hidden/Unknown" else ssid.removeSurrounding("\"")
                    info["WiFi Frequency"]  = "${wifiInfo.frequency} MHz (${if (wifiInfo.frequency > 4000) "5 GHz" else "2.4 GHz"})"
                    info["WiFi Link Speed"] = "${wifiInfo.linkSpeed} Mbps"
                    val bars = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
                    info["WiFi Signal"]     = "$bars/5 bars (${wifiInfo.rssi} dBm)"
                    info["IP Address"]      = intToIp(wifiInfo.ipAddress)
                }
            } else {
                @Suppress("DEPRECATION")
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val wifiInfo = wm.connectionInfo
                val ssid = wifiInfo.ssid
                info["WiFi SSID"]       = if (ssid.isNullOrBlank() || ssid == "<unknown ssid>") "Hidden/Unknown" else ssid.removeSurrounding("\"")
                info["WiFi Frequency"]  = "${wifiInfo.frequency} MHz (${if (wifiInfo.frequency > 4000) "5 GHz" else "2.4 GHz"})"
                info["WiFi Link Speed"] = "${wifiInfo.linkSpeed} Mbps"
                @Suppress("DEPRECATION")
                val bars = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
                info["WiFi Signal"]     = "$bars/5 bars (${wifiInfo.rssi} dBm)"
                info["IP Address"]      = intToIp(wifiInfo.ipAddress)
            }
        }

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            info["Network Metered"] = if (cm.isActiveNetworkMetered) "Yes" else "No"
        }
        return info
    }

    private fun intToIp(i: Int): String =
        "${i and 0xFF}.${i shr 8 and 0xFF}.${i shr 16 and 0xFF}.${i shr 24 and 0xFF}"
}
