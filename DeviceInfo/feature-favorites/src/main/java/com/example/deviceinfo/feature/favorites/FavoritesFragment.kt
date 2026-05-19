package com.example.deviceinfo.feature.favorites

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deviceinfo.core.favorites.FavoritesManager
import com.example.deviceinfo.feature.favorites.databinding.FragmentFavoritesBinding

class FavoritesFragment : Fragment() {

    private var _b: FragmentFavoritesBinding? = null
    private val b get() = _b!!
    private var adapter: FavoritesAdapter? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentFavoritesBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        b.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        b.btnClearAll.setOnClickListener {
            val count = FavoritesManager.getAll(requireContext()).size
            AlertDialog.Builder(requireContext())
                .setTitle("Clear all pinned items?")
                .setMessage("$count pinned item${if (count == 1) "" else "s"} will be removed.")
                .setPositiveButton("Clear") { _, _ ->
                    FavoritesManager.clearAll(requireContext())
                    refresh()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        b.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter?.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh() // Re-load every time tab becomes visible
    }

    private fun refresh() {
        val ctx   = context ?: return
        val items = FavoritesManager.getAll(ctx)

        b.tvCount.text = "${items.size} item${if (items.size == 1) "" else "s"}"

        if (items.isEmpty()) {
            b.layoutEmpty.visibility  = View.VISIBLE
            b.recyclerView.visibility = View.GONE
            adapter = null
        } else {
            b.layoutEmpty.visibility  = View.GONE
            b.recyclerView.visibility = View.VISIBLE

            if (adapter == null) {
                adapter = FavoritesAdapter(items) { fav ->
                    FavoritesManager.unpin(ctx, fav.tabName, fav.label)
                    refresh()
                }
                b.recyclerView.adapter = adapter
            } else {
                adapter!!.updateItems(items)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
