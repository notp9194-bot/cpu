package com.example.deviceinfo.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.core.databinding.ItemInfoBinding
import com.example.deviceinfo.core.favorites.FavoritesManager
import com.example.deviceinfo.core.model.InfoItem

/**
 * Main adapter for all info tabs.
 *
 * Pass [tabName] to enable Favorites:
 *   - Long-press any data row → pin/unpin
 *   - Copy button tint changes to indicate pinned state
 */
class InfoAdapter(
    private val allItems: List<InfoItem>,
    private val tabName: String = ""
) : RecyclerView.Adapter<InfoAdapter.VH>() {

    private val favEnabled = tabName.isNotBlank()
    private var filteredItems: List<InfoItem> = allItems

    inner class VH(val b: ItemInfoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = filteredItems[pos]
        val ctx  = holder.itemView.context
        val isNight = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val labelColor  = if (isNight) 0xFF8B9AB5.toInt() else 0xFF546E7A.toInt()
        val valueColor  = if (isNight) 0xFF00E5FF.toInt() else 0xFF006064.toInt()
        val headerBg    = if (isNight) 0xFF0A0E1A.toInt() else 0xFFE0F7FA.toInt()
        val headerText  = if (isNight) 0xFF00E5FF.toInt() else 0xFF006064.toInt()
        val cardHl      = if (isNight) 0x0F00E5FF.toInt() else 0x190097A7.toInt()
        val strokeHl    = if (isNight) 0x2000E5FF.toInt() else 0x400097A7.toInt()
        val accentBar   = if (isNight) 0x6600E5FF.toInt() else 0xFF0097A7.toInt()
        val normalCard  = if (isNight) 0x09AABBCC.toInt() else 0x08546E7A.toInt()

        // Reset defaults
        holder.b.tvLabel.visibility    = View.VISIBLE
        holder.b.tvValue.visibility    = View.VISIBLE
        holder.b.btnCopy.visibility    = View.VISIBLE
        holder.b.accentBar.visibility  = View.GONE
        holder.b.tvLabel.letterSpacing = 0.01f
        holder.b.tvLabel.textSize      = 13f
        holder.b.btnCopy.clearColorFilter()
        holder.itemView.setOnLongClickListener(null)

        // ── Section header (isHighlighted + empty value) ──────────────
        if (item.isHighlighted && item.value.isEmpty()) {
            holder.b.tvValue.visibility = View.GONE
            holder.b.btnCopy.visibility = View.GONE
            holder.b.tvLabel.text = item.label
            holder.b.tvLabel.setTextColor(headerText)
            holder.b.tvLabel.textSize     = 10f
            holder.b.tvLabel.letterSpacing = 0.10f
            holder.b.cardRoot.setCardBackgroundColor(headerBg)
            holder.b.cardRoot.strokeWidth = 0
            return
        }

        // ── Spacer row ────────────────────────────────────────────────
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
        holder.b.tvLabel.setTextColor(labelColor)
        holder.b.tvValue.setTextColor(valueColor)

        if (item.isHighlighted) {
            holder.b.cardRoot.setCardBackgroundColor(cardHl)
            holder.b.cardRoot.strokeWidth = 1
            holder.b.cardRoot.strokeColor = strokeHl
            holder.b.accentBar.visibility = View.VISIBLE
            holder.b.accentBar.setBackgroundColor(accentBar)
        } else {
            holder.b.cardRoot.setCardBackgroundColor(normalCard)
            holder.b.cardRoot.strokeWidth = 0
        }

        // ── Copy (tap) ────────────────────────────────────────────────
        holder.b.btnCopy.setOnClickListener {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(item.label, "${item.label}: ${item.value}"))
            Toast.makeText(ctx, "Copied!", Toast.LENGTH_SHORT).show()
        }

        // ── Favorites: long-press on any data row ─────────────────────
        if (favEnabled && item.value.isNotBlank()) {
            updateStarTint(holder, ctx, item, isNight)

            holder.itemView.setOnLongClickListener {
                val nowPinned = FavoritesManager.toggle(ctx, tabName, item.label, item.value)
                val msg = if (nowPinned) "⭐ Pinned to Favorites!" else "✖ Unpinned from Favorites"
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                updateStarTint(holder, ctx, item, isNight)
                true
            }
        }
    }

    /** Tints the copy-button icon to cyan when item is pinned. */
    private fun updateStarTint(holder: VH, ctx: Context, item: InfoItem, isNight: Boolean) {
        val pinned      = FavoritesManager.isPinned(ctx, tabName, item.label)
        val pinnedTint  = if (isNight) 0xFF00E5FF.toInt() else 0xFF0097A7.toInt()
        val normalTint  = 0x3A607080
        holder.b.btnCopy.setColorFilter(if (pinned) pinnedTint else normalTint)
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
}
