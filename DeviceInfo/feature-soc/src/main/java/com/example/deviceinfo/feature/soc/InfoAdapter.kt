package com.example.deviceinfo.feature.soc

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.core.model.InfoItem
import com.example.deviceinfo.feature.soc.databinding.ItemInfoBinding

class InfoAdapter(private val items: List<InfoItem>) :
    RecyclerView.Adapter<InfoAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemInfoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvLabel.text = item.label
        holder.binding.tvValue.text = item.value
        val valueColor = if (item.isHighlighted) Color.parseColor("#9B59B6") else Color.parseColor("#9B59B6")
        holder.binding.tvValue.setTextColor(valueColor)
    }

    override fun getItemCount(): Int = items.size
}
