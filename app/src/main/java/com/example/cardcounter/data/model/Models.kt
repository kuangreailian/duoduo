package com.example.cardcounter.data.model

enum class Suit(val symbol: String) {
    SPADE("♠"), HEART("♥"), CLUB("♣"), DIAMOND("♦"), JOKER("🃏")
}

enum class Rank(val label: String, val value: Int) {
    THREE("3", 3), FOUR("4", 4), FIVE("5", 5), SIX("6", 6),
    SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9), TEN("10", 10),
    JACK("J", 11), QUEEN("Q", 12), KING("K", 13), ACE("A", 14),
    TWO("2", 15), SMALL_JOKER("小王", 16), BIG_JOKER("大王", 17)
}

data class Card(val suit: Suit, val rank: Rank) {
    val display: String
        get() = if (suit == Suit.JOKER) rank.label else suit.symbol + rank.label
}

enum class GameType {
    DADAA;

    fun displayName(): String = "打大A"

    fun displayOrder(): List<Rank> = listOf(
        Rank.BIG_JOKER, Rank.SMALL_JOKER, Rank.ACE, Rank.KING, Rank.QUEEN,
        Rank.JACK, Rank.TEN, Rank.NINE, Rank.EIGHT, Rank.SEVEN, Rank.SIX,
        Rank.FIVE, Rank.FOUR, Rank.THREE, Rank.TWO
    )

    fun seatLabels(): List<String> = listOf("本", "上", "对", "下", "五")
}

data class GameConfig(
    val gameType: GameType,
    val deckCount: Int,
    val playerCount: Int,
    val cardsPerPlayer: Int,
    val bottomCount: Int,
    val cardPool: List<Card>
)

object DeckFactory {
    private val single54: List<Card> = buildList {
        for (s in listOf(Suit.SPADE, Suit.HEART, Suit.CLUB, Suit.DIAMOND)) {
            for (r in Rank.values().filter { it.value in 3..15 }) {
                add(Card(s, r))
            }
        }
        add(Card(Suit.JOKER, Rank.SMALL_JOKER))
        add(Card(Suit.JOKER, Rank.BIG_JOKER))
    }

    fun pool(deckCount: Int): List<Card> = buildList {
        repeat(deckCount) { addAll(single54) }
    }
}
