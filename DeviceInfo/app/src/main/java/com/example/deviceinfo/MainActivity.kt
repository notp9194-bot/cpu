package com.example.deviceinfo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.deviceinfo.databinding.ActivityMainBinding
import com.example.deviceinfo.feature.soc.SocFragment
import com.example.deviceinfo.feature.device.DeviceFragment
import com.example.deviceinfo.feature.system.SystemFragment
import com.example.deviceinfo.feature.battery.BatteryFragment
import com.example.deviceinfo.feature.thermal.ThermalFragment
import com.example.deviceinfo.feature.sensors.SensorsFragment
import com.example.deviceinfo.feature.about.AboutFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tabs = listOf("SOC","DEVICE","SYSTEM","BATTERY","THERMAL","SENSORS","ABOUT")

    override fun onCreate(savedInstanceState: Bundle?) {
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
    }

    private inner class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = tabs.size
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> SocFragment(); 1 -> DeviceFragment(); 2 -> SystemFragment()
            3 -> BatteryFragment(); 4 -> ThermalFragment(); 5 -> SensorsFragment()
            6 -> AboutFragment(); else -> SocFragment()
        }
    }
}
