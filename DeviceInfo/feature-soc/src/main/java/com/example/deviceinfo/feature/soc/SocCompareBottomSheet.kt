package com.example.deviceinfo.feature.soc

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.deviceinfo.feature.soc.databinding.BottomSheetSocCompareBinding

/**
 * Bottom sheet that lets the user:
 *  1. See their auto-detected chip (if matched)
 *  2. Filter by maker / tier
 *  3. Search chips by name
 *  4. Tap any chip to show comparison chart
 */
class SocCompareBottomSheet : BottomSheetDialogFragment() {

    private var _b: BottomSheetSocCompareBinding? = null
    private val b get() = _b!!

    private var allChips    = SocDatabase.chips
    private var filtered    = allChips.toMutableList()
    private var yourChip    = SocDatabase.detectCurrentChip()
    private var selectedRef: SocDatabase.SocEntry? = null

    private var makerFilter = "All"
    private var tierFilter  = "All"
    private var searchQuery = ""

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        BottomSheetSocCompareBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)

        // ── Detected chip banner ───────────────────────────────────────
        if (yourChip != null) {
            b.tvYourChip.text = "📱 Your Device: ${yourChip!!.name}"
            b.tvYourChip.visibility = View.VISIBLE
            b.tvYourScore.text = buildScoreText(yourChip!!)
            b.tvYourScore.visibility = View.VISIBLE
        } else {
            b.tvYourChip.text = "⚠ Chip not in database — select one from the list"
            b.tvYourChip.visibility = View.VISIBLE
            b.tvYourScore.visibility = View.GONE
        }

        // ── Maker chips ────────────────────────────────────────────────
        val makers = listOf("All", "Qualcomm", "MediaTek", "Samsung", "Google", "HiSilicon", "Unisoc")
        makers.forEach { maker ->
            val chip = Chip(requireContext()).apply {
                text = maker
                isCheckable = true
                isChecked = maker == "All"
            }
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) { makerFilter = maker; applyFilters() }
            }
            b.chipGroupMaker.addView(chip)
        }

        // ── Tier chips ─────────────────────────────────────────────────
        val tiers = listOf("All") + SocDatabase.Tier.values().map { it.label }
        tiers.forEach { tier ->
            val chip = Chip(requireContext()).apply {
                text = tier
                isCheckable = true
                isChecked = tier == "All"
            }
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) { tierFilter = tier; applyFilters() }
            }
            b.chipGroupTier.addView(chip)
        }

        // ── Search ─────────────────────────────────────────────────────
        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { searchQuery = s?.toString() ?: ""; applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        // ── RecyclerView ───────────────────────────────────────────────
        b.rvChips.layoutManager = LinearLayoutManager(requireContext())
        applyFilters()

        // ── Default: compare with flagship (Snapdragon 8 Gen 3) ───────
        if (selectedRef == null) {
            selectedRef = allChips.find { it.name.contains("8 Gen 3") } ?: allChips.first()
            updateChart()
        }
    }

    private fun applyFilters() {
        filtered = allChips.filter { chip ->
            val makerOk = makerFilter == "All" || chip.maker == makerFilter
            val tierOk  = tierFilter  == "All" || chip.tier.label == tierFilter
            val queryOk = searchQuery.isBlank() || chip.name.contains(searchQuery, ignoreCase = true)
            makerOk && tierOk && queryOk
        }.sortedByDescending { SocDatabase.totalScore(it) }.toMutableList()

        b.rvChips.adapter = ChipListAdapter(filtered, selectedRef) { chip ->
            selectedRef = chip
            updateChart()
            (b.rvChips.adapter as ChipListAdapter).setSelected(chip)
        }
    }

    private fun updateChart() {
        val ref = selectedRef ?: return
        b.compareChart.update(SocCompareChartView.CompareData(yourChip, ref))
        b.tvRefChipName.text = "vs ${ref.name}  •  ${ref.process}  •  ${ref.year}"
        b.tvRefChipScore.text = buildScoreText(ref)
    }

    private fun buildScoreText(chip: SocDatabase.SocEntry): String {
        val total = SocDatabase.totalScore(chip)
        return "Score: $total/100  •  ${chip.tier.label}  •  ${chip.process}"
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }

    companion object { const val TAG = "SocCompareBottomSheet" }

    // ── Inner RecyclerView Adapter ─────────────────────────────────────
    private class ChipListAdapter(
        private val items: List<SocDatabase.SocEntry>,
        private var selected: SocDatabase.SocEntry?,
        private val onSelect: (SocDatabase.SocEntry) -> Unit
    ) : RecyclerView.Adapter<ChipListAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val root:    LinearLayout  = v as LinearLayout
            val tvName:  TextView      = v.findViewById(android.R.id.text1)
            val tvSub:   TextView      = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val ll  = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                setPadding(
                    (16 * ctx.resources.displayMetrics.density).toInt(),
                    (10 * ctx.resources.displayMetrics.density).toInt(),
                    (16 * ctx.resources.displayMetrics.density).toInt(),
                    (10 * ctx.resources.displayMetrics.density).toInt()
                )
            }
            val tv1 = TextView(ctx).apply {
                id = android.R.id.text1
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val tv2 = TextView(ctx).apply {
                id = android.R.id.text2
                textSize = 12f
                setTextColor(0xFF888888.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            ll.addView(tv1); ll.addView(tv2)
            return VH(ll)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val chip = items[pos]
            val total = SocDatabase.totalScore(chip)
            h.tvName.text = chip.name
            h.tvName.setTextColor(chip.tier.color)
            h.tvSub.text  = "${chip.maker}  •  ${chip.process}  •  ${chip.year}  •  Score: $total"
            h.root.setBackgroundColor(
                if (chip == selected) 0x207B2FBE else 0x00000000
            )
            h.root.setOnClickListener { onSelect(chip) }
        }

        fun setSelected(chip: SocDatabase.SocEntry) {
            selected = chip; notifyDataSetChanged()
        }

        override fun getItemCount() = items.size
    }
}
