package com.example.deviceinfo.feature.storageio

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class StorageIoChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private val readData  = ArrayDeque<Float>()
    private val writeData = ArrayDeque<Float>()
    private val MAX = 60
    private val bg    = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val rPaint = Paint().apply { color = 0xFF4CAF50.toInt(); strokeWidth=2.5f; style=Paint.Style.STROKE; isAntiAlias=true }
    private val wPaint = Paint().apply { color = 0xFFF44336.toInt(); strokeWidth=2.5f; style=Paint.Style.STROKE; isAntiAlias=true }
    private val rFill  = Paint().apply { color = 0x334CAF50 }
    private val wFill  = Paint().apply { color = 0x33F44336 }
    private val grid   = Paint().apply { color = 0x22FFFFFF; strokeWidth=1f }
    private val tp     = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize=26f; isAntiAlias=true }

    fun add(readMbps: Float, writeMbps: Float) {
        fun push(d: ArrayDeque<Float>, v: Float) { if (d.size >= MAX) d.removeFirst(); d.addLast(v) }
        push(readData, readMbps); push(writeData, writeMbps); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat()
        canvas.drawRect(0f,0f,w,h,bg)
        val mx = maxOf(readData.maxOrNull()?:10f, writeData.maxOrNull()?:10f, 10f)
        for (g in listOf(25f,50f,75f,100f)) {
            val y=h-(g/100f)*(h-40f)-20f; canvas.drawLine(0f,y,w,y,grid)
            tp.textSize=22f; canvas.drawText("${(g/100f*mx).toInt()}MB/s",4f,y-2f,tp)
        }
        fun drawLine(data: ArrayDeque<Float>, lp: Paint, fp: Paint) {
            if (data.size < 2) return
            val step = w/(MAX-1).toFloat(); val path=Path(); val fill=Path()
            data.forEachIndexed { i,v ->
                val x=i*step; val y=h-(v/mx)*(h-40f)-20f
                if(i==0){path.moveTo(x,y);fill.moveTo(x,h-20f);fill.lineTo(x,y)}
                else{path.lineTo(x,y);fill.lineTo(x,y)}
            }
            fill.lineTo((data.size-1)*step,h-20f); fill.close()
            canvas.drawPath(fill,fp); canvas.drawPath(path,lp)
        }
        drawLine(readData,rPaint,rFill); drawLine(writeData,wPaint,wFill)
        tp.textSize=26f
        val rv=readData.lastOrNull()?:0f; val wv=writeData.lastOrNull()?:0f
        canvas.drawText("▶ Read: ${"%.1f".format(rv)} MB/s",16f,h-28f,tp.also{it.color=0xFF4CAF50.toInt()})
        canvas.drawText("◀ Write: ${"%.1f".format(wv)} MB/s",w/2,h-28f,tp.also{it.color=0xFFF44336.toInt()})
        tp.color=0xFFFFFFFF.toInt()
    }
}
