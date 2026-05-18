package com.cpua.deviceinfo.feature.sensors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cpua.deviceinfo.core.model.InfoItem
import com.cpua.deviceinfo.feature.sensors.databinding.ItemSensorBinding

class SensorsAdapter(private val items: List<InfoItem>) : RecyclerView.Adapter<SensorsAdapter.VH>() {
    inner class VH(val b: ItemSensorBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(ItemSensorBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) { h.b.tvSensorName.text = items[pos].label; h.b.tvSensorType.text = items[pos].value }
    override fun getItemCount() = items.size
}
