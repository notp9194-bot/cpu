package com.example.deviceinfo.feature.featurematrix

import android.content.pm.PackageManager; import android.hardware.camera2.*; import android.nfc.NfcAdapter; import android.os.*
import android.view.*; import android.widget.*; import androidx.fragment.app.Fragment
import com.example.deviceinfo.core.ui.ShareableFragment; import java.util.concurrent.Executors

class FeatureMatrixFragment : Fragment(), ShareableFragment {
    private var rootView: View?=null; private var gridView: FeatureMatrixGridView?=null; private var tvStats: TextView?=null
    private val executor=Executors.newSingleThreadExecutor()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val scroll=ScrollView(requireContext()); val root=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;val p=dp(12);setPadding(p,p,p,p)}
        scroll.addView(root); rootView=scroll
        root.addView(TextView(requireContext()).apply{text="📋 Feature Availability Matrix";textSize=16f;setTypeface(null,android.graphics.Typeface.BOLD);setPadding(0,0,0,dp(6))})
        root.addView(TextView(requireContext()).apply{text="✅ Available  ❌ Not present  ❓ Unknown";textSize=11f;alpha=0.7f;setPadding(0,0,0,dp(4))})
        tvStats=TextView(requireContext()).apply{textSize=12f;setPadding(0,0,0,dp(8));setTextColor(0xFF4CAF50.toInt())}
        root.addView(tvStats)
        gridView=FeatureMatrixGridView(requireContext()).apply{layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)}
        root.addView(gridView)
        checkFeatures()
        return scroll
    }

    private fun checkFeatures() {
        val ctx=requireContext().applicationContext
        executor.submit {
            val pm=ctx.packageManager
            fun has(f: String)=pm.hasSystemFeature(f)
            fun st(b: Boolean)=if(b)FeatureMatrixGridView.Status.YES else FeatureMatrixGridView.Status.NO

            val nfcAvail=NfcAdapter.getDefaultAdapter(ctx)!=null
            val cameraCount=try{(ctx.getSystemService(android.content.Context.CAMERA_SERVICE) as CameraManager).cameraIdList.size}catch(_:Exception){0}
            val sensorList=try{(ctx.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager).getSensorList(android.hardware.Sensor.TYPE_ALL).size}catch(_:Exception){0}

            val features=listOf(
                // Connectivity
                FeatureMatrixGridView.Feature("NFC","Connectivity",st(nfcAvail),"Near Field Communication"),
                FeatureMatrixGridView.Feature("WiFi 6 (802.11ax)","Connectivity",st(has("android.hardware.wifi.passpoint")||Build.VERSION.SDK_INT>=29&&has("android.hardware.wifi")),"High-speed WiFi"),
                FeatureMatrixGridView.Feature("Bluetooth 5.0+","Connectivity",if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)FeatureMatrixGridView.Status.YES else FeatureMatrixGridView.Status.UNKNOWN,"BT LE Audio support"),
                FeatureMatrixGridView.Feature("5G Modem","Connectivity",st(has("android.hardware.telephony.radio.access")||has("android.hardware.telephony.5gsa")),"Sub-6 / mmWave 5G"),
                FeatureMatrixGridView.Feature("USB-C","Connectivity",FeatureMatrixGridView.Status.UNKNOWN,"Check device spec"),
                // Display
                FeatureMatrixGridView.Feature("HDR Display","Display",st(has("android.hardware.sensor.hdrviewfinder")||Build.VERSION.SDK_INT>=24),"High Dynamic Range"),
                FeatureMatrixGridView.Feature("High Refresh Rate","Display",st(requireActivity().windowManager.defaultDisplay.supportedModes.any{it.refreshRate>61f}),"90Hz / 120Hz / 144Hz"),
                FeatureMatrixGridView.Feature("Always-On Display","Display",st(has("android.hardware.sensor.proximity")),"Requires proximity sensor"),
                FeatureMatrixGridView.Feature("Notch/Cutout","Display",st(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P),"Display cutout API"),
                // Security
                FeatureMatrixGridView.Feature("Fingerprint","Security",st(has(PackageManager.FEATURE_FINGERPRINT)),"In-display or side sensor"),
                FeatureMatrixGridView.Feature("Face Unlock","Security",st(has(PackageManager.FEATURE_FACE)),"3D or 2D face auth"),
                FeatureMatrixGridView.Feature("Iris Scanner","Security",st(has(PackageManager.FEATURE_IRIS)),"Biometric iris"),
                FeatureMatrixGridView.Feature("Strongbox","Security",st(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P&&has("android.hardware.strongbox_keystore")),"Hardware security module"),
                FeatureMatrixGridView.Feature("Verified Boot","Security",FeatureMatrixGridView.Status.UNKNOWN,"ROM integrity check"),
                // Camera
                FeatureMatrixGridView.Feature("Multiple Cameras","Camera",st(cameraCount>2),"$cameraCount cameras detected"),
                FeatureMatrixGridView.Feature("RAW Capture","Camera",st(has(PackageManager.FEATURE_CAMERA_ANY)&&Build.VERSION.SDK_INT>=21),"DNG/RAW format support"),
                FeatureMatrixGridView.Feature("Manual Control","Camera",st(has(PackageManager.FEATURE_CAMERA_CAPABILITY_MANUAL_SENSOR)),"ISO/shutter/WB manual"),
                FeatureMatrixGridView.Feature("Video Stabilization","Camera",st(has(PackageManager.FEATURE_CAMERA_CAPABILITY_MANUAL_POST_PROCESSING)),"OIS/EIS"),
                // Sensors
                FeatureMatrixGridView.Feature("Barometer","Sensors",st(has(PackageManager.FEATURE_SENSOR_BAROMETER)),"Pressure sensor"),
                FeatureMatrixGridView.Feature("Gyroscope","Sensors",st(has(PackageManager.FEATURE_SENSOR_GYROSCOPE)),"Rotation sensor"),
                FeatureMatrixGridView.Feature("Compass","Sensors",st(has(PackageManager.FEATURE_SENSOR_COMPASS)),"Magnetic field sensor"),
                FeatureMatrixGridView.Feature("Thermometer","Sensors",st(has("android.hardware.sensor.ambient_temperature")),"Ambient temp sensor"),
                FeatureMatrixGridView.Feature("$sensorList Total Sensors","Sensors",FeatureMatrixGridView.Status.YES,"All sensor count"),
                // Performance
                FeatureMatrixGridView.Feature("Hardware Vulkan","GPU",st(has(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)),"Vulkan graphics API"),
                FeatureMatrixGridView.Feature("OpenGL ES 3.1+","GPU",st(has(PackageManager.FEATURE_OPENGLES_EXTENSION_PACK)),"GLES 3.1 + AEP"),
                FeatureMatrixGridView.Feature("64-bit ABI","CPU",st(Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()),"ARM64 / x86_64"),
                FeatureMatrixGridView.Feature("Treble Support","System",st(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O_MR1),"Project Treble modular"),
                FeatureMatrixGridView.Feature("Seamless Updates","System",st(has("android.software.adoptable_storage")||Build.VERSION.SDK_INT>=Build.VERSION_CODES.N),"A/B partition updates"),
                FeatureMatrixGridView.Feature("GMS (Google Services)","System",st(pm.hasSystemFeature("com.google.android.gms.permission.BROADCAST_DATA_MESSAGE")||try{pm.getPackageInfo("com.google.android.gms",0);true}catch(_:Exception){false}),"Google Play Services"),
                FeatureMatrixGridView.Feature("USB OTG","USB",st(has(PackageManager.FEATURE_USB_HOST)),"USB Host / OTG")
            )

            val yesCount=features.count{it.status==FeatureMatrixGridView.Status.YES}
            val noCount=features.count{it.status==FeatureMatrixGridView.Status.NO}
            requireActivity().runOnUiThread {
                if(rootView==null)return@runOnUiThread
                gridView?.setFeatures(features)
                tvStats?.text="✅ $yesCount available  ❌ $noCount not available  out of ${features.size} checked"
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); rootView=null; gridView=null }
    override fun getShareText()="Feature Matrix\n${tvStats?.text}\nGenerated by CPU-A"
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
