package com.example.deviceinfo.feature.screentime

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class ScreenTimeDayBarChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private var data = listOf<Pair<String,Long>>()
    private val bg=Paint().apply{color=0xFF1A1A2E.toInt()}
    private val barPaint=Paint().apply{color=0xFF2196F3.toInt();isAntiAlias=true}
    private val todayPaint=Paint().apply{color=0xFF4CAF50.toInt();isAntiAlias=true}
    private val tp=Paint().apply{color=0xFFFFFFFF.toInt();textSize=26f;isAntiAlias=true;textAlign=Paint.Align.CENTER}
    private val sp=Paint().apply{color=0xFFAAAAAA.toInt();textSize=22f;isAntiAlias=true;textAlign=Paint.Align.CENTER}

    fun setData(d: List<Pair<String,Long>>) { data=d; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        if(data.isEmpty()) return
        val max=data.maxOf{it.second}.coerceAtLeast(1L)
        val barW=(w-20f)/data.size-8f; val maxH=h-60f
        data.forEachIndexed{i,(day,ms)->
            val x=10f+i*(barW+8f); val bh=(ms.toFloat()/max)*maxH
            val paint=if(i==data.size-1)todayPaint else barPaint
            canvas.drawRoundRect(x,h-40f-bh,x+barW,h-40f,8f,8f,paint)
            tp.textSize=24f; canvas.drawText(ScreenTimeManager.formatMs(ms),x+barW/2,h-42f-bh-4f,tp)
            sp.textSize=22f; canvas.drawText(day,x+barW/2,h-20f,sp)
        }
    }
}
