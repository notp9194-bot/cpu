package com.example.deviceinfo.feature.dischargerate

import android.content.*; import android.graphics.*; import android.os.*; import android.util.AttributeSet
import android.view.*; import android.widget.*; import androidx.fragment.app.Fragment
import com.example.deviceinfo.core.ui.ShareableFragment; import java.text.SimpleDateFormat; import java.util.*

class DischargeScatterChart @JvmOverloads constructor(ctx: android.content.Context, attrs: AttributeSet?=null) : android.view.View(ctx,attrs) {
    data class Point(val timeMin: Float, val pct: Int)
    private val points=mutableListOf<Point>()
    private val bg=Paint().apply{color=0xFF1A1A2E.toInt()}
    private val dot=Paint().apply{color=0xFF4CAF50.toInt();isAntiAlias=true}
    private val line=Paint().apply{color=0xFFFF9800.toInt();strokeWidth=2f;style=Paint.Style.STROKE;isAntiAlias=true}
    private val grid=Paint().apply{color=0x22FFFFFF;strokeWidth=1f}
    private val tp=Paint().apply{color=0xFFCCCCCC.toInt();textSize=22f;isAntiAlias=true}

    fun add(timeMin: Float, pct: Int) { points.add(Point(timeMin,pct)); if(points.size>100)points.removeAt(0); invalidate() }
    fun clear() { points.clear(); invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); canvas.drawRect(0f,0f,w,h,bg)
        for(g in listOf(25,50,75,100)){
            val y=h-(g/100f)*(h-40f)-20f; canvas.drawLine(0f,y,w,y,grid)
            canvas.drawText("$g%",4f,y-2f,tp)
        }
        if(points.size<2){tp.textAlign=Paint.Align.CENTER;canvas.drawText("Recording discharge… keep screen on",w/2,h/2,tp);tp.textAlign=Paint.Align.LEFT;return}
        val maxT=points.maxOf{it.timeMin}.coerceAtLeast(1f)
        points.forEach{p->
            val x=(p.timeMin/maxT)*(w-40f)+20f; val y=h-(p.pct/100f)*(h-40f)-20f
            canvas.drawCircle(x,y,5f,dot)
        }
        // Trend line (simple linear regression)
        val n=points.size.toFloat()
        val sx=points.sumOf{it.timeMin.toDouble()}; val sy=points.sumOf{it.pct.toDouble()}
        val sx2=points.sumOf{(it.timeMin*it.timeMin).toDouble()}; val sxy=points.sumOf{(it.timeMin*it.pct).toDouble()}
        val slope=(n*sxy-sx*sy)/(n*sx2-sx*sx)
        val intercept=(sy-slope*sx)/n
        val x0=20f; val x1=w-20f
        val t0=0f; val t1=maxT
        val y0=h-((slope*t0+intercept)/100f)*(h-40f)-20f
        val y1=h-((slope*t1+intercept)/100f)*(h-40f)-20f
        canvas.drawLine(x0,y0.toFloat().coerceIn(0f,h),x1,y1.toFloat().coerceIn(0f,h),line)
        tp.color=0xFFFF9800.toInt()
        val ratePerHr=slope*60; canvas.drawText("Rate: ${"%.1f".format(-ratePerHr)}%/hr",w-220f,h-28f,tp)
        tp.color=0xFFCCCCCC.toInt(); tp.textAlign=Paint.Align.LEFT
    }
}

class DischargeRateFragment : Fragment(), ShareableFragment {
    private var rootView: View?=null; private var chart: DischargeScatterChart?=null
    private var tvStats: TextView?=null; private var startPct=-1; private var startTime=0L
    private val handler=Handler(Looper.getMainLooper())
    private val receiver=object:BroadcastReceiver(){
        override fun onReceive(ctx: Context?,intent:Intent?){
            val level=intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,-1)?:return
            val scale=intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE,100)
            val pct=if(level>=0&&scale>0)level*100/scale else return
            val status=intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS,-1)
            val discharging=status==android.os.BatteryManager.BATTERY_STATUS_DISCHARGING
            if(discharging){
                if(startPct<0){startPct=pct;startTime=System.currentTimeMillis()}
                val minElapsed=(System.currentTimeMillis()-startTime)/60_000f
                chart?.add(minElapsed,pct)
                val dropped=startPct-pct
                val rateHr=if(minElapsed>0)(dropped/minElapsed)*60 else 0f
                tvStats?.text="Started at $startPct%  →  Now $pct%  (dropped $dropped%)\nElapsed: ${"%.0f".format(minElapsed)} min\nRate: ${"%.1f".format(rateHr)}%/hr\nEst. remaining: ${"%.0f".format(if(rateHr>0)pct/rateHr else 0f)} hrs"
            } else { startPct=-1; startTime=0 }
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val scroll=ScrollView(requireContext()); val root=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;val p=dp(12);setPadding(p,p,p,p)}
        scroll.addView(root); rootView=scroll
        root.addView(TextView(requireContext()).apply{text="🔋 Discharge Rate Analyzer";textSize=16f;setTypeface(null,android.graphics.Typeface.BOLD);setPadding(0,0,0,dp(6))})
        chart=DischargeScatterChart(requireContext()).apply{layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(220))}
        root.addView(chart)
        tvStats=TextView(requireContext()).apply{textSize=12f;setPadding(0,dp(8),0,0);setTextColor(0xFFCCCCCC.toInt())}
        root.addView(tvStats)
        val btnClear=Button(requireContext()).apply{text="Clear";setOnClickListener{chart?.clear();startPct=-1;startTime=0}}
        root.addView(btnClear)
        return scroll
    }
    override fun onResume(){super.onResume();requireContext().registerReceiver(receiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED))}
    override fun onPause(){super.onPause();try{requireContext().unregisterReceiver(receiver)}catch(_:Exception){}}
    override fun onDestroyView(){super.onDestroyView();rootView=null;chart=null}
    override fun getShareText()="Battery Discharge Rate\n${tvStats?.text}\nGenerated by CPU-A"
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
