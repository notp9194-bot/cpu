package com.cpua.deviceinfo.feature.chargecurve

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class ChargeCurveDualAxisChart @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    data class Sample(val tempC: Float, val voltageMv: Int, val currentMa: Int, val pct: Int)
    private val samples = ArrayDeque<Sample>(); private val MAX = 60
    private val bg   = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val tP   = Paint().apply { color = 0xFFFF9800.toInt(); strokeWidth=2.5f; style=Paint.Style.STROKE; isAntiAlias=true }
    private val vP   = Paint().apply { color = 0xFF2196F3.toInt(); strokeWidth=2.5f; style=Paint.Style.STROKE; isAntiAlias=true }
    private val cP   = Paint().apply { color = 0xFF4CAF50.toInt(); strokeWidth=2f;   style=Paint.Style.STROKE; isAntiAlias=true; pathEffect=DashPathEffect(floatArrayOf(6f,3f),0f) }
    private val grid = Paint().apply { color = 0x22FFFFFF; strokeWidth=1f }
    private val tp   = Paint().apply { color = 0xFFCCCCCC.toInt(); textSize=22f; isAntiAlias=true }

    fun add(s: Sample) { if(samples.size>=MAX)samples.removeFirst(); samples.addLast(s); invalidate() }
    fun clear() { samples.clear(); invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        for(g in 1..4){val y=h*(g/5f);canvas.drawLine(0f,y,w,y,grid)}
        if(samples.size<2){tp.textAlign=Paint.Align.CENTER;canvas.drawText("Plug in to record charging curve",w/2,h/2,tp);tp.textAlign=Paint.Align.LEFT;return}
        val step=w/(MAX-1).toFloat()
        val maxV=samples.maxOf{it.voltageMv}.toFloat().coerceAtLeast(4200f)
        val minV=samples.minOf{it.voltageMv}.toFloat().coerceAtMost(3500f)
        val maxC=samples.maxOf{it.currentMa}.toFloat().coerceAtLeast(500f)
        val maxT=samples.maxOf{it.tempC}.coerceAtLeast(40f)
        fun norm(v:Float,mn:Float,mx:Float)=((v-mn)/(mx-mn)).coerceIn(0f,1f)
        fun drawSeries(getter:(Sample)->Float,mn:Float,mx:Float,paint:Paint){
            if(samples.size<2)return; val path=Path()
            samples.forEachIndexed{i,s->val x=i*step;val y=h-(norm(getter(s),mn,mx))*(h-40f)-20f;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
            canvas.drawPath(path,paint)
        }
        drawSeries({it.voltageMv.toFloat()},minV,maxV,vP)
        drawSeries({it.tempC},20f,maxT,tP)
        drawSeries({it.currentMa.toFloat()},0f,maxC,cP)
        tp.color=0xFF2196F3.toInt(); canvas.drawText("⚡ Voltage: ${samples.last().voltageMv}mV",8f,20f,tp)
        tp.color=0xFFFF9800.toInt(); canvas.drawText("🌡 Temp: ${"%.1f".format(samples.last().tempC)}°C",w/3,20f,tp)
        tp.color=0xFF4CAF50.toInt(); canvas.drawText("⚡ Current: ${samples.last().currentMa}mA",w*2/3,20f,tp)
        tp.color=0xFFCCCCCC.toInt()
    }
}
