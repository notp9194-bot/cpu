package com.example.deviceinfo.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
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
        holder.b.tvLabel.text = item.label
        holder.b.tvValue.text = item.value

        if (item.label.isEmpty() && item.value.isEmpty()) {
            // Spacer
            holder.b.tvLabel.visibility = android.view.View.INVISIBLE
            holder.b.tvValue.visibility = android.view.View.INVISIBLE
            holder.b.btnCopy.visibility = android.view.View.INVISIBLE
            return
        }
        holder.b.tvLabel.visibility = android.view.View.VISIBLE
        holder.b.tvValue.visibility = android.view.View.VISIBLE
        holder.b.btnCopy.visibility = android.view.View.VISIBLE

        holder.b.btnCopy.setOnClickListener {
            val ctx = holder.itemView.context
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(item.label, "${item.label}: ${item.value}"))
            Toast.makeText(ctx, "Copied: ${item.label}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = items.size
}
