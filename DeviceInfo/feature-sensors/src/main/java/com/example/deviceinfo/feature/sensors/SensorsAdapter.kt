package com.example.deviceinfo.feature.sensors

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.feature.sensors.databinding.ItemSensorBinding

class SensorsAdapter(private val items: MutableList<InfoItem>) :
    RecyclerView.Adapter<SensorsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSensorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSensorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvSensorName.text = item.label
        holder.binding.tvSensorType.text = item.value
    }

    override fun getItemCount(): Int = items.size
}
