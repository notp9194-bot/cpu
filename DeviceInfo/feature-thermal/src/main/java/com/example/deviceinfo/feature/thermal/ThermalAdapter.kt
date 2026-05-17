package com.example.deviceinfo.feature.thermal

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.feature.thermal.databinding.ItemThermalBinding

data class ThermalItem(val zone: String, val tempC: Float)

class ThermalAdapter(private val allItems: List<ThermalItem>) :
    RecyclerView.Adapter<ThermalAdapter.VH>() {

    private var filteredItems: List<ThermalItem> = allItems

    inner class VH(val b: ItemThermalBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemThermalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = filteredItems.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = filteredItems[pos]
        holder.b.tvLabel.text = item.zone
        val color = when {
            item.tempC >= 60f -> Color.parseColor("#D32F2F")
            item.tempC >= 40f -> Color.parseColor("#F57C00")
            else              -> Color.parseColor("#388E3C")
        }
        holder.b.tvValue.setTextColor(color)
        val suffix = when {
            item.tempC >= 60f -> "  🔴"
            item.tempC >= 40f -> "  🟡"
            else              -> ""
        }
        holder.b.tvValue.text = String.format("%.1f °C%s", item.tempC, suffix)
    }

    fun filter(query: String) {
        filteredItems = if (query.isBlank()) allItems
        else allItems.filter { it.zone.lowercase().contains(query.trim().lowercase()) }
        notifyDataSetChanged()
    }
}
