package com.example.cardcounter.data.presets

import com.example.cardcounter.data.model.*

object GamePresets {
    val DADAA = GameConfig(
        gameType = GameType.DADAA,
        deckCount = 2,
        playerCount = 5,
        cardsPerPlayer = 21,
        bottomCount = 3,
        cardPool = DeckFactory.pool(2)
    )
}
