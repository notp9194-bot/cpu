package com.example.deviceinfo

import android.Manifest
import android.content.Intent
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
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.core.util.ExportUtils
import com.example.deviceinfo.databinding.ActivityMainBinding
import com.example.deviceinfo.feature.about.AboutFragment
import com.example.deviceinfo.feature.audio.AudioFragment
import com.example.deviceinfo.feature.battery.BatteryFragment
import com.example.deviceinfo.feature.benchmark.BenchmarkFragment
import com.example.deviceinfo.feature.security.BuildFragment
import com.example.deviceinfo.feature.camera.CameraFragment
import com.example.deviceinfo.feature.codec.CodecFragment
import com.example.deviceinfo.feature.connectivity.ConnectivityFragment
import com.example.deviceinfo.feature.device.DeviceFragment
import com.example.deviceinfo.feature.display.DisplayFragment
import com.example.deviceinfo.feature.favorites.FavoritesFragment
import com.example.deviceinfo.feature.health.HealthFragment
import com.example.deviceinfo.feature.gpu.GpuFragment
import com.example.deviceinfo.feature.input.InputFragment
import com.example.deviceinfo.feature.memory.MemoryFragment
import com.example.deviceinfo.feature.network.NetworkFragment
import com.example.deviceinfo.feature.power.PowerFragment
import com.example.deviceinfo.feature.sensors.SensorsFragment
import com.example.deviceinfo.feature.soc.SocFragment
import com.example.deviceinfo.feature.system.SystemFragment
import com.example.deviceinfo.feature.thermal.ThermalFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    // ── Tab order (21 tabs — FAVORITES at 0, HEALTH at 19) ────────────
    private val tabs = listOf(
        "★ FAV",        // 0  ← NEW
        "SOC",          // 1
        "DEVICE",       // 2
        "SYSTEM",       // 3
        "BATTERY",      // 4
        "THERMAL",      // 5
        "SENSORS",      // 6
        "NETWORK",      // 7
        "CAMERA",       // 8
        "AUDIO",        // 9
        "DISPLAY",      // 10
        "CODEC",        // 11
        "BENCHMARK",    // 12
        "CONNECTIVITY", // 13
        "POWER",        // 14
        "GPU",          // 15
        "MEMORY",       // 16
        "BUILD",        // 17
        "INPUT",        // 18
        "HEALTH",       // 19
        "ABOUT"          // 20
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

        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter
        // FIX: Removed offscreenPageLimit = tabs.size — loading all 21 fragments at once
        // caused ANR/crash. Default lazy loading (1 neighbor) is correct behavior.
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = tabs[pos]
        }.attach()

        // Request POST_NOTIFICATIONS (Android 13+)
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
        val pos = binding.viewPager.currentItem
        val fragment = supportFragmentManager.findFragmentByTag("f$pos")
        val text = (fragment as? ShareableFragment)?.getShareText()
            ?: "\uD83D\uDCF1 Device Info — ${tabs[pos]}\nChecked with CPU-A Device Info app."
        startActivity(
            Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "Device Info — ${tabs[pos]}")
            }, "Share via")
        )
    }

    private fun exportCurrentTabPdf() {
        val pos = binding.viewPager.currentItem
        val fragment = supportFragmentManager.findFragmentByTag("f$pos")
        val data = (fragment as? ShareableFragment)?.getExportData() ?: emptyMap()
        ExportUtils.exportToPdf(this, tabs[pos], data)
    }

    // ── Pager adapter ─────────────────────────────────────────────────
    private inner class MainPagerAdapter(a: AppCompatActivity) : FragmentStateAdapter(a) {
        override fun getItemCount() = tabs.size
        override fun createFragment(pos: Int): Fragment = when (pos) {
            0  -> FavoritesFragment()    // ← NEW
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
            else -> SocFragment() // fallback
        }
    }

    companion object { private const val REQ_NOTIF = 100 }
}
