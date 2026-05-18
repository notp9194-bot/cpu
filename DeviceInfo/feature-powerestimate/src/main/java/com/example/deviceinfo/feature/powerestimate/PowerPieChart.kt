package com.cpua.deviceinfo.feature.powerestimate

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View; import kotlin.math.cos; import kotlin.math.sin

class PowerPieChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    data class Slice(val label: String, val mw: Float, val color: Int)
    private var slices = listOf<Slice>()
    private val bg = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val tp = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val sp = Paint().apply { color = 0xFFCCCCCC.toInt(); textSize = 20f; isAntiAlias = true }

    fun setSlices(s: List<Slice>) { slices = s; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        if(slices.isEmpty()){tp.textAlign=Paint.Align.CENTER;canvas.drawText("Calculating…",w/2,h/2,tp);return}
        val total=slices.sumOf{it.mw.toDouble()}.toFloat().coerceAtLeast(1f)
        val cx=w*0.4f; val cy=h/2; val r=minOf(cx,cy)-20f; val innerR=r*0.45f
        var startAngle=-90f
        val paint=Paint().apply{style=Paint.Style.FILL;isAntiAlias=true}
        slices.forEach{s->
            val sweep=(s.mw/total)*360f; paint.color=s.color
            canvas.drawArc(cx-r,cy-r,cx+r,cy+r,startAngle,sweep,true,paint)
            // Sector label
            val midAngle=Math.toRadians((startAngle+sweep/2).toDouble())
            val lx=cx+(r*0.7f*cos(midAngle)).toFloat(); val ly=cy+(r*0.7f*sin(midAngle)).toFloat()
            tp.textSize=19f; tp.color=0xFFFFFFFF.toInt(); canvas.drawText("${"%.0f".format(s.mw/1000f)}W",lx,ly,tp)
            startAngle+=sweep
        }
        paint.color=0xFF1A1A2E.toInt(); canvas.drawCircle(cx,cy,innerR,paint)
        tp.textSize=22f; tp.color=0xFFFFFFFF.toInt(); canvas.drawText("${slices.sumOf{it.mw.toDouble()}.let{"${"%.1f".format(it/1000f)}W"}}",cx,cy+8f,tp)
        // Legend
        val lx2=w*0.72f
        slices.forEachIndexed{i,s->
            val y=24f+i*40f
            val dp=Paint().apply{color=s.color;isAntiAlias=true}
            canvas.drawCircle(lx2+12f,y+4f,8f,dp)
            sp.color=0xFFFFFFFF.toInt(); sp.textSize=20f
            canvas.drawText(s.label,lx2+28f,y+12f,sp)
            sp.color=0xFFAAAAAA.toInt(); sp.textSize=18f
            canvas.drawText("${"%.0f".format(s.mw/1000f)}W (${(s.mw/total*100).toInt()}%)",lx2+28f,y+30f,sp)
        }
    }
}
