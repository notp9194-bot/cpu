package com.example.deviceinfo.feature.device

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.feature.device.databinding.ItemDeviceBinding

class DeviceAdapter(private val items: List<InfoItem>) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
    inner class ViewHolder(val b: ItemDeviceBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.b.tvLabel.text = item.label
        holder.b.tvValue.text = item.value
        holder.b.tvValue.setTextColor(Color.parseColor("#9B59B6"))
    }
    override fun getItemCount() = items.size
}
