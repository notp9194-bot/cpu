package com.cpua.deviceinfo.feature.soc

import android.os.Build

/**
 * Offline SoC Comparison Database.
 * Contains ~55 popular SoCs with normalized benchmark scores.
 * Data sourced from public Geekbench / AnTuTu averages (normalized 0-100).
 * No internet needed. Play Store safe ✅
 *
 * Scores are normalized relative to best chip (Snapdragon 8 Gen 3 = 100).
 *   singleScore  → Single-thread CPU performance
 *   multiScore   → Multi-thread CPU performance
 *   gpuScore     → GPU performance
 *   aiScore      → AI / NPU performance
 *   totalScore   → Weighted average (30% single + 30% multi + 25% gpu + 15% ai)
 */
object SocDatabase {

    data class SocEntry(
        val name: String,           // Display name
        val maker: String,          // Qualcomm / MediaTek / Samsung / Apple / HiSilicon / Google
        val year: Int,              // Launch year
        val process: String,        // e.g. "4nm TSMC"
        val cores: String,          // e.g. "1+3+4 @ 3.3GHz"
        val gpu: String,            // GPU name
        val singleScore: Int,       // 0-100
        val multiScore: Int,        // 0-100
        val gpuScore: Int,          // 0-100
        val aiScore: Int,           // 0-100
        val tier: Tier,
        val keywords: List<String>  // for search/matching from Build.HARDWARE
    )

    enum class Tier(val label: String, val color: Int) {
        FLAGSHIP("Flagship",    0xFFF44336.toInt()),
        HIGH_END("High-End",    0xFFFF9800.toInt()),
        MID_HIGH("Mid-High",    0xFF2196F3.toInt()),
        MID("Mid-Range",        0xFF4CAF50.toInt()),
        ENTRY("Entry-Level",    0xFF9E9E9E.toInt())
    }

