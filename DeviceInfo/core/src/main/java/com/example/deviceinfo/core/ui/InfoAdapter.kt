package com.example.deviceinfo.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.core.databinding.ItemInfoBinding
import com.example.deviceinfo.core.model.InfoItem

class InfoAdapter(private val allItems: List<InfoItem>) : RecyclerView.Adapter<InfoAdapter.VH>() {

    private var filteredItems: List<InfoItem> = allItems

    inner class VH(val b: ItemInfoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = filteredItems[pos]

        // Section header row (isHighlighted + no value)
        if (item.isHighlighted && item.value.isEmpty()) {
            holder.b.tvLabel.visibility = View.VISIBLE
            holder.b.tvValue.visibility = View.GONE
            holder.b.btnCopy.visibility = View.GONE
            holder.b.tvLabel.text = item.label
            holder.b.tvLabel.setTextColor(0xFF00E5FF.toInt())
            holder.b.tvLabel.textSize = 11f
            holder.b.tvLabel.letterSpacing = 0.08f
            holder.b.cardRoot.setCardBackgroundColor(0xFF0A0E1A.toInt())
            holder.b.cardRoot.strokeWidth = 0
            return
        }

        // Spacer row
        if (item.label.isEmpty() && item.value.isEmpty()) {
            holder.b.tvLabel.visibility = View.INVISIBLE
            holder.b.tvValue.visibility = View.INVISIBLE
            holder.b.btnCopy.visibility = View.INVISIBLE
            holder.b.cardRoot.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
            return
        }

        holder.b.tvLabel.visibility = View.VISIBLE
        holder.b.tvValue.visibility = View.VISIBLE
        holder.b.btnCopy.visibility = View.VISIBLE
        holder.b.tvLabel.letterSpacing = 0.01f
        holder.b.tvLabel.textSize = 13f

        holder.b.tvLabel.text = item.label
        holder.b.tvValue.text = item.value

        if (item.isHighlighted) {
            // Highlighted data row — subtle cyan tint card
            holder.b.cardRoot.setCardBackgroundColor(0x1200E5FF.toInt())
            holder.b.cardRoot.strokeWidth = 1
            holder.b.cardRoot.strokeColor = 0x2200E5FF.toInt()
        } else {
            holder.b.cardRoot.setCardBackgroundColor(0x08AABBCC.toInt())
            holder.b.cardRoot.strokeWidth = 0
        }

        holder.b.tvLabel.setTextColor(0xFF8B9AB5.toInt())
        holder.b.tvValue.setTextColor(0xFF00E5FF.toInt())

        holder.b.btnCopy.setOnClickListener {
            val ctx = holder.itemView.context
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(item.label, "${item.label}: ${item.value}"))
            Toast.makeText(ctx, "Copied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = filteredItems.size

    fun filter(query: String) {
        filteredItems = if (query.isBlank()) {
            allItems
        } else {
            val q = query.trim().lowercase()
            allItems.filter {
                it.label.lowercase().contains(q) || it.value.lowercase().contains(q)
            }
        }
        notifyDataSetChanged()
    }
}
