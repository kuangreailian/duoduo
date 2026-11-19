package com.example.cardcounter.capture

import android.content.Context

/**
 * 玩家区域配置：屏幕划分给各玩家（归一化坐标 0..1）。
 * 识别到牌后根据牌的中心点落在哪个区域，自动归到对应玩家。
 * 默认按横屏 5 人（打大A）：本家底部、上家左、对家顶部、下家右、第五家中部。
 */
data class PlayerRegion(
    val playerIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

object PlayerRegionConfig {

    private const val PREFS = "player_regions"

    fun defaults(): List<PlayerRegion> = listOf(
        PlayerRegion(0, 0.25f, 0.65f, 0.75f, 1.00f),   // 本家（自己）
        PlayerRegion(1, 0.00f, 0.25f, 0.30f, 0.75f),   // 上家
        PlayerRegion(2, 0.30f, 0.00f, 0.70f, 0.30f),   // 对家
        PlayerRegion(3, 0.70f, 0.25f, 1.00f, 0.75f),   // 下家
        PlayerRegion(4, 0.25f, 0.30f, 0.75f, 0.65f)    // 第五家
    )

    fun load(context: Context): List<PlayerRegion> {
        // MVP：先用默认区域，后续可加可视化框选配置
        return defaults()
    }

    fun playerAt(regions: List<PlayerRegion>, x: Float, y: Float, w: Float, h: Float): Int? {
        if (w <= 0f || h <= 0f) return null
        val nx = x / w
        val ny = y / h
        return regions.firstOrNull {
            nx >= it.left && nx <= it.right && ny >= it.top && ny <= it.bottom
        }?.playerIndex
    }
}
