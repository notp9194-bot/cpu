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

        // Reset visibility defaults
        holder.b.tvLabel.visibility = View.VISIBLE
        holder.b.tvValue.visibility = View.VISIBLE
        holder.b.btnCopy.visibility = View.VISIBLE
        holder.b.accentBar.visibility = View.GONE
        holder.b.tvLabel.letterSpacing = 0.01f
        holder.b.tvLabel.textSize = 13f

        // Section header row (isHighlighted + no value)
        if (item.isHighlighted && item.value.isEmpty()) {
            holder.b.tvValue.visibility = View.GONE
            holder.b.btnCopy.visibility = View.GONE
            holder.b.tvLabel.text = item.label
            holder.b.tvLabel.setTextColor(0xFF00E5FF.toInt())
            holder.b.tvLabel.textSize = 10f
            holder.b.tvLabel.letterSpacing = 0.10f
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
            holder.b.cardRoot.strokeWidth = 0
            return
        }

        holder.b.tvLabel.text = item.label
        holder.b.tvValue.text = item.value
        holder.b.tvLabel.setTextColor(0xFF8B9AB5.toInt())
        holder.b.tvValue.setTextColor(0xFF00E5FF.toInt())

        if (item.isHighlighted) {
            // Highlighted data row — cyan tinted card with accent bar
            holder.b.cardRoot.setCardBackgroundColor(0x0F00E5FF.toInt())
            holder.b.cardRoot.strokeWidth = 1
            holder.b.cardRoot.strokeColor = 0x2000E5FF.toInt()
            holder.b.accentBar.visibility = View.VISIBLE
            holder.b.accentBar.setBackgroundColor(0x6600E5FF.toInt())
        } else {
            holder.b.cardRoot.setCardBackgroundColor(0x09AABBCC.toInt())
            holder.b.cardRoot.strokeWidth = 0
        }

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
