package com.example.deviceinfo.feature.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import java.io.BufferedReader
import java.io.FileReader

/**
 * Battery Wear Level calculator.
 *
 * Strategy (no root required, Play Store safe ✅):
 *
 * 1. CHARGE_COUNTER → current charge in µAh → divide by 1000 → mAh remaining
 * 2. Design capacity estimation:
 *    a. Try /sys/class/power_supply/battery/charge_full_design  (µAh)
 *    b. Try /sys/class/power_supply/battery/charge_full         (µAh, learned capacity)
 *    c. Fallback: track historical max CHARGE_COUNTER across sessions (SharedPrefs)
 * 3. Wear level = 1 - (charge_full / charge_full_design)
 *
 * Note: charge_full_design file is readable on most OEM ROMs without root.
 * If unavailable, historical max provides a reasonable approximation.
 */
object BatteryWearManager {

    private const val PREFS_NAME     = "battery_wear"
    private const val KEY_MAX_MAH    = "max_observed_mah"
    private const val KEY_DESIGN_MAH = "design_mah_cached"

    data class WearResult(
        val chargeMah: Int,        // current charge from CHARGE_COUNTER
        val designMah: Int,        // design / full capacity estimate
        val learnedMah: Int,       // charge_full (learned by OS)
        val wearPct: Int,          // 0 = new, 100 = dead
        val currentMa: Int,        // current flow absolute mA
        val isCharging: Boolean,
        val voltageMv: Int,
        val tempC: Float,
        val sourceLabel: String    // where design capacity came from
    )

    fun compute(context: Context): WearResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // ── 1. Current charge (µAh → mAh) ─────────────────────────────
        val chargeUah = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val chargeMah = if (chargeUah > 0) chargeUah / 1000 else 0

        // ── 2. Current now (µA → mA) ───────────────────────────────────
        val currentUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (currentUa != Int.MIN_VALUE) Math.abs(currentUa) / 1000 else 0

        // ── 3. Battery intent (charging, voltage, temp) ────────────────
        val filter  = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent  = context.registerReceiver(null, filter)
        val status  = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tempRaw   = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC     = tempRaw / 10.0f

        // ── 4. Design capacity (try sysfs first) ───────────────────────
        val sysfsDesign  = readSysfsMah("/sys/class/power_supply/battery/charge_full_design")
        val sysfsFull    = readSysfsMah("/sys/class/power_supply/battery/charge_full")
        // Some devices use different path names
        val sysfsDesign2 = readSysfsMah("/sys/class/power_supply/Battery/charge_full_design")
        val sysfsFull2   = readSysfsMah("/sys/class/power_supply/Battery/charge_full")

        val designFromSysfs  = (sysfsDesign  ?: sysfsDesign2)
        val learnedFromSysfs = (sysfsFull    ?: sysfsFull2)

        // Update historical max (only when discharging & charge > 0)
        if (!isCharging && chargeMah > 100) {
            val prevMax = prefs.getInt(KEY_MAX_MAH, 0)
            if (chargeMah > prevMax) {
                prefs.edit().putInt(KEY_MAX_MAH, chargeMah).apply()
            }
        }
        // Cache sysfs design if we got it
        if (designFromSysfs != null && designFromSysfs > 500) {
            prefs.edit().putInt(KEY_DESIGN_MAH, designFromSysfs).apply()
        }

        val historicalMax  = prefs.getInt(KEY_MAX_MAH, 0)
        val cachedDesign   = prefs.getInt(KEY_DESIGN_MAH, 0)

        // Pick best design capacity value + source label
        val (designMah, sourceLabel) = when {
            designFromSysfs != null && designFromSysfs > 500 ->
                Pair(designFromSysfs, "sysfs (design)")
            cachedDesign > 500 ->
                Pair(cachedDesign, "cached sysfs")
            learnedFromSysfs != null && learnedFromSysfs > 500 ->
                Pair(learnedFromSysfs, "sysfs (learned)")
            historicalMax > 500 ->
                Pair(historicalMax, "observed max")
            else ->
                Pair(estimateDesignCapacityFromBenchmarks(context), "estimated")
        }

        val learnedMah = learnedFromSysfs ?: sysfsFull2 ?: 0

        // ── 5. Wear level ──────────────────────────────────────────────
        // If learned < design → battery has degraded
        val wearPct = if (designMah > 0 && learnedMah > 0 && learnedMah < designMah) {
            ((1f - learnedMah.toFloat() / designMah) * 100).toInt().coerceIn(0, 100)
        } else if (designMah > 0 && chargeMah > 0) {
            // Approximate: current charge vs design (rough lower bound when not at 100%)
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (level > 10) {
                val estimatedFull = (chargeMah * 100f / level).toInt()
                ((1f - estimatedFull.toFloat() / designMah) * 100).toInt().coerceIn(0, 95)
            } else 0
        } else 0

        return WearResult(
            chargeMah    = chargeMah,
            designMah    = designMah,
            learnedMah   = learnedMah,
            wearPct      = wearPct,
            currentMa    = currentMa,
            isCharging   = isCharging,
            voltageMv    = voltageMv,
            tempC        = tempC,
            sourceLabel  = sourceLabel
        )
    }

    /** Read a sysfs file that contains capacity in µAh, return mAh */
    private fun readSysfsMah(path: String): Int? = try {
        val raw = BufferedReader(FileReader(path)).use { it.readLine() }?.trim()?.toLongOrNull()
        if (raw != null && raw > 0) (raw / 1000).toInt() else null
    } catch (e: Exception) { null }

    /**
     * Fallback: estimate design capacity from RAM size (rough heuristic).
     * Flagship (12+ GB RAM) → ~5000 mAh, mid-range → ~4000, entry → ~3000
     */
    private fun estimateDesignCapacityFromBenchmarks(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val ramGb = mi.totalMem / (1024 * 1024 * 1024)
        return when {
            ramGb >= 12 -> 5000
            ramGb >= 8  -> 4500
            ramGb >= 6  -> 4000
            ramGb >= 4  -> 3800
            else        -> 3000
        }
    }
}
