package com.cpua.deviceinfo.core.favorites

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages pinned items using SharedPreferences (no permissions needed).
 * Each pinned item stores: tabName, label, value.
 * Key used for dedup: "$tabName|$label"
 */
object FavoritesManager {

    private const val PREFS_NAME = "device_info_favorites"
    private const val KEY_ITEMS  = "pinned_items"

    data class FavoriteItem(
        val tabName: String,
        val label:   String,
        val value:   String
    ) {
        val uniqueKey: String get() = "$tabName|$label"
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(ctx: Context): List<FavoriteItem> {
        val raw = prefs(ctx).getString(KEY_ITEMS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                FavoriteItem(
                    tabName = o.optString("tab", ""),
                    label   = o.optString("label", ""),
                    value   = o.optString("value", "")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun isPinned(ctx: Context, tabName: String, label: String): Boolean {
        val key = "$tabName|$label"
        return getAll(ctx).any { it.uniqueKey == key }
    }

    fun pin(ctx: Context, tabName: String, label: String, value: String) {
        val items = getAll(ctx).toMutableList()
        val key   = "$tabName|$label"
        if (items.none { it.uniqueKey == key }) {
            items.add(0, FavoriteItem(tabName, label, value)) // newest first
            save(ctx, items)
        }
    }

    fun unpin(ctx: Context, tabName: String, label: String) {
        val key   = "$tabName|$label"
        val items = getAll(ctx).filter { it.uniqueKey != key }
        save(ctx, items)
    }

    fun toggle(ctx: Context, tabName: String, label: String, value: String): Boolean {
        return if (isPinned(ctx, tabName, label)) {
            unpin(ctx, tabName, label); false
        } else {
            pin(ctx, tabName, label, value); true
        }
    }

    fun clearAll(ctx: Context) = save(ctx, emptyList())

    private fun save(ctx: Context, items: List<FavoriteItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("tab",   item.tabName)
                put("label", item.label)
                put("value", item.value)
            })
        }
        prefs(ctx).edit().putString(KEY_ITEMS, arr.toString()).apply()
    }
}
