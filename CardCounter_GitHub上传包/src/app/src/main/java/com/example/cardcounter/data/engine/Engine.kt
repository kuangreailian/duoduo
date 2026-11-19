package com.example.cardcounter.data.engine

import com.example.cardcounter.data.model.Card
import com.example.cardcounter.data.model.GameConfig
import com.example.cardcounter.data.model.Rank

class CountingEngine(val config: GameConfig) {

    sealed class HistoryEntry {
        data class Play(val card: Card, val playerIndex: Int?) : HistoryEntry()
        data class Restore(val card: Card) : HistoryEntry()
    }

    private val remaining = config.cardPool.groupingBy { it }.eachCount().toMutableMap()
    private val totalCounts = config.cardPool.groupingBy { it }.eachCount()
    private val playedByPlayer = Array(config.playerCount) { mutableMapOf<Card, Int>() }
    private val history = ArrayDeque<HistoryEntry>()

    fun record(card: Card, playerIndex: Int? = null): Boolean {
        val cur = remaining[card] ?: return false
        if (cur <= 0) return false
        remaining[card] = cur - 1
        if (playerIndex != null && playerIndex in 0 until config.playerCount) {
            playedByPlayer[playerIndex][card] = (playedByPlayer[playerIndex][card] ?: 0) + 1
        }
        history.addLast(HistoryEntry.Play(card, playerIndex))
        return true
    }

    fun recordByRank(rank: Rank, playerIndex: Int? = null): Boolean {
        val card = remaining.entries
            .firstOrNull { it.key.rank == rank && it.value > 0 }
            ?.key ?: return false
        return record(card, playerIndex)
    }

    /** 4取蛋：用 fourCount 个 4，取回 (fourCount+1) 张同点数牌 */
    fun takeEggWithFours(fourCount: Int, takenRank: Rank): Boolean {
        if (fourCount < 2) return false
        val foursAvailable = remaining.entries
            .filter { it.key.rank == Rank.FOUR }.sumOf { it.value }
        if (foursAvailable < fourCount) return false

        val take = fourCount + 1
        val restored = pickRestorable(takenRank, take)
        if (restored.size < take) return false

        repeat(fourCount) {
            val four = remaining.entries
                .firstOrNull { it.key.rank == Rank.FOUR && it.value > 0 }!!.key
            record(four, null)
        }
        restored.forEach { card ->
            remaining[card] = (remaining[card] ?: 0) + 1
            history.addLast(HistoryEntry.Restore(card))
        }
        return true
    }

    fun undo(): Boolean {
        val e = history.removeLastOrNull() ?: return false
        when (e) {
            is HistoryEntry.Play -> {
                remaining[e.card] = (remaining[e.card] ?: 0) + 1
                if (e.playerIndex != null && e.playerIndex in 0 until config.playerCount) {
                    val m = playedByPlayer[e.playerIndex]
                    m[e.card] = (m[e.card] ?: 1) - 1
                    if (m[e.card] == 0) m.remove(e.card)
                }
            }
            is HistoryEntry.Restore -> {
                remaining[e.card] = ((remaining[e.card] ?: 1) - 1).coerceAtLeast(0)
            }
        }
        return true
    }

    fun remainingCount(card: Card): Int = remaining[card] ?: 0

    fun remainingByRank(): Map<Rank, Int> =
        remaining.entries
            .groupBy({ it.key.rank }, { it.value })
            .mapValues { (_, v) -> v.sum() }

    fun unseenTotal(): Int = remaining.values.sum()

    fun playedCount(): Int = history.count { it is HistoryEntry.Play }

    fun playerHandSize(playerIndex: Int): Int {
        if (playerIndex !in 0 until config.playerCount) return 0
        val played = playedByPlayer[playerIndex].values.sum()
        return (config.cardsPerPlayer - played).coerceAtLeast(0)
    }

    fun probabilityAtLeastOne(
        card: Card,
        playerIndex: Int,
        knownHand: Map<Card, Int> = emptyMap()
    ): Double {
        val c = remaining[card] ?: 0
        if (c == 0) return 0.0
        val hand = playerHandSize(playerIndex) - knownHand.values.sum()
        if (hand <= 0) return 0.0
        val unseen = (unseenTotal() - knownHand.values.sum()).coerceAtLeast(0)
        if (unseen <= 0 || hand > unseen) return 0.0
        val total = comb(unseen, hand)
        val noThis = comb(unseen - c, hand)
        return 1.0 - noThis / total
    }

    fun probabilityAtLeastOneOfRank(
        rank: Rank,
        playerIndex: Int,
        knownHand: Map<Card, Int> = emptyMap()
    ): Double {
        val m = remainingByRank()[rank] ?: 0
        if (m == 0) return 0.0
        val hand = playerHandSize(playerIndex) - knownHand.values.sum()
        if (hand <= 0) return 0.0
        val unseen = (unseenTotal() - knownHand.values.sum()).coerceAtLeast(0)
        if (unseen <= 0 || hand > unseen) return 0.0
        val total = comb(unseen, hand)
        val noThis = comb(unseen - m, hand)
        return 1.0 - noThis / total
    }

    fun reset() {
        remaining.clear()
        remaining.putAll(config.cardPool.groupingBy { it }.eachCount())
        playedByPlayer.forEach { it.clear() }
        history.clear()
    }

    private fun pickRestorable(rank: Rank, count: Int): List<Card> {
        val result = mutableListOf<Card>()
        for (card in config.cardPool) {
            if (card.rank != rank || result.size >= count) continue
            val cur = remaining[card] ?: 0
            val max = totalCounts[card] ?: 0
            val canAdd = max - cur
            if (canAdd > 0) {
                repeat(minOf(canAdd, count - result.size)) { result.add(card) }
            }
        }
        return result
    }

    private fun comb(n: Int, k: Int): Double {
        if (k < 0 || k > n) return 0.0
        val kk = minOf(k, n - k)
        var res = 1.0
        for (i in 1..kk) res = res * (n - kk + i) / i
        return res
    }
}

data class EngineSnapshot(
    val remainingByRank: Map<Rank, Int>,
    val totalByRank: Map<Rank, Int>,
    val unseenTotal: Int,
    val playerHandSizes: List<Int>,
    val probabilityByPlayer: List<Map<Rank, Double>>
)

fun CountingEngine.toSnapshot(): EngineSnapshot {
    val remaining = Rank.values().associateWith { 0 }.toMutableMap()
    remainingByRank().forEach { (r, c) -> remaining[r] = c }
    val total = Rank.values().associateWith { 0 }.toMutableMap()
    config.cardPool.groupingBy { it.rank }.eachCount().forEach { (r, c) -> total[r] = c }
    return EngineSnapshot(
        remainingByRank = remaining,
        totalByRank = total,
        unseenTotal = unseenTotal(),
        playerHandSizes = List(config.playerCount) { playerHandSize(it) },
        probabilityByPlayer = List(config.playerCount) { p ->
            Rank.values().associateWith { r -> probabilityAtLeastOneOfRank(r, p) }
        }
    )
}
