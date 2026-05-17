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

class InfoAdapter(private val items: List<InfoItem>) : RecyclerView.Adapter<InfoAdapter.VH>() {
    inner class VH(val b: ItemInfoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = items[pos]

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

        // Highlighted rows get a subtle tinted background
        if (item.isHighlighted) {
            holder.b.root.setBackgroundColor(0x0F7B2FBE)  // 6% purple tint
        } else {
            holder.b.root.setBackgroundResource(android.R.color.transparent)
        }

        // BUG FIX #5 (copy): Copy individual row — already in UI, now properly wired
        holder.b.btnCopy.setOnClickListener {
            val ctx = holder.itemView.context
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(item.label, "${item.label}: ${item.value}"))
            Toast.makeText(ctx, "Copied: ${item.label}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = items.size
}