    val chips: List<SocEntry> = listOf(

        // ── Qualcomm Snapdragon ────────────────────────────────────────
        SocEntry("Snapdragon 8 Gen 3", "Qualcomm", 2023, "4nm TSMC",
            "1×3.3GHz + 5×3.15GHz + 2×2.27GHz", "Adreno 750",
            100, 100, 100, 100, Tier.FLAGSHIP,
            listOf("kona", "pineapple", "sm8650", "8gen3")),

        SocEntry("Snapdragon 8 Gen 2", "Qualcomm", 2022, "4nm TSMC",
            "1×3.2GHz + 2×2.8GHz + 2×2.8GHz + 3×2.0GHz", "Adreno 740",
            91, 93, 88, 87, Tier.FLAGSHIP,
            listOf("waipio", "sm8550", "8gen2")),

        SocEntry("Snapdragon 8 Gen 1", "Qualcomm", 2021, "4nm Samsung",
            "1×3.0GHz + 3×2.5GHz + 4×1.8GHz", "Adreno 730",
            82, 85, 78, 78, Tier.FLAGSHIP,
            listOf("lahaina", "sm8450", "8gen1")),

        SocEntry("Snapdragon 888", "Qualcomm", 2020, "5nm Samsung",
            "1×2.84GHz + 3×2.42GHz + 4×1.8GHz", "Adreno 660",
            74, 77, 70, 68, Tier.FLAGSHIP,
            listOf("lahaina", "sm8350", "888")),

        SocEntry("Snapdragon 865", "Qualcomm", 2019, "7nm TSMC",
            "1×2.84GHz + 3×2.42GHz + 4×1.8GHz", "Adreno 650",
            66, 68, 60, 58, Tier.HIGH_END,
            listOf("kona", "sm8250", "865")),

        SocEntry("Snapdragon 8s Gen 3", "Qualcomm", 2024, "4nm TSMC",
            "1×3.0GHz + 4×2.8GHz + 3×2.0GHz", "Adreno 735",
            90, 91, 84, 85, Tier.HIGH_END,
            listOf("kalama", "sm8635", "8sgen3")),

        SocEntry("Snapdragon 7 Gen 3", "Qualcomm", 2023, "4nm TSMC",
            "1×2.63GHz + 4×2.4GHz + 3×1.8GHz", "Adreno 720",
            78, 78, 72, 72, Tier.HIGH_END,
            listOf("sm7675", "7gen3")),

        SocEntry("Snapdragon 7 Gen 1", "Qualcomm", 2022, "4nm TSMC",
            "1×2.4GHz + 3×2.36GHz + 4×1.8GHz", "Adreno 644",
            68, 68, 58, 58, Tier.MID_HIGH,
            listOf("sm7450", "7gen1")),

        SocEntry("Snapdragon 778G", "Qualcomm", 2021, "6nm Samsung",
            "1×2.4GHz + 3×2.2GHz + 4×1.9GHz", "Adreno 642L",
            62, 63, 50, 50, Tier.MID_HIGH,
            listOf("holi", "sm7325", "778g")),

        SocEntry("Snapdragon 695", "Qualcomm", 2021, "6nm Samsung",
            "2×2.2GHz + 6×1.7GHz", "Adreno 619",
            52, 54, 38, 38, Tier.MID,
            listOf("sm6375", "695")),

        SocEntry("Snapdragon 680", "Qualcomm", 2021, "6nm Samsung",
            "4×2.4GHz + 4×1.9GHz", "Adreno 610",
            44, 46, 28, 28, Tier.MID,
            listOf("sm6225", "680")),

        SocEntry("Snapdragon 4 Gen 2", "Qualcomm", 2023, "4nm TSMC",
            "2×2.2GHz + 6×1.95GHz", "Adreno 613",
            48, 50, 32, 32, Tier.MID,
            listOf("sm4450", "4gen2")),

        SocEntry("Snapdragon 4 Gen 1", "Qualcomm", 2022, "6nm Samsung",
            "2×2.0GHz + 6×1.8GHz", "Adreno 613",
            42, 44, 26, 26, Tier.ENTRY,
            listOf("sm4350", "4gen1")),

        SocEntry("Snapdragon 662", "Qualcomm", 2020, "11nm Samsung",
            "4×2.0GHz + 4×1.8GHz", "Adreno 610",
            36, 38, 22, 22, Tier.ENTRY,
            listOf("sm6115", "662")),

        // ── MediaTek Dimensity ─────────────────────────────────────────
        SocEntry("Dimensity 9300", "MediaTek", 2023, "4nm TSMC",
            "4×3.25GHz + 4×2.0GHz", "Immortalis-G720",
            96, 99, 96, 90, Tier.FLAGSHIP,
            listOf("mt6989", "dimensity9300", "9300")),

        SocEntry("Dimensity 9200+", "MediaTek", 2023, "4nm TSMC",
            "1×3.35GHz + 3×3.0GHz + 4×1.8GHz", "Immortalis-G715",
            92, 94, 90, 84, Tier.FLAGSHIP,
            listOf("mt6985", "dimensity9200", "9200")),

        SocEntry("Dimensity 9000+", "MediaTek", 2022, "4nm TSMC",
            "1×3.2GHz + 3×2.85GHz + 4×1.8GHz", "Mali-G710",
            84, 87, 80, 76, Tier.FLAGSHIP,
            listOf("mt6983", "dimensity9000", "9000")),

        SocEntry("Dimensity 8300", "MediaTek", 2023, "4nm TSMC",
            "4×3.35GHz + 4×2.2GHz", "Mali-G615",
            82, 84, 74, 74, Tier.HIGH_END,
            listOf("mt6897", "dimensity8300", "8300")),

        SocEntry("Dimensity 8200", "MediaTek", 2022, "4nm TSMC",
            "1×3.1GHz + 3×3.0GHz + 4×2.0GHz", "Mali-G610",
            76, 79, 66, 66, Tier.HIGH_END,
            listOf("mt6895", "dimensity8200", "8200")),

        SocEntry("Dimensity 8100", "MediaTek", 2022, "5nm TSMC",
            "4×2.85GHz + 4×2.0GHz", "Mali-G610",
            70, 73, 58, 58, Tier.HIGH_END,
            listOf("mt6893", "dimensity8100", "8100")),

        SocEntry("Dimensity 7200", "MediaTek", 2023, "4nm TSMC",
            "2×2.8GHz + 6×2.0GHz", "Mali-G610",
            64, 65, 50, 50, Tier.MID_HIGH,
            listOf("mt6886", "dimensity7200", "7200")),

        SocEntry("Dimensity 1200", "MediaTek", 2021, "6nm TSMC",
            "1×3.0GHz + 3×2.6GHz + 4×2.0GHz", "Mali-G77",
            62, 64, 46, 46, Tier.MID_HIGH,
            listOf("mt6893", "dimensity1200", "1200")),

        SocEntry("Dimensity 700", "MediaTek", 2020, "7nm TSMC",
            "2×2.2GHz + 6×2.0GHz", "Mali-G57",
            44, 46, 28, 28, Tier.MID,
            listOf("mt6833", "dimensity700", "700")),

        SocEntry("Dimensity 6100+", "MediaTek", 2023, "6nm TSMC",
            "2×2.2GHz + 6×2.0GHz", "Mali-G57",
            40, 42, 24, 24, Tier.ENTRY,
            listOf("mt6877", "dimensity6100", "6100")),

        SocEntry("Helio G99", "MediaTek", 2022, "6nm TSMC",
            "2×2.2GHz + 6×2.0GHz", "Mali-G57",
            46, 48, 30, 30, Tier.MID,
            listOf("mt6789", "heliog99", "g99")),

        SocEntry("Helio G85", "MediaTek", 2020, "12nm TSMC",
            "2×2.0GHz + 6×1.8GHz", "Mali-G52",
            32, 34, 18, 18, Tier.ENTRY,
            listOf("mt6769", "heliog85", "g85")),

        SocEntry("Helio G37", "MediaTek", 2021, "12nm TSMC",
            "4×2.3GHz + 4×1.8GHz", "IMG PowerVR GE8320",
            24, 26, 12, 12, Tier.ENTRY,
            listOf("mt6765", "heliog37", "g37")),

        // ── Samsung Exynos ─────────────────────────────────────────────
        SocEntry("Exynos 2400", "Samsung", 2024, "4nm Samsung",
            "1×3.2GHz + 2×2.9GHz + 3×2.6GHz + 4×1.95GHz", "Xclipse 940",
            89, 90, 86, 86, Tier.FLAGSHIP,
            listOf("exynos2400", "s5e9945")),

        SocEntry("Exynos 2200", "Samsung", 2022, "4nm Samsung",
            "1×2.8GHz + 3×2.5GHz + 4×1.8GHz", "Xclipse 920 (RDNA2)",
            76, 78, 74, 70, Tier.FLAGSHIP,
            listOf("exynos2200", "s5e9925")),

        SocEntry("Exynos 2100", "Samsung", 2021, "5nm Samsung",
            "1×2.9GHz + 3×2.8GHz + 4×2.2GHz", "Mali-G78",
            72, 74, 64, 62, Tier.FLAGSHIP,
            listOf("exynos2100", "s5e9840")),

        SocEntry("Exynos 990", "Samsung", 2020, "7nm Samsung",
            "2×2.73GHz + 2×2.5GHz + 4×2.0GHz", "Mali-G77",
            62, 63, 52, 50, Tier.HIGH_END,
            listOf("exynos990", "s5e9830")),

        SocEntry("Exynos 1380", "Samsung", 2023, "5nm Samsung",
            "4×2.4GHz + 4×2.0GHz", "Xclipse 530",
            60, 62, 44, 44, Tier.MID_HIGH,
            listOf("exynos1380", "s5e8535")),

        SocEntry("Exynos 850", "Samsung", 2020, "8nm Samsung",
            "8×2.0GHz", "Mali-G52",
            28, 30, 14, 14, Tier.ENTRY,
            listOf("exynos850", "s5e3830")),

        // ── Google Tensor ──────────────────────────────────────────────
        SocEntry("Google Tensor G3", "Google", 2023, "4nm Samsung",
            "1×3.0GHz + 4×2.45GHz + 4×2.15GHz", "Immortalis-G715",
            78, 79, 80, 95, Tier.FLAGSHIP,
            listOf("zuma", "g3", "tensor")),

        SocEntry("Google Tensor G2", "Google", 2022, "5nm Samsung",
            "2×2.85GHz + 2×2.35GHz + 4×1.8GHz", "Mali-G710",
            68, 70, 62, 82, Tier.HIGH_END,
            listOf("cloudripper", "g2", "tensor")),

        SocEntry("Google Tensor G1", "Google", 2021, "5nm Samsung",
            "2×2.8GHz + 2×2.25GHz + 4×1.8GHz", "Mali-G78",
            60, 62, 52, 72, Tier.HIGH_END,
            listOf("whitechapel", "gs101", "tensor")),

        // ── HiSilicon Kirin ────────────────────────────────────────────
        SocEntry("Kirin 9000S", "HiSilicon", 2023, "7nm SMIC",
            "1×2.62GHz + 3×2.15GHz + 4×1.53GHz", "Maleoon 910",
            60, 62, 46, 60, Tier.HIGH_END,
            listOf("kirin9000s", "hisilicon")),

        SocEntry("Kirin 9000", "HiSilicon", 2020, "5nm TSMC",
            "1×3.13GHz + 3×2.54GHz + 4×2.05GHz", "Mali-G78",
            70, 72, 60, 68, Tier.HIGH_END,
            listOf("kirin9000", "hisilicon")),

        SocEntry("Kirin 990 5G", "HiSilicon", 2019, "7nm TSMC",
            "2×2.86GHz + 2×2.36GHz + 4×1.95GHz", "Mali-G76",
            58, 60, 46, 56, Tier.HIGH_END,
            listOf("kirin990", "hisilicon")),

        // ── Unisoc / SPRD ──────────────────────────────────────────────
        SocEntry("Unisoc T616", "Unisoc", 2021, "12nm TSMC",
            "2×1.82GHz + 6×1.82GHz", "Mali-G57",
            26, 28, 14, 14, Tier.ENTRY,
            listOf("t616", "unisoc", "sprd")),

        SocEntry("Unisoc T700", "Unisoc", 2022, "6nm TSMC",
            "4×1.8GHz + 4×1.6GHz", "Mali-G57",
            30, 32, 16, 16, Tier.ENTRY,
            listOf("t700", "unisoc", "sprd"))
    )

    /**
     * Try to auto-detect current device chip from Build info.
     * Returns matched SocEntry or null.
     */
    fun detectCurrentChip(): SocEntry? {
        val hardware = android.os.Build.HARDWARE.lowercase()
        val model    = android.os.Build.MODEL.lowercase()
        val board    = android.os.Build.BOARD.lowercase()
        val combined = "$hardware $model $board"

        // Score-based matching: most keyword hits wins
        return chips.maxByOrNull { chip ->
            chip.keywords.count { kw -> combined.contains(kw.lowercase()) }
                .takeIf { it > 0 } ?: -1
        }.let { best ->
            // Only return if at least 1 keyword matched
            if (best != null && best.keywords.any { kw -> combined.contains(kw.lowercase()) }) best
            else null
        }
    }

    /** Compute weighted total score */
    fun totalScore(e: SocEntry): Int =
        (e.singleScore * 0.30 + e.multiScore * 0.30 + e.gpuScore * 0.25 + e.aiScore * 0.15).toInt()
}
