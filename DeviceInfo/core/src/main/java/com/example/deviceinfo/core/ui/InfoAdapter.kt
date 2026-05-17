package com.example.deviceinfo.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
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

        // Spacer row
        if (item.label.isEmpty() && item.value.isEmpty()) {
            holder.b.tvLabel.visibility = View.INVISIBLE
            holder.b.tvValue.visibility = View.INVISIBLE
            holder.b.btnCopy.visibility = View.INVISIBLE
            holder.b.root.setBackgroundColor(Color.TRANSPARENT)
            return
        }

        holder.b.tvLabel.visibility = View.VISIBLE
        holder.b.tvValue.visibility = View.VISIBLE
        holder.b.btnCopy.visibility = View.VISIBLE

        holder.b.tvLabel.text = item.label
        holder.b.tvValue.text = item.value

        if (item.isHighlighted) {
            holder.b.root.setBackgroundColor(0x0F7B2FBE)
        } else {
            holder.b.root.setBackgroundResource(android.R.color.transparent)
        }

        holder.b.btnCopy.setOnClickListener {
            val ctx = holder.itemView.context
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(item.label, "${item.label}: ${item.value}"))
            Toast.makeText(ctx, "Copied: ${item.label}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = filteredItems.size

    /** Filter rows where label OR value contains [query] (case-insensitive). */
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
