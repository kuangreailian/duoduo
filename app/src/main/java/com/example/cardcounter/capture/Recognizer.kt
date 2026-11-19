package com.example.cardcounter.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.example.cardcounter.data.model.Rank
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

interface CardRecognizer {
    fun recognize(bitmap: Bitmap): List<DetectedRank>
}

data class DetectedRank(
    val rank: Rank,
    val confidence: Float,
    val box: RectF
)

object CardRecognizerFactory {
    fun create(context: Context): CardRecognizer = MlKitCardRecognizer()
}

class MlKitCardRecognizer : CardRecognizer {

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    override fun recognize(bitmap: Bitmap): List<DetectedRank> {
        return try {
            val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
            result.textBlocks
                .flatMap { block -> block.lines }
                .flatMap { line ->
                    val conf = line.confidence ?: 0.8f
                    val box = line.boundingBox
                    line.elements.mapNotNull { el ->
                        parseRank(el.text)?.let { rank ->
                            DetectedRank(rank, conf, box?.let { RectF(it) } ?: RectF())
                        }
                    }
                }
                .distinctBy { it.rank }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseRank(text: String): Rank? {
        val t = text.trim().replace(" ", "").uppercase()
        if (t.isEmpty()) return null
        return when {
            t == "10" || t == "1O" || t == "I0" || t == "IO" ||
            t == "十" || t == "一零" -> Rank.TEN
            t == "3" || t == "三" -> Rank.THREE
            t == "4" || t == "四" -> Rank.FOUR
            t == "5" || t == "五" -> Rank.FIVE
            t == "6" || t == "六" -> Rank.SIX
            t == "7" || t == "七" -> Rank.SEVEN
            t == "8" || t == "八" -> Rank.EIGHT
            t == "9" || t == "九" -> Rank.NINE
            t == "J" || t == "勾" -> Rank.JACK
            t == "Q" || t == "圈" -> Rank.QUEEN
            t == "K" || t == "凯" -> Rank.KING
            t == "A" || t == "尖" || t == "1" -> Rank.ACE
            t == "2" || t == "二" -> Rank.TWO
            t.contains("小王") -> Rank.SMALL_JOKER
            t.contains("大王") -> Rank.BIG_JOKER
            else -> null
        }
    }
}
