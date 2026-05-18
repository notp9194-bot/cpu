package com.cpua.deviceinfo.feature.screentime

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object ScreenTimeManager {
    private const val PREFS="screen_time_prefs"

    fun addScreenOnTime(ctx: Context, ms: Long) {
        val today=todayKey()
        val prefs=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val cur=prefs.getLong(today,0L)
        prefs.edit().putLong(today,cur+ms).apply()
    }

    fun getLast7Days(ctx: Context): List<Pair<String,Long>> {
        val prefs=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val cal=Calendar.getInstance(); val fmt=SimpleDateFormat("yyyy-MM-dd",Locale.US)
        val label=SimpleDateFormat("EEE",Locale.US)
        return (6 downTo 0).map {
            cal.time=Date(); cal.add(Calendar.DAY_OF_YEAR,-it)
            val key=fmt.format(cal.time); val lbl=label.format(cal.time)
            lbl to (prefs.getLong(key,0L))
        }
    }

    fun todayKey()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
    fun todayMs(ctx: Context)=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getLong(todayKey(),0L)
    fun formatMs(ms:Long):String{val h=ms/3600000;val m=(ms%3600000)/60000;return if(h>0)"${h}h ${m}m" else "${m}m"}
}
