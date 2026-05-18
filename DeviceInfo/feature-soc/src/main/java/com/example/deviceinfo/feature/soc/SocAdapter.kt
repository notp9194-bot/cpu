package com.cpua.deviceinfo.feature.soc

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cpua.deviceinfo.core.model.InfoItem
import com.cpua.deviceinfo.feature.soc.databinding.ItemSocBinding

class SocAdapter(private val items: List<InfoItem>) : RecyclerView.Adapter<SocAdapter.ViewHolder>() {
    inner class ViewHolder(val b: ItemSocBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSocBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.b.tvLabel.text = item.label
        holder.b.tvValue.text = item.value
        holder.b.tvValue.setTextColor(Color.parseColor("#9B59B6"))
    }
    override fun getItemCount() = items.size
}
