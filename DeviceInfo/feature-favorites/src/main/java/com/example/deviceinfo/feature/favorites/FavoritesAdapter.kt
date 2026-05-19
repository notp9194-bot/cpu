package com.example.deviceinfo.feature.favorites

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinfo.core.favorites.FavoritesManager
import com.example.deviceinfo.feature.favorites.databinding.ItemFavoriteBinding

class FavoritesAdapter(
    private var items: List<FavoritesManager.FavoriteItem>,
    private val onUnpin: (FavoritesManager.FavoriteItem) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.VH>() {

    private var filteredItems: List<FavoritesManager.FavoriteItem> = items

    inner class VH(val b: ItemFavoriteBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = filteredItems.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = filteredItems[pos]
        val ctx  = holder.itemView.context
        val isNight = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val labelColor = if (isNight) 0xFF8B9AB5.toInt() else 0xFF546E7A.toInt()
        val valueColor = if (isNight) 0xFF00E5FF.toInt() else 0xFF006064.toInt()
        val tabColor   = if (isNight) 0x8000E5FF.toInt() else 0x80009688.toInt()
        val cardBg     = if (isNight) 0x0B00E5FF.toInt() else 0x08009688.toInt()
        val strokeCol  = if (isNight) 0x1500E5FF.toInt() else 0x25009688.toInt()
        val barColor   = if (isNight) 0xFF00E5FF.toInt() else 0xFF0097A7.toInt()
        val unpinTint  = if (isNight) 0x9900E5FF.toInt() else 0x99007780.toInt()

        holder.b.cardRoot.setCardBackgroundColor(cardBg)
        holder.b.cardRoot.strokeColor = strokeCol
        holder.b.accentBar.setBackgroundColor(barColor)
        holder.b.tvTab.text      = item.tabName
        holder.b.tvTab.setTextColor(tabColor)
        holder.b.tvLabel.text    = item.label
        holder.b.tvLabel.setTextColor(labelColor)
        holder.b.tvValue.text    = item.value
        holder.b.tvValue.setTextColor(valueColor)
        holder.b.btnUnpin.setColorFilter(unpinTint)

        holder.b.btnUnpin.setOnClickListener {
            onUnpin(item)
        }

        holder.itemView.setOnLongClickListener {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(item.label, "${item.label}: ${item.value}"))
            Toast.makeText(ctx, "Copied!", Toast.LENGTH_SHORT).show()
            true
        }
    }

    fun updateItems(newItems: List<FavoritesManager.FavoriteItem>) {
        items         = newItems
        filteredItems = newItems
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredItems = if (query.isBlank()) items
        else {
            val q = query.trim().lowercase()
            items.filter {
                it.label.lowercase().contains(q) ||
                it.value.lowercase().contains(q) ||
                it.tabName.lowercase().contains(q)
            }
        }
        notifyDataSetChanged()
    }
}
