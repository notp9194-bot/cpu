package com.cpua.deviceinfo

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cpua.deviceinfo.core.ui.ShareableFragment
import com.cpua.deviceinfo.core.util.ExportUtils
import com.cpua.deviceinfo.databinding.ActivityMainBinding
import com.cpua.deviceinfo.feature.about.AboutFragment
import com.cpua.deviceinfo.feature.audio.AudioFragment
import com.cpua.deviceinfo.feature.battery.BatteryFragment
import com.cpua.deviceinfo.feature.benchmark.BenchmarkFragment
import com.cpua.deviceinfo.feature.security.BuildFragment
import com.cpua.deviceinfo.feature.camera.CameraFragment
import com.cpua.deviceinfo.feature.codec.CodecFragment
import com.cpua.deviceinfo.feature.connectivity.ConnectivityFragment
import com.cpua.deviceinfo.feature.device.DeviceFragment
import com.cpua.deviceinfo.feature.display.DisplayFragment
import com.cpua.deviceinfo.feature.favorites.FavoritesFragment
import com.cpua.deviceinfo.feature.health.HealthFragment
import com.cpua.deviceinfo.feature.gpu.GpuFragment
import com.cpua.deviceinfo.feature.input.InputFragment
import com.cpua.deviceinfo.feature.memory.MemoryFragment
import com.cpua.deviceinfo.feature.network.NetworkFragment
import com.cpua.deviceinfo.feature.power.PowerFragment
import com.cpua.deviceinfo.feature.sensors.SensorsFragment
import com.cpua.deviceinfo.feature.soc.SocFragment
import com.cpua.deviceinfo.feature.system.SystemFragment
import com.cpua.deviceinfo.feature.thermal.ThermalFragment
import com.cpua.deviceinfo.feature.cpucore.CpuCoreFragment
import com.cpua.deviceinfo.feature.chargesession.ChargeSessionFragment
import com.cpua.deviceinfo.feature.pingmonitor.PingMonitorFragment
import com.cpua.deviceinfo.feature.screentime.ScreenTimeFragment
import com.cpua.deviceinfo.feature.dischargerate.DischargeRateFragment
import com.cpua.deviceinfo.feature.cpuheatmap.CpuHeatmapFragment
import com.cpua.deviceinfo.feature.throttle.ThrottleFragment
import com.cpua.deviceinfo.feature.loadavg.LoadAvgFragment
import com.cpua.deviceinfo.feature.chargecurve.ChargeCurveFragment
import com.cpua.deviceinfo.feature.sensorlive.SensorLiveFragment
import com.cpua.deviceinfo.feature.devcompare.DeviceCompareFragment
import com.cpua.deviceinfo.feature.powerestimate.PowerEstimateFragment
import com.cpua.deviceinfo.feature.brightness.BrightnessFragment
import com.cpua.deviceinfo.feature.bootspeed.BootSpeedFragment
import com.cpua.deviceinfo.feature.featurematrix.FeatureMatrixFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private val tabs = listOf(
        "★ FAV", "SOC", "DEVICE", "SYSTEM", "BATTERY", "THERMAL",
        "SENSORS", "NETWORK", "CAMERA", "AUDIO", "DISPLAY", "CODEC",
        "BENCHMARK", "CONNECTIVITY", "POWER", "GPU", "MEMORY", "BUILD",
        "INPUT", "HEALTH", "ABOUT",
        "CPU CORES", "CHG SESSION", "PING", "SCREEN TIME", "DISCHARGE",
        "CPU HEATMAP", "THROTTLE", "LOAD AVG", "CHG CURVE", "SENSOR LIVE",
        "DEV COMPARE", "PWR ESTIMATE", "BRIGHTNESS", "BOOT SPEED", "FEAT MATRIX"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.statusBarSpacer.layoutParams =
                binding.statusBarSpacer.layoutParams.also { it.height = sb.top }
            binding.viewPager.setPadding(0, 0, 0, sb.bottom)
            binding.viewPager.clipToPadding = false
            insets
        }

        binding.viewPager.adapter = MainPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = tabs[pos]
        }.attach()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val isDark = prefs.getBoolean("dark_mode", false)
        menu.findItem(R.id.action_theme)?.setIcon(
            if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_theme -> {
            val newDark = !prefs.getBoolean("dark_mode", false)
            prefs.edit().putBoolean("dark_mode", newDark).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (newDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate(); true
        }
        R.id.action_share      -> { shareCurrentTab(); true }
        R.id.action_export_pdf -> { exportCurrentTabPdf(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun shareCurrentTab() {
        val pos  = binding.viewPager.currentItem
        val frag = supportFragmentManager.findFragmentByTag("f$pos")
        val text = (frag as? ShareableFragment)?.getShareText()
            ?: "\uD83D\uDCF1 Device Info — ${tabs[pos]}\nChecked with CPU-A Device Info app."
        startActivity(
            android.content.Intent.createChooser(
                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Device Info — ${tabs[pos]}")
                }, "Share via"
            )
        )
    }

    private fun exportCurrentTabPdf() {
        val pos  = binding.viewPager.currentItem
        val frag = supportFragmentManager.findFragmentByTag("f$pos")
        val data = (frag as? ShareableFragment)?.getExportData() ?: emptyMap()
        ExportUtils.exportToPdf(this, tabs[pos], data)
    }

    private inner class MainPagerAdapter(a: AppCompatActivity) : FragmentStateAdapter(a) {
        override fun getItemCount() = tabs.size
        override fun createFragment(pos: Int): Fragment = when (pos) {
            0  -> FavoritesFragment()
            1  -> SocFragment()
            2  -> DeviceFragment()
            3  -> SystemFragment()
            4  -> BatteryFragment()
            5  -> ThermalFragment()
            6  -> SensorsFragment()
            7  -> NetworkFragment()
            8  -> CameraFragment()
            9  -> AudioFragment()
            10 -> DisplayFragment()
            11 -> CodecFragment()
            12 -> BenchmarkFragment()
            13 -> ConnectivityFragment()
            14 -> PowerFragment()
            15 -> GpuFragment()
            16 -> MemoryFragment()
            17 -> BuildFragment()
            18 -> InputFragment()
            19 -> HealthFragment()
            20 -> AboutFragment()
            21 -> CpuCoreFragment()
            22 -> ChargeSessionFragment()
            23 -> PingMonitorFragment()
            24 -> ScreenTimeFragment()
            25 -> DischargeRateFragment()
            26 -> CpuHeatmapFragment()
            27 -> ThrottleFragment()
            28 -> LoadAvgFragment()
            29 -> ChargeCurveFragment()
            30 -> SensorLiveFragment()
            31 -> DeviceCompareFragment()
            32 -> PowerEstimateFragment()
            33 -> BrightnessFragment()
            34 -> BootSpeedFragment()
            35 -> FeatureMatrixFragment()
            else -> SocFragment()
        }
    }

    companion object { private const val REQ_NOTIF = 100 }
}
