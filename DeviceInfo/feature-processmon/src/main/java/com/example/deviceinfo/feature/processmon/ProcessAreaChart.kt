package com.example.deviceinfo.feature.processmon

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class ProcessAreaChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private val sysData = ArrayDeque<Int>(); private val usrData = ArrayDeque<Int>(); private val MAX = 60
    private val bg   = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val sysFill = Paint().apply { color = 0x662196F3; style = Paint.Style.FILL }
    private val usrFill = Paint().apply { color = 0x664CAF50; style = Paint.Style.FILL }
    private val sysLine = Paint().apply { color = 0xFF2196F3.toInt(); strokeWidth=2.5f; style=Paint.Style.STROKE; isAntiAlias=true }
    private val usrLine = Paint().apply { color = 0xFF4CAF50.toInt(); strokeWidth=2.5f; style=Paint.Style.STROKE; isAntiAlias=true }
    private val grid = Paint().apply { color = 0x22FFFFFF; strokeWidth=1f }
    private val tp = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize=22f; isAntiAlias=true }

    fun add(sys: Int, usr: Int) {
        fun push(d: ArrayDeque<Int>, v: Int) { if(d.size>=MAX)d.removeFirst(); d.addLast(v) }
        push(sysData, sys); push(usrData, usr); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        val maxV = maxOf(sysData.maxOrNull()?:1, usrData.maxOrNull()?:1, 10).toFloat()
        for(g in listOf(0.25f,0.5f,0.75f,1f)) {
            val y=h-(g)*(h-40f)-20f; canvas.drawLine(0f,y,w,y,grid)
            canvas.drawText("${(g*maxV).toInt()}",4f,y-2f,tp)
        }
        val step=w/(MAX-1).toFloat()
        fun drawArea(data: ArrayDeque<Int>, fillP: Paint, lineP: Paint) {
            if(data.size<2) return
            val path=Path(); val fill=Path()
            data.forEachIndexed{i,v->
                val x=i*step; val y=h-(v/maxV)*(h-40f)-20f
                if(i==0){path.moveTo(x,y);fill.moveTo(x,h-20f);fill.lineTo(x,y)}else{path.lineTo(x,y);fill.lineTo(x,y)}
            }
            fill.lineTo((data.size-1)*step,h-20f); fill.close()
            canvas.drawPath(fill,fillP); canvas.drawPath(path,lineP)
        }
        drawArea(sysData, sysFill, sysLine); drawArea(usrData, usrFill, usrLine)
        tp.color=0xFF2196F3.toInt(); canvas.drawText("■ System: ${sysData.lastOrNull()?:0}",8f,20f,tp)
        tp.color=0xFF4CAF50.toInt(); canvas.drawText("■ User: ${usrData.lastOrNull()?:0}",w/2,20f,tp)
        tp.color=0xFFCCCCCC.toInt()
    }
}
