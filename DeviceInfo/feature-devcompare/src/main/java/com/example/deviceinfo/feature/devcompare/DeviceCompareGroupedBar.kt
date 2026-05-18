package com.cpua.deviceinfo.feature.devcompare

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class DeviceCompareGroupedBar @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    data class MetricPair(val label: String, val yourVal: Float, val compareVal: Float, val max: Float, val unit: String)
    private var metrics = listOf<MetricPair>()
    private val bg    = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val you   = Paint().apply { color = 0xFF4CAF50.toInt(); isAntiAlias = true }
    private val comp  = Paint().apply { color = 0xFF2196F3.toInt(); isAntiAlias = true }
    private val tp    = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize = 22f; isAntiAlias = true }
    private val sp    = Paint().apply { color = 0xFFAAAAAA.toInt(); textSize = 19f; isAntiAlias = true }

    fun setMetrics(list: List<MetricPair>) { metrics = list; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        if(metrics.isEmpty()){tp.textAlign=Paint.Align.CENTER;canvas.drawText("Select a device to compare",w/2,h/2,tp);tp.textAlign=Paint.Align.LEFT;return}
        val labelW=200f; val barArea=w-labelW-20f; val rowH=h/metrics.size
        metrics.forEachIndexed{i,m->
            val y=i*rowH; val mid=y+rowH/2; val bH=(rowH-12f)/2f
            val maxW=barArea*0.9f
            val yw=(m.yourVal/m.max)*maxW; val cw=(m.compareVal/m.max)*maxW
            // Your bar
            canvas.drawRoundRect(labelW,mid-bH,labelW+yw,mid,4f,4f,you)
            // Compare bar
            canvas.drawRoundRect(labelW,mid,labelW+cw,mid+bH,4f,4f,comp)
            // Labels
            tp.textSize=20f; tp.color=0xFFFFFFFF.toInt(); canvas.drawText(m.label,4f,mid+6f,tp)
            sp.color=0xFF4CAF50.toInt(); canvas.drawText("${"%.0f".format(m.yourVal)}${m.unit}",labelW+yw+4f,mid-2f,sp)
            sp.color=0xFF2196F3.toInt(); canvas.drawText("${"%.0f".format(m.compareVal)}${m.unit}",labelW+cw+4f,mid+bH-2f,sp)
        }
        // Legend
        tp.textSize=20f; tp.color=0xFF4CAF50.toInt(); canvas.drawText("■ Your Device",8f,h-26f,tp)
        tp.color=0xFF2196F3.toInt(); canvas.drawText("■ Compare",w/2,h-26f,tp)
        tp.color=0xFFCCCCCC.toInt()
    }
}
