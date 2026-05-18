package com.cpua.deviceinfo.feature.chargesession

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class ChargeSession(
    val startPct: Int, val endPct: Int,
    val durationMin: Int, val avgTempC: Float,
    val peakCurrentMa: Int, val date: String
)

object ChargeSessionManager {
    private const val PREFS = "charge_sessions"
    private const val KEY   = "sessions_json"

    fun save(ctx: Context, s: ChargeSession) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = try { JSONArray(prefs.getString(KEY, "[]")) } catch (_: Exception) { JSONArray() }
        val obj = JSONObject().apply {
            put("startPct", s.startPct); put("endPct", s.endPct)
            put("durationMin", s.durationMin); put("avgTempC", s.avgTempC)
            put("peakCurrentMa", s.peakCurrentMa); put("date", s.date)
        }
        arr.put(obj)
        // Keep last 30
        val trimmed = JSONArray()
        val start = if (arr.length() > 30) arr.length() - 30 else 0
        for (i in start until arr.length()) trimmed.put(arr.getJSONObject(i))
        prefs.edit().putString(KEY, trimmed.toString()).apply()
    }

    fun getAll(ctx: Context): List<ChargeSession> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return try {
            val arr = JSONArray(prefs.getString(KEY, "[]"))
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                ChargeSession(o.getInt("startPct"), o.getInt("endPct"),
                    o.getInt("durationMin"), o.getDouble("avgTempC").toFloat(),
                    o.getInt("peakCurrentMa"), o.getString("date"))
            }.reversed()
        } catch (_: Exception) { emptyList() }
    }

    fun clear(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()

    fun today(): String = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date())
}
