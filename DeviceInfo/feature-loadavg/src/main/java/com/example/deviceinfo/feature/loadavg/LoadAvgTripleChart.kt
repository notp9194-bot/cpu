package com.example.deviceinfo.feature.loadavg

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class LoadAvgTripleChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private val d1 = ArrayDeque<Float>(); private val d5 = ArrayDeque<Float>(); private val d15 = ArrayDeque<Float>()
    private val MAX = 60; private var coreCount = Runtime.getRuntime().availableProcessors().toFloat()
    private val bg = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val p1 = Paint().apply { color = 0xFF4CAF50.toInt(); strokeWidth=2.5f; style=Paint.Style.STROKE; isAntiAlias=true }
    private val p5 = Paint().apply { color = 0xFFFF9800.toInt(); strokeWidth=2.5f; style=Paint.Style.STROKE; isAntiAlias=true }
    private val p15= Paint().apply { color = 0xFFF44336.toInt(); strokeWidth=2f; style=Paint.Style.STROKE; isAntiAlias=true; pathEffect=DashPathEffect(floatArrayOf(8f,4f),0f) }
    private val overP=Paint().apply{color=0x33F44336;style=Paint.Style.FILL}
    private val grid=Paint().apply{color=0x22FFFFFF;strokeWidth=1f}
    private val tp  =Paint().apply{color=0xFFCCCCCC.toInt();textSize=22f;isAntiAlias=true}

    fun add(l1: Float, l5: Float, l15: Float) {
        fun push(d: ArrayDeque<Float>, v: Float) { if(d.size>=MAX)d.removeFirst(); d.addLast(v) }
        push(d1,l1); push(d5,l5); push(d15,l15); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        val cap=coreCount*1.5f; val step=w/(MAX-1).toFloat()
        // overload zone
        val overY=h-(coreCount/cap)*(h-40f)-20f
        canvas.drawRect(0f,0f,w,overY,overP)
        for(g in listOf(0.5f,1f,1.5f)) {
            val y=h-(g*coreCount/cap)*(h-40f)-20f; canvas.drawLine(0f,y,w,y,grid)
            canvas.drawText("${g}x",4f,y-2f,tp)
        }
        fun draw(data: ArrayDeque<Float>, paint: Paint) {
            if(data.size<2) return; val path=Path()
            data.forEachIndexed{i,v->val x=i*step;val y=h-(v/cap)*(h-40f)-20f;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
            canvas.drawPath(path,paint)
        }
        draw(d15,p15); draw(d5,p5); draw(d1,p1)
        tp.color=0xFF4CAF50.toInt(); canvas.drawText("1min: ${d1.lastOrNull()?:0f}",8f,20f,tp)
        tp.color=0xFFFF9800.toInt(); canvas.drawText("5min: ${d5.lastOrNull()?:0f}",w/3,20f,tp)
        tp.color=0xFFF44336.toInt(); canvas.drawText("15min: ${d15.lastOrNull()?:0f}",w*2/3,20f,tp)
        tp.color=0xFFCCCCCC.toInt()
    }
}
