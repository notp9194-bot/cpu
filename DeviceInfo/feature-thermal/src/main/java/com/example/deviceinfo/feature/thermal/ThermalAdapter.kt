package com.example.deviceinfo.feature.thermal

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.feature.thermal.databinding.ItemThermalBinding

class ThermalAdapter(private val items: List<InfoItem>) :
    RecyclerView.Adapter<ThermalAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemThermalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemThermalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvLabel.text = item.label
        holder.binding.tvValue.text = item.value
        holder.binding.tvValue.setTextColor(Color.parseColor("#9B59B6"))
    }

    override fun getItemCount(): Int = items.size
}
