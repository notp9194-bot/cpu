package com.example.deviceinfo.feature.bootspeed

import android.content.Context; import org.json.JSONArray; import org.json.JSONObject; import java.text.SimpleDateFormat; import java.util.*

object BootSpeedManager {
    private const val PREFS="boot_speed_prefs"; private const val KEY="boot_entries"

    fun recordBoot(ctx: Context, uptimeSec: Float) {
        val prefs=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val arr=try{JSONArray(prefs.getString(KEY,"[]"))}catch(_:Exception){JSONArray()}
        val obj=JSONObject().apply{put("sec",uptimeSec);put("date",SimpleDateFormat("MM/dd HH:mm",Locale.US).format(Date()))}
        arr.put(obj)
        val trimmed=JSONArray(); val start=if(arr.length()>20)arr.length()-20 else 0
        for(i in start until arr.length())trimmed.put(arr.getJSONObject(i))
        prefs.edit().putString(KEY,trimmed.toString()).apply()
    }

    fun getAll(ctx: Context): List<BootSpeedBarChart.BootEntry> {
        return try {
            val arr=JSONArray(ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]"))
            (0 until arr.length()).map{val o=arr.getJSONObject(it);BootSpeedBarChart.BootEntry(o.getDouble("sec").toFloat(),o.getString("date"))}
        } catch(_:Exception){emptyList()}
    }
}
