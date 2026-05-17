package com.example.deviceinfo.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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
        val ctx  = holder.itemView.context

        // Resolve theme colors dynamically (light + dark safe)
        fun themeColor(attr: Int): Int {
            val tv = TypedValue()
            ctx.theme.resolveAttribute(attr, tv, true)
            return tv.data
        }
        val colorSurface    = themeColor(com.google.android.material.R.attr.colorSurface)
        val colorOnSurface  = themeColor(com.google.android.material.R.attr.colorOnSurface)
        val colorPrimary    = themeColor(com.google.android.material.R.attr.colorPrimary)
        val colorSecondary  = themeColor(android.R.attr.textColorSecondary)

        // Reset defaults
        holder.b.tvLabel.visibility  = View.VISIBLE
        holder.b.tvValue.visibility  = View.VISIBLE
        holder.b.btnCopy.visibility  = View.VISIBLE
        holder.b.accentBar.visibility = View.GONE
        holder.b.tvLabel.letterSpacing = 0.01f
        holder.b.tvLabel.textSize = 13f

        // ── Section header row (isHighlighted + empty value) ──────────
        if (item.isHighlighted && item.value.isEmpty()) {
            holder.b.tvValue.visibility  = View.GONE
            holder.b.btnCopy.visibility  = View.GONE
            holder.b.tvLabel.text = item.label
            holder.b.tvLabel.setTextColor(colorPrimary)
            holder.b.tvLabel.textSize = 10f
            holder.b.tvLabel.letterSpacing = 0.10f
            // Section header bg: dark in night, very light tint in day
            val isDark = isDarkTheme(ctx)
            holder.b.cardRoot.setCardBackgroundColor(
                if (isDark) 0xFF0A0E1A.toInt() else 0xFFF0F4FF.toInt()
            )
            holder.b.cardRoot.strokeWidth = 0
            return
        }

        // ── Spacer row ────────────────────────────────────────────────
        if (item.label.isEmpty() && item.value.isEmpty()) {
            holder.b.tvLabel.visibility  = View.INVISIBLE
            holder.b.tvValue.visibility  = View.INVISIBLE
            holder.b.btnCopy.visibility  = View.INVISIBLE
            holder.b.cardRoot.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
            holder.b.cardRoot.strokeWidth = 0
            return
        }

        // ── Normal / highlighted data row ─────────────────────────────
        holder.b.tvLabel.text  = item.label
        holder.b.tvValue.text  = item.value

        // Label: use theme secondary text — visible in both light & dark
        holder.b.tvLabel.setTextColor(colorSecondary)
        // Value: primary accent (cyan)
        holder.b.tvValue.setTextColor(colorPrimary)

        if (item.isHighlighted) {
            // Subtle tinted card
            holder.b.cardRoot.setCardBackgroundColor(blendAlpha(colorPrimary, 0x12))
            holder.b.cardRoot.strokeWidth = 1
            holder.b.cardRoot.strokeColor = blendAlpha(colorPrimary, 0x28)
            holder.b.accentBar.visibility = View.VISIBLE
            holder.b.accentBar.setBackgroundColor(blendAlpha(colorPrimary, 0x88))
        } else {
            holder.b.cardRoot.setCardBackgroundColor(colorSurface)
            holder.b.cardRoot.strokeWidth = 0
        }

        holder.b.btnCopy.setOnClickListener {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(item.label, "${item.label}: ${item.value}"))
            Toast.makeText(ctx, "Copied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = filteredItems.size

    fun filter(query: String) {
        filteredItems = if (query.isBlank()) allItems
        else {
            val q = query.trim().lowercase()
            allItems.filter { it.label.lowercase().contains(q) || it.value.lowercase().contains(q) }
        }
        notifyDataSetChanged()
    }

    // Helper: blend a color with given alpha keeping RGB from color
    private fun blendAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun isDarkTheme(ctx: Context): Boolean {
        val nightMode = ctx.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
