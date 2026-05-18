package com.cpua.deviceinfo.feature.battery

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists battery % readings every 5 minutes using SharedPreferences.
 * Keeps last 288 points = 24 hours. No permissions needed.
 * Play Store safe ✅
 */
object BatteryHistoryManager {

    private const val PREFS_NAME  = "battery_history"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_POINTS  = 288          // 24h × 12 per hour
    private const val INTERVAL_MS = 5 * 60 * 1000L  // 5 minutes

    data class Entry(val timestampMs: Long, val percent: Int)

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(ctx: Context): List<Entry> {
        val raw = prefs(ctx).getString(KEY_ENTRIES, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Entry(o.getLong("ts"), o.getInt("pct"))
            }
        } catch (e: Exception) { emptyList() }
    }

    /**
     * Call this from BatteryFragment every 5 minutes (or on battery change).
     * Internally throttles — will not double-log within INTERVAL_MS.
     */
    fun record(ctx: Context, percent: Int) {
        val now     = System.currentTimeMillis()
        val entries = getAll(ctx).toMutableList()

        // Throttle: skip if last entry is too recent
        val last = entries.lastOrNull()
        if (last != null && (now - last.timestampMs) < INTERVAL_MS) return

        entries.add(Entry(now, percent))
        if (entries.size > MAX_POINTS) entries.removeAt(0)
        save(ctx, entries)
    }

    fun clearAll(ctx: Context) = save(ctx, emptyList())

    private fun save(ctx: Context, entries: List<Entry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("ts",  e.timestampMs)
                put("pct", e.percent)
            })
        }
        prefs(ctx).edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }
}
