package com.example.deviceinfo.feature.thermal

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.feature.thermal.databinding.ItemThermalBinding

data class ThermalItem(val zone: String, val tempC: Float)

class ThermalAdapter(private val items: List<ThermalItem>) :
    RecyclerView.Adapter<ThermalAdapter.VH>() {

    inner class VH(val b: ItemThermalBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemThermalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = items[pos]
        holder.b.tvLabel.text = item.zone
        holder.b.tvValue.text = String.format("%.1f °C", item.tempC)

        // Color coding based on temperature:
        // < 40°C → green (safe)
        // 40–60°C → orange (warm)
        // > 60°C → red (hot / warning)
        val color = when {
            item.tempC >= 60f -> Color.parseColor("#D32F2F")  // Red
            item.tempC >= 40f -> Color.parseColor("#F57C00")  // Orange
            else              -> Color.parseColor("#388E3C")  // Green
        }
        holder.b.tvValue.setTextColor(color)

        // Show warning emoji for hot zones
        val suffix = when {
            item.tempC >= 60f -> "  🔴"
            item.tempC >= 40f -> "  🟡"
            else              -> ""
        }
        holder.b.tvValue.text = String.format("%.1f °C%s", item.tempC, suffix)
    }
}
