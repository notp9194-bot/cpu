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
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.deviceinfo.core.ui.ShareableFragment
import com.example.deviceinfo.databinding.ActivityMainBinding
import com.example.deviceinfo.feature.soc.SocFragment
import com.example.deviceinfo.feature.device.DeviceFragment
import com.example.deviceinfo.feature.system.SystemFragment
import com.example.deviceinfo.feature.battery.BatteryFragment
import com.example.deviceinfo.feature.thermal.ThermalFragment
import com.example.deviceinfo.feature.sensors.SensorsFragment
import com.example.deviceinfo.feature.about.AboutFragment
import com.example.deviceinfo.feature.network.NetworkFragment
import com.example.deviceinfo.feature.camera.CameraFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private val tabs = listOf("SOC", "DEVICE", "SYSTEM", "BATTERY", "THERMAL", "SENSORS", "NETWORK", "CAMERA", "ABOUT")

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = tabs.size
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIF
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                val isDark = prefs.getBoolean("dark_mode", false)
                val newDark = !isDark
                prefs.edit().putBoolean("dark_mode", newDark).apply()
                AppCompatDelegate.setDefaultNightMode(
                    if (newDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
                recreate()
                true
            }
            R.id.action_share -> {
                shareCurrentTab()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * FIXED: Ask the active fragment for its real data via ShareableFragment.
     * Falls back to a generic message only for tabs that don't implement it (e.g. About).
     */
    private fun shareCurrentTab() {
        val pos = binding.viewPager.currentItem
        val tabName = tabs[pos]

        // FragmentStateAdapter tags fragments as "f{itemId}" inside the ViewPager2 host
        val fragmentTag = "f$pos"
        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)

        val shareText = if (fragment is ShareableFragment) {
            fragment.getShareText()
        } else {
            "📱 Device Info — $tabName\nChecked with CPU-A Device Info app."
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Device Info — $tabName")
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private inner class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = tabs.size
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> SocFragment()
            1 -> DeviceFragment()
            2 -> SystemFragment()
            3 -> BatteryFragment()
            4 -> ThermalFragment()
            5 -> SensorsFragment()
            6 -> NetworkFragment()
            7 -> CameraFragment()
            8 -> AboutFragment()
            else -> SocFragment()
        }
    }

    companion object {
        private const val REQ_NOTIF = 100
    }
}
