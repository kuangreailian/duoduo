package com.example.cardcounter.core

import android.graphics.Bitmap
import com.example.cardcounter.data.engine.CountingEngine
import com.example.cardcounter.data.engine.EngineSnapshot
import com.example.cardcounter.data.engine.toSnapshot
import com.example.cardcounter.data.model.Card
import com.example.cardcounter.data.model.GameConfig
import com.example.cardcounter.data.model.GameType
import com.example.cardcounter.data.model.Rank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveSession {

    private val _snapshot = MutableStateFlow<EngineSnapshot?>(null)
    val snapshot: StateFlow<EngineSnapshot?> = _snapshot.asStateFlow()

    private val _countingStarted = MutableStateFlow(false)
    val countingStarted: StateFlow<Boolean> = _countingStarted.asStateFlow()

    private val _selectedPlayer = MutableStateFlow<Int?>(null)
    val selectedPlayer: StateFlow<Int?> = _selectedPlayer.asStateFlow()

    private var startTimeMs: Long = 0L

    var engine: CountingEngine? = null
        private set
    var config: GameConfig? = null
        private set

    val isRunning: Boolean get() = engine != null

    data class SessionSummary(
        val gameType: GameType,
        val playerCount: Int,
        val deckCount: Int,
        val startTimeMs: Long,
        val endTimeMs: Long,
        val totalPlayed: Int,
        val remaining: Int
    )

    fun start(config: GameConfig) {
        startTimeMs = System.currentTimeMillis()
        this.config = config
        this.engine = CountingEngine(config)
        _countingStarted.value = false
        emit()
    }

    fun startCounting() {
        _countingStarted.value = true
    }

    fun selectPlayer(index: Int?) {
        _selectedPlayer.value = index
    }

    fun recordCard(card: Card) {
        if (!_countingStarted.value) return
        engine?.record(card)
        emit()
    }

    fun recordRank(rank: Rank) {
        if (!_countingStarted.value) return
        engine?.recordByRank(rank, _selectedPlayer.value)
        emit()
    }

    /** 自动识别专用：按位置直接归到指定玩家 */
    fun recordRankFor(rank: Rank, playerIndex: Int?) {
        if (!_countingStarted.value) return
        engine?.recordByRank(rank, playerIndex)
        emit()
    }

    fun takeEgg(fourCount: Int, takenRank: Rank) {
        if (!_countingStarted.value) return
        engine?.takeEggWithFours(fourCount, takenRank)
        emit()
    }

    fun undo() {
        engine?.undo()
        emit()
    }

    fun reset() {
        engine?.reset()
        emit()
    }

    fun endSession(): SessionSummary? {
        val eng = engine ?: return null
        val cfg = config ?: return null
        val summary = SessionSummary(
            gameType = cfg.gameType,
            playerCount = cfg.playerCount,
            deckCount = cfg.deckCount,
            startTimeMs = startTimeMs,
            endTimeMs = System.currentTimeMillis(),
            totalPlayed = eng.playedCount(),
            remaining = eng.unseenTotal()
        )
        stop()
        return summary
    }

    fun stop() {
        engine = null
        config = null
        _snapshot.value = null
        _countingStarted.value = false
    }

    private fun emit() {
        _snapshot.value = engine?.toSnapshot()
    }
}

object LatestFrameHolder {
    @Volatile
    var latestBitmap: Bitmap? = null
        private set

    fun set(bitmap: Bitmap) {
        latestBitmap = bitmap
    }
}
