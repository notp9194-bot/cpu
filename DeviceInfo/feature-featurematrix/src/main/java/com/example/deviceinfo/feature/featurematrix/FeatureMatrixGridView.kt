package com.example.deviceinfo.feature.featurematrix

import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View

class FeatureMatrixGridView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    enum class Status { YES, NO, UNKNOWN }
    data class Feature(val name: String, val category: String, val status: Status, val detail: String="")
    private var features = listOf<Feature>()
    private val bg        = Paint().apply { color = 0xFF1A1A2E.toInt() }
    private val yesPaint  = Paint().apply { color = 0xFF4CAF50.toInt(); isAntiAlias=true }
    private val noPaint   = Paint().apply { color = 0xFFF44336.toInt(); isAntiAlias=true }
    private val unkPaint  = Paint().apply { color = 0xFF9E9E9E.toInt(); isAntiAlias=true }
    private val tp        = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize=22f; isAntiAlias=true }
    private val sp        = Paint().apply { color = 0xFFAAAAAA.toInt(); textSize=18f; isAntiAlias=true }
    private val catPaint  = Paint().apply { color = 0xFF333355.toInt(); isAntiAlias=true }

    fun setFeatures(list: List<Feature>) { features=list; invalidate() }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w=MeasureSpec.getSize(wSpec)
        val rowH=56; val h=features.size*rowH+40
        setMeasuredDimension(w,h)
    }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); canvas.drawRect(0f,0f,w,height.toFloat(),bg)
        var lastCat=""; var y=0f
        features.forEach{f->
            if(f.category!=lastCat){
                lastCat=f.category; canvas.drawRect(0f,y,w,y+28f,catPaint)
                tp.textSize=20f; tp.color=0xFFCCCCCC.toInt(); canvas.drawText(f.category.uppercase(),8f,y+20f,tp)
                y+=28f
            }
            val paint=when(f.status){Status.YES->yesPaint;Status.NO->noPaint;Status.UNKNOWN->unkPaint}
            val icon=when(f.status){Status.YES->"✅";Status.NO->"❌";Status.UNKNOWN->"❓"}
            canvas.drawRoundRect(4f,y+4f,w-4f,y+52f,8f,8f,Paint().apply{color=0xFF222233.toInt();isAntiAlias=true})
            canvas.drawCircle(30f,y+28f,12f,paint)
            tp.textSize=22f; tp.color=0xFFFFFFFF.toInt(); canvas.drawText("$icon ${f.name}",52f,y+24f,tp)
            sp.textSize=18f; sp.color=0xFFAAAAAA.toInt()
            if(f.detail.isNotEmpty()) canvas.drawText(f.detail,52f,y+44f,sp)
            y+=56f
        }
    }
}
