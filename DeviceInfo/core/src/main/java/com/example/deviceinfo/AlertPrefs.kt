package com.cpua.deviceinfo

import android.content.Context

/**
 * Central store for all user-configurable alert thresholds.
 * SharedPreferences — no root, no special permission. Play Store safe ✅
 *
 * Defaults:
 *   Battery Low         → 20%
 *   Battery Full        → 100%   (toggle on/off)
 *   RAM High            → 90%
 *   Temperature High    → 50°C
 */
object AlertPrefs {

    private const val PREFS = "alert_prefs"

    // Keys
    const val KEY_BATT_LOW_ENABLED   = "batt_low_enabled"
    const val KEY_BATT_LOW_PCT       = "batt_low_pct"
    const val KEY_BATT_FULL_ENABLED  = "batt_full_enabled"
    const val KEY_RAM_ENABLED        = "ram_enabled"
    const val KEY_RAM_PCT            = "ram_pct"
    const val KEY_TEMP_ENABLED       = "temp_enabled"
    const val KEY_TEMP_C             = "temp_c"

    // Defaults
    const val DEF_BATT_LOW_PCT  = 20
    const val DEF_BATT_FULL_PCT = 100
    const val DEF_RAM_PCT       = 90
    const val DEF_TEMP_C        = 50

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isBattLowEnabled(ctx: Context)  = prefs(ctx).getBoolean(KEY_BATT_LOW_ENABLED,  true)
    fun isBattFullEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_BATT_FULL_ENABLED, true)
    fun isRamEnabled(ctx: Context)      = prefs(ctx).getBoolean(KEY_RAM_ENABLED,        true)
    fun isTempEnabled(ctx: Context)     = prefs(ctx).getBoolean(KEY_TEMP_ENABLED,       true)

    fun getBattLowPct(ctx: Context)  = prefs(ctx).getInt(KEY_BATT_LOW_PCT, DEF_BATT_LOW_PCT)
    fun getRamPct(ctx: Context)      = prefs(ctx).getInt(KEY_RAM_PCT,      DEF_RAM_PCT)
    fun getTempC(ctx: Context)       = prefs(ctx).getInt(KEY_TEMP_C,       DEF_TEMP_C)

    fun save(
        ctx: Context,
        battLowEnabled: Boolean, battLowPct: Int,
        battFullEnabled: Boolean,
        ramEnabled: Boolean,     ramPct: Int,
        tempEnabled: Boolean,    tempC: Int
    ) {
        prefs(ctx).edit()
            .putBoolean(KEY_BATT_LOW_ENABLED,  battLowEnabled)
            .putInt(KEY_BATT_LOW_PCT,          battLowPct)
            .putBoolean(KEY_BATT_FULL_ENABLED, battFullEnabled)
            .putBoolean(KEY_RAM_ENABLED,        ramEnabled)
            .putInt(KEY_RAM_PCT,               ramPct)
            .putBoolean(KEY_TEMP_ENABLED,       tempEnabled)
            .putInt(KEY_TEMP_C,                tempC)
            .apply()
    }
}
