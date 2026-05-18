package com.cpua.deviceinfo.feature.health

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Stores one Health Score entry per day (last 7 days) using SharedPreferences.
 * No permissions required. Play Store safe ✅
 *
 * Storage format (JSON array, newest last):
 *   [ { "date": "Mon", "score": 82, "grade": "Good", "ts": 1234567890 }, … ]
 */
object HealthScoreHistory {

    private const val PREFS_NAME = "health_score_history"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_DAYS    = 7

    data class Entry(
        val dateLabel: String,   // e.g. "Mon", "Tue"
        val score:     Int,      // 0–100
        val grade:     String,   // Excellent / Good / Fair / Poor / Critical
        val timestamp: Long      // epoch ms — used to deduplicate by calendar day
    )

    /** Record today's score. Replaces today's entry if already exists. */
    fun record(ctx: Context, score: Int, grade: String) {
        val prefs    = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = load(ctx).toMutableList()

        val now       = System.currentTimeMillis()
        val dayLabel  = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(now))
        val todayKey  = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))

        // Remove any existing entry for today
        existing.removeAll { entry ->
            SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(Date(entry.timestamp)) == todayKey
        }

        existing.add(Entry(dayLabel, score, grade, now))

        // Keep only last MAX_DAYS
        val trimmed = if (existing.size > MAX_DAYS)
            existing.takeLast(MAX_DAYS)
        else existing

        // Serialize
        val arr = JSONArray()
        trimmed.forEach { e ->
            arr.put(JSONObject().apply {
                put("date",  e.dateLabel)
                put("score", e.score)
                put("grade", e.grade)
                put("ts",    e.timestamp)
            })
        }
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }

    /** Load all stored entries, oldest first. Returns empty list if none. */
    fun load(ctx: Context): List<Entry> {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Entry(
                    dateLabel = obj.getString("date"),
                    score     = obj.getInt("score"),
                    grade     = obj.getString("grade"),
                    timestamp = obj.getLong("ts")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Average score across stored entries. Returns null if no data. */
    fun average(ctx: Context): Int? {
        val entries = load(ctx)
        return if (entries.isEmpty()) null
        else entries.map { it.score }.average().toInt()
    }
}
