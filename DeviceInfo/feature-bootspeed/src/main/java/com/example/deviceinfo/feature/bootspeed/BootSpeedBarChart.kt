package com.example.deviceinfo.feature.bootspeed

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class BootSpeedBarChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    data class BootEntry(val seconds: Float, val label: String)
    private var entries = listOf<BootEntry>()
    private val bg   = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val tp   = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize=24f; isAntiAlias=true; textAlign=Paint.Align.CENTER }
    private val sp   = Paint().apply { color = 0xFFAAAAAA.toInt(); textSize=20f; isAntiAlias=true; textAlign=Paint.Align.CENTER }

    private fun barColor(s: Float): Int = when {
        s < 10f -> 0xFF4CAF50.toInt()
        s < 20f -> 0xFFFF9800.toInt()
        else    -> 0xFFF44336.toInt()
    }

    fun setEntries(list: List<BootEntry>) { entries = list.takeLast(10); invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        if(entries.isEmpty()){canvas.drawText("No boot data yet",w/2,h/2,tp);return}
        val max=entries.maxOf{it.seconds}.coerceAtLeast(1f)
        val n=entries.size; val barW=(w-20f)/n-8f; val maxH=h-60f
        entries.forEachIndexed{i,e->
            val x=10f+i*(barW+8f); val bh=(e.seconds/max)*maxH
            val paint=Paint().apply{color=barColor(e.seconds);isAntiAlias=true}
            canvas.drawRoundRect(x,h-40f-bh,x+barW,h-40f,8f,8f,paint)
            tp.textSize=22f; tp.color=0xFFFFFFFF.toInt(); canvas.drawText("${e.seconds.toInt()}s",x+barW/2,h-42f-bh-4f,tp)
            sp.textSize=19f; canvas.drawText(e.label.take(8),x+barW/2,h-22f,sp)
        }
        // Average line
        val avg=entries.map{it.seconds}.average().toFloat()
        val avgY=h-40f-(avg/max)*maxH
        val avgPaint=Paint().apply{color=0xFFFFD700.toInt();strokeWidth=2f;style=Paint.Style.STROKE;pathEffect=DashPathEffect(floatArrayOf(8f,4f),0f)}
        canvas.drawLine(0f,avgY,w,avgY,avgPaint)
        tp.color=0xFFFFD700.toInt(); tp.textSize=20f; canvas.drawText("avg ${avg.toInt()}s",w-80f,avgY-4f,tp)
    }
}
