package com.example.deviceinfo.feature.appmemory

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class AppMemoryBarChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    data class AppMemEntry(val name: String, val mb: Float)
    private var entries = listOf<AppMemEntry>()
    private val colors = listOf(0xFF4CAF50.toInt(),0xFF2196F3.toInt(),0xFFFF9800.toInt(),0xFFF44336.toInt(),
        0xFF9C27B0.toInt(),0xFF00BCD4.toInt(),0xFFFFEB3B.toInt(),0xFFFF5722.toInt(),0xFF795548.toInt(),0xFF607D8B.toInt())
    private val bg=Paint().apply{color=0xFF1A1A2E.toInt()}
    private val tp=Paint().apply{color=0xFFFFFFFF.toInt();textSize=24f;isAntiAlias=true}

    fun setEntries(list: List<AppMemEntry>) { entries=list.take(10); invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        if(entries.isEmpty()){tp.textAlign=Paint.Align.CENTER;canvas.drawText("Loading…",w/2,h/2,tp);tp.textAlign=Paint.Align.LEFT;return}
        val max=entries.maxOf{it.mb}.coerceAtLeast(1f)
        val barH=(h-20f)/entries.size-6f
        entries.forEachIndexed{i,e->
            val y=i*(barH+6f)+10f
            val barW=(e.mb/max)*(w-120f)
            val paint=Paint().apply{color=colors[i%colors.size];isAntiAlias=true}
            canvas.drawRoundRect(0f,y,barW,y+barH,8f,8f,paint)
            tp.textSize=22f; tp.color=0xFFFFFFFF.toInt()
            canvas.drawText("${"%.0f".format(e.mb)}MB",barW+6f,y+barH*0.7f,tp)
            tp.textSize=20f; tp.color=0xFFCCCCCC.toInt()
            canvas.drawText(e.name.take(18),4f,y+barH*0.7f,tp)
        }
    }
}
