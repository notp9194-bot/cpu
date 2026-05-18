package com.cpua.deviceinfo

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Alert Thresholds Settings Screen.
 *
 * Programmatic layout (no extra XML needed).
 * All sliders use standard SeekBar — no library. Play Store safe ✅
 */
class AlertSettingsActivity : AppCompatActivity() {

    // Battery Low
    private lateinit var swBattLow: Switch
    private lateinit var sbBattLow: SeekBar
    private lateinit var tvBattLowVal: TextView

    // Battery Full
    private lateinit var swBattFull: Switch

    // RAM
    private lateinit var swRam: Switch
    private lateinit var sbRam: SeekBar
    private lateinit var tvRamVal: TextView

    // Temperature
    private lateinit var swTemp: Switch
    private lateinit var sbTemp: SeekBar
    private lateinit var tvTempVal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Match app theme
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("dark_mode", false)) {
            setTheme(android.R.style.Theme_Material_NoActionBar)
        }
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val dp16 = dp(16)
            setPadding(dp16, dp16, dp16, dp16)
        }
        scroll.addView(root)
        setContentView(scroll)

        supportActionBar?.apply {
            title = "Alert Thresholds"
            setDisplayHomeAsUpEnabled(true)
        }

        // ── Load current values ───────────────────────────────────────
        val battLowEnabled  = AlertPrefs.isBattLowEnabled(this)
        val battLowPct      = AlertPrefs.getBattLowPct(this)
        val battFullEnabled = AlertPrefs.isBattFullEnabled(this)
        val ramEnabled      = AlertPrefs.isRamEnabled(this)
        val ramPct          = AlertPrefs.getRamPct(this)
        val tempEnabled     = AlertPrefs.isTempEnabled(this)
        val tempC           = AlertPrefs.getTempC(this)

        // ── Section: Battery ──────────────────────────────────────────
        root.addView(sectionHeader("🔋 Battery Alerts"))

        // Battery Low
        val (rowBattLow, swBL, sbBL, tvBL) = sliderRow(
            label     = "Low Battery Alert",
            sublabel  = "Notify when battery drops below threshold",
            min       = 5, max = 50, default = battLowPct,
            unit      = "%",
            enabled   = battLowEnabled
        )
        swBattLow   = swBL; sbBattLow = sbBL; tvBattLowVal = tvBL
        root.addView(rowBattLow)
        root.addView(divider())

        // Battery Full
        val rowFull = switchOnlyRow(
            label    = "Battery Full Alert (100%)",
            sublabel = "Notify when battery is fully charged",
            enabled  = battFullEnabled
        )
        swBattFull = rowFull.second
        root.addView(rowFull.first)
        root.addView(divider())

        // ── Section: RAM ──────────────────────────────────────────────
        root.addView(sectionHeader("💾 RAM Alert"))

        val (rowRam, swR, sbR, tvR) = sliderRow(
            label    = "High RAM Alert",
            sublabel = "Notify when RAM usage exceeds threshold",
            min      = 50, max = 99, default = ramPct,
            unit     = "%",
            enabled  = ramEnabled
        )
        swRam = swR; sbRam = sbR; tvRamVal = tvR
        root.addView(rowRam)
        root.addView(divider())

        // ── Section: Temperature ──────────────────────────────────────
        root.addView(sectionHeader("🌡 Temperature Alert"))

        val (rowTemp, swT, sbT, tvT) = sliderRow(
            label    = "High Temperature Alert",
            sublabel = "Notify when device temp exceeds threshold",
            min      = 35, max = 80, default = tempC,
            unit     = "°C",
            enabled  = tempEnabled
        )
        swTemp = swT; sbTemp = sbT; tvTempVal = tvT
        root.addView(rowTemp)

        // ── Save button ───────────────────────────────────────────────
        root.addView(space(24))
        val btnSave = Button(this).apply {
            text = "Save Settings"
            textSize = 15f
            setPadding(dp(24), dp(14), dp(24), dp(14))
            setOnClickListener { saveAndFinish() }
        }
        root.addView(btnSave, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        root.addView(space(8))
        val btnReset = Button(this).apply {
            text = "Reset to Defaults"
            textSize = 13f
            setOnClickListener { resetToDefaults() }
        }
        root.addView(btnReset, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    // ── Save ─────────────────────────────────────────────────────────
    private fun saveAndFinish() {
        AlertPrefs.save(
            ctx             = this,
            battLowEnabled  = swBattLow.isChecked,
            battLowPct      = sbBattLow.progress + 5,      // offset: min=5
            battFullEnabled = swBattFull.isChecked,
            ramEnabled      = swRam.isChecked,
            ramPct          = sbRam.progress + 50,         // offset: min=50
            tempEnabled     = swTemp.isChecked,
            tempC           = sbTemp.progress + 35         // offset: min=35
        )
        Toast.makeText(this, "✅ Alert settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun resetToDefaults() {
        swBattLow.isChecked  = true
        sbBattLow.progress   = AlertPrefs.DEF_BATT_LOW_PCT - 5
        tvBattLowVal.text    = "${AlertPrefs.DEF_BATT_LOW_PCT}%"
        swBattFull.isChecked = true
        swRam.isChecked      = true
        sbRam.progress       = AlertPrefs.DEF_RAM_PCT - 50
        tvRamVal.text        = "${AlertPrefs.DEF_RAM_PCT}%"
        swTemp.isChecked     = true
        sbTemp.progress      = AlertPrefs.DEF_TEMP_C - 35
        tvTempVal.text       = "${AlertPrefs.DEF_TEMP_C}°C"
        Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show()
    }

    // ── UI Builders ───────────────────────────────────────────────────

    private data class SliderRow(
        val container: View,
        val switch: Switch,
        val seekBar: SeekBar,
        val valueLabel: TextView
    )

    private fun sliderRow(
        label: String, sublabel: String,
        min: Int, max: Int, default: Int,
        unit: String, enabled: Boolean
    ): SliderRow {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(4), 0, dp(4))
        }

        // Top row: label + switch
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val labelCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvLabel = TextView(this).apply {
            text = label; textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val tvSub = TextView(this).apply {
            text = sublabel; textSize = 12f; alpha = 0.6f
        }
        labelCol.addView(tvLabel); labelCol.addView(tvSub)

        val sw = Switch(this).apply { isChecked = enabled }
        topRow.addView(labelCol); topRow.addView(sw)
        card.addView(topRow)

        // Slider row
        val sliderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }
        val sb = SeekBar(this).apply {
            this.max = max - min
            progress = (default - min).coerceIn(0, this.max)
            isEnabled = enabled
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvVal = TextView(this).apply {
            text = "$default$unit"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            minWidth = dp(52)
            gravity = Gravity.END
        }

        // Live update label as slider moves
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                tvVal.text = "${p + min}$unit"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?)  {}
        })

        // Toggle slider enabled state with switch
        sw.setOnCheckedChangeListener { _, checked -> sb.isEnabled = checked }

        sliderRow.addView(sb); sliderRow.addView(space(8)); sliderRow.addView(tvVal)
        card.addView(sliderRow)

        return SliderRow(card, sw, sb, tvVal)
    }

    private fun switchOnlyRow(label: String, sublabel: String, enabled: Boolean): Pair<View, Switch> {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(4), 0, dp(4))
        }
        val labelCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvLabel = TextView(this).apply {
            text = label; textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val tvSub = TextView(this).apply {
            text = sublabel; textSize = 12f; alpha = 0.6f
        }
        labelCol.addView(tvLabel); labelCol.addView(tvSub)
        val sw = Switch(this).apply { isChecked = enabled }
        card.addView(labelCol); card.addView(sw)
        return Pair(card, sw)
    }

    private fun sectionHeader(title: String): TextView = TextView(this).apply {
        text = title; textSize = 13f; alpha = 0.55f
        setTypeface(null, android.graphics.Typeface.BOLD)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(dp(4), dp(20), 0, dp(6))
        layoutParams = lp
    }

    private fun divider(): View = View(this).apply {
        setBackgroundColor(0x18000000)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ).also { it.setMargins(0, dp(4), 0, dp(4)) }
    }

    private fun space(dpVal: Int): Space = Space(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(dpVal))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
