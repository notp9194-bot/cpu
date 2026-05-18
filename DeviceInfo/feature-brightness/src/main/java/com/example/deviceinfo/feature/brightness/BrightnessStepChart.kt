package com.cpua.deviceinfo.feature.brightness

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class BrightnessStepChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private val samples = ArrayDeque<Int>(); private val MAX = 60
    private val bg    = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val fill  = Paint().apply { color = 0x44FFD700; style = Paint.Style.FILL }
    private val line  = Paint().apply { color = 0xFFFFD700.toInt(); strokeWidth=3f; style=Paint.Style.STROKE; isAntiAlias=true }
    private val grid  = Paint().apply { color = 0x22FFFFFF; strokeWidth=1f }
    private val tp    = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize=22f; isAntiAlias=true }

    fun add(brightness: Int) {
        if (samples.isNotEmpty() && samples.last() == brightness) { invalidate(); return }
        if (samples.size >= MAX) samples.removeFirst()
        samples.addLast(brightness); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        for (g in listOf(64,128,192,255)) {
            val y=h-(g/255f)*(h-40f)-20f; canvas.drawLine(0f,y,w,y,grid)
            canvas.drawText("$g",4f,y-2f,tp)
        }
        if (samples.size < 2) { tp.textAlign=Paint.Align.CENTER; canvas.drawText("Tracking brightness changes…",w/2,h/2,tp); tp.textAlign=Paint.Align.LEFT; return }
        val step=w/(MAX-1).toFloat()
        val path=Path(); val fillPath=Path()
        samples.forEachIndexed{i,v->
            val x=i*step; val y=h-(v/255f)*(h-40f)-20f
            if(i==0){path.moveTo(x,y);fillPath.moveTo(x,h-20f);fillPath.lineTo(x,y)}
            else{
                // Step line: go horizontal first, then vertical
                path.lineTo(x,samples[i-1].let{h-(it/255f)*(h-40f)-20f})
                path.lineTo(x,y)
                fillPath.lineTo(x,samples[i-1].let{h-(it/255f)*(h-40f)-20f})
                fillPath.lineTo(x,y)
            }
        }
        fillPath.lineTo((samples.size-1)*step,h-20f); fillPath.close()
        canvas.drawPath(fillPath,fill); canvas.drawPath(path,line)
        val cur=samples.last()
        val autoBright=cur==0||cur>250
        tp.color=0xFFFFD700.toInt(); canvas.drawText("Brightness: $cur / 255  (${(cur*100/255)}%)",8f,h-4f,tp)
        tp.color=0xFFAAAAAA.toInt()
        canvas.drawText(if(autoBright)"Auto" else "Manual",w-120f,h-4f,tp)
        tp.color=0xFFCCCCCC.toInt()
    }
}
