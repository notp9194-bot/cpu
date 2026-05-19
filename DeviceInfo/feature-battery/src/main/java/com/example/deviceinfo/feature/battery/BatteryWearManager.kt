package com.example.deviceinfo.feature.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.BufferedReader
import java.io.FileReader

/**
 * Battery Wear Level calculator — only real data, no estimates.
 *
 * Strategy (no root required, Play Store safe ✅):
 * 1. CHARGE_COUNTER → current charge in µAh → mAh remaining (real API data)
 * 2. Design capacity: only from sysfs files (charge_full_design / charge_full).
 *    If sysfs is unavailable → capacity shown as "Not Available on this device".
 *    NO RAM-based estimates. NO guesses.
 * 3. Wear level = only computed when both design and learned capacity are available.
 */
object BatteryWearManager {

    data class WearResult(
        val chargeMah: Int,
        val designMah: Int,       // -1 = not available
        val learnedMah: Int,      // -1 = not available
        val wearPct: Int,         // -1 = cannot compute
        val currentMa: Int,
        val isCharging: Boolean,
        val voltageMv: Int,
        val tempC: Float,
        val sourceLabel: String
    )

    fun compute(context: Context): WearResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        // ── Current charge (µAh → mAh) ─────────────────────────────
        val chargeUah = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val chargeMah = if (chargeUah > 0) chargeUah / 1000 else 0

        // ── Current now (µA → mA) ───────────────────────────────────
        val currentUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (currentUa != Int.MIN_VALUE) Math.abs(currentUa) / 1000 else 0

        // ── Battery intent (charging, voltage, temp) ────────────────
        val filter    = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent    = context.registerReceiver(null, filter)
        val status    = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tempRaw   = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC     = tempRaw / 10.0f

        // ── Design capacity — sysfs ONLY, no fallback estimates ─────
        val sysfsDesign  = readSysfsMah("/sys/class/power_supply/battery/charge_full_design")
            ?: readSysfsMah("/sys/class/power_supply/Battery/charge_full_design")
        val sysfsFull    = readSysfsMah("/sys/class/power_supply/battery/charge_full")
            ?: readSysfsMah("/sys/class/power_supply/Battery/charge_full")

        val designMah  = sysfsDesign ?: -1
        val learnedMah = sysfsFull   ?: -1

        val sourceLabel = when {
            sysfsDesign != null -> "sysfs (design)"
            sysfsFull   != null -> "sysfs (learned)"
            else                -> "Not available on this device"
        }

        // ── Wear level — only when real data exists ─────────────────
        val wearPct = if (designMah > 0 && learnedMah > 0 && learnedMah < designMah) {
            ((1f - learnedMah.toFloat() / designMah) * 100).toInt().coerceIn(0, 100)
        } else -1

        return WearResult(
            chargeMah  = chargeMah,
            designMah  = designMah,
            learnedMah = learnedMah,
            wearPct    = wearPct,
            currentMa  = currentMa,
            isCharging = isCharging,
            voltageMv  = voltageMv,
            tempC      = tempC,
            sourceLabel = sourceLabel
        )
    }

    /** Read a sysfs file that contains capacity in µAh, return mAh */
    private fun readSysfsMah(path: String): Int? = try {
        val raw = BufferedReader(FileReader(path)).use { it.readLine() }?.trim()?.toLongOrNull()
        if (raw != null && raw > 0) (raw / 1000).toInt() else null
    } catch (e: Exception) { null }
}
