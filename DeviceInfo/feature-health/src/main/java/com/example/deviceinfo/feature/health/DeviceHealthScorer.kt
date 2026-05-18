package com.example.deviceinfo.feature.health

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.example.deviceinfo.core.util.DeviceUtils

/**
 * Computes a weighted Device Health Score (0–100).
 * All inputs use standard Android APIs — no root, no special permissions.
 * Play Store safe ✅
 *
 * Score breakdown (total weight = 100):
 *   Battery Level    30 pts  — linear 0→100%
 *   Battery Health   20 pts  — Good=20, else 0
 *   Battery Temp     15 pts  — ≤35°C=15, linear decay to 0 at 50°C
 *   Free Storage     20 pts  — linear 0→100% free
 *   Free RAM         15 pts  — linear 0→100% free
 *
 * Grade:
 *   90–100 → Excellent  🟢
 *   70–89  → Good       🟢
 *   50–69  → Fair       🟡
 *   30–49  → Poor       🟠
 *   0–29   → Critical   🔴
 */
object DeviceHealthScorer {

    data class ScoreBreakdown(
        val total:          Int,     // 0–100
        val batteryLevel:   Int,     // 0–30
        val batteryHealth:  Int,     // 0–20
        val batteryTemp:    Int,     // 0–15
        val freeStorage:    Int,     // 0–20
        val freeRam:        Int,     // 0–15
        val batteryPct:     Int,
        val batteryTempC:   Float,
        val storageFreeGb:  Float,
        val storageTotalGb: Float,
        val ramFreeGb:      Float,
        val ramTotalGb:     Float,
        val healthLabel:    String,
        val grade:          String,
        val gradeColor:     Int,
        val tip:            String
    )

    fun compute(ctx: Context): ScoreBreakdown {
        // ── Battery ───────────────────────────────────────────────────
        val battFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val battIntent = ctx.registerReceiver(null, battFilter)

        val level  = battIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale  = battIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val battPct = if (scale > 0) level * 100 / scale else 0

        val healthCode = battIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val healthLabel = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD      -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT  -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD       -> "Dead"
            BatteryManager.BATTERY_HEALTH_COLD       -> "Cold"
            else -> "Unknown"
        }

        val tempRaw  = battIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250) ?: 250
        val tempC    = tempRaw / 10f

        // ── Storage ───────────────────────────────────────────────────
        val statFs = StatFs(Environment.getDataDirectory().path)
        val blockSize   = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val freeBlocks  = statFs.availableBlocksLong
        val totalBytes  = totalBlocks * blockSize
        val freeBytes   = freeBlocks  * blockSize
        val storageTotalGb = totalBytes / 1_073_741_824f
        val storageFreeGb  = freeBytes  / 1_073_741_824f
        val storageFreeRatio = if (totalBytes > 0) freeBytes.toFloat() / totalBytes else 1f

        // ── RAM ───────────────────────────────────────────────────────
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val ramTotalGb = mi.totalMem / 1_073_741_824f
        val ramFreeGb  = mi.availMem / 1_073_741_824f
        val ramFreeRatio = if (mi.totalMem > 0) mi.availMem.toFloat() / mi.totalMem else 1f

        // ── Score computation ─────────────────────────────────────────
        val scoreBattLevel   = (battPct / 100f * 30).toInt().coerceIn(0, 30)
        val scoreBattHealth  = if (healthCode == BatteryManager.BATTERY_HEALTH_GOOD) 20 else 0
        val scoreBattTemp    = when {
            tempC <= 35f -> 15
            tempC >= 50f -> 0
            else -> ((50f - tempC) / 15f * 15).toInt()
        }.coerceIn(0, 15)
        val scoreStorage     = (storageFreeRatio * 20).toInt().coerceIn(0, 20)
        val scoreRam         = (ramFreeRatio * 15).toInt().coerceIn(0, 15)

        val total = scoreBattLevel + scoreBattHealth + scoreBattTemp + scoreStorage + scoreRam

        val (grade, gradeColor) = when {
            total >= 90 -> "Excellent" to 0xFF4CAF50.toInt()
            total >= 70 -> "Good"      to 0xFF8BC34A.toInt()
            total >= 50 -> "Fair"      to 0xFFFFEB3B.toInt()
            total >= 30 -> "Poor"      to 0xFFFF9800.toInt()
            else         -> "Critical" to 0xFFF44336.toInt()
        }

        val tip = buildTip(battPct, healthCode, tempC, storageFreeRatio, ramFreeRatio)

        return ScoreBreakdown(
            total          = total,
            batteryLevel   = scoreBattLevel,
            batteryHealth  = scoreBattHealth,
            batteryTemp    = scoreBattTemp,
            freeStorage    = scoreStorage,
            freeRam        = scoreRam,
            batteryPct     = battPct,
            batteryTempC   = tempC,
            storageFreeGb  = storageFreeGb,
            storageTotalGb = storageTotalGb,
            ramFreeGb      = ramFreeGb,
            ramTotalGb     = ramTotalGb,
            healthLabel    = healthLabel,
            grade          = grade,
            gradeColor     = gradeColor,
            tip            = tip
        )
    }

    private fun buildTip(battPct: Int, healthCode: Int, tempC: Float,
                          storageFreeRatio: Float, ramFreeRatio: Float): String {
        if (battPct < 20) return "⚠ Battery critically low. Charge now."
        if (healthCode != BatteryManager.BATTERY_HEALTH_GOOD)
            return "⚠ Battery health is degraded. Consider replacement."
        if (tempC > 45f) return "🌡 Device is overheating. Close heavy apps and cool down."
        if (storageFreeRatio < 0.10f) return "💾 Storage almost full. Delete unused files or apps."
        if (ramFreeRatio < 0.15f) return "💡 Low free RAM. Close background apps for better performance."
        if (battPct < 50) return "🔋 Battery below 50%. Charge when convenient."
        return "✅ Your device is in good condition. Keep it up!"
    }
}
