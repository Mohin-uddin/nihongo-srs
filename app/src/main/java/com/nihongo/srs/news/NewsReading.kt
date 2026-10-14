package com.nihongo.srs.news

import com.nihongo.srs.data.VocabEntity

/** One word (or particle, or punctuation) of a news sentence. */
data class NewsToken(
    /** As written in the sentence, e.g. "発表しました". */
    val text: String,
    /** Reading of [text] in kana. */
    val kana: String = "",
    /** Dictionary form, e.g. "発表する". */
    val base: String = "",
    val baseKana: String = "",
    val english: String = "",
    val bangla: String = "",
) {
    val hasMeaning: Boolean get() = english.isNotBlank() || bangla.isNotBlank()
    val isPunctuation: Boolean get() = text.all { !it.isLetterOrDigit() }
}

data class NewsSentence(
    val tokens: List<NewsToken>,
    val english: String = "",
    val bangla: String = "",
) {
    val text: String get() = tokens.joinToString("") { it.text }
}

/** How an article is shown: rewritten in easy Japanese, or the real NHK text. */
enum class ReadMode(val en: String, val bn: String) {
    EASY_N5("Easy N5", "সহজ"),
    EASY_N4("Easy N4", "একটু কঠিন"),
    ORIGINAL("Real news", "আসল খবর"),
}

/** A news story ready to read: every word can be tapped. */
data class NewsReading(
    val title: NewsSentence,
    val sentences: List<NewsSentence>,
    /** True when the meanings came from Gemini; false = offline, only words already in the app. */
    val fromAi: Boolean,
)

/**
 * Offline fallback: splits Japanese text into words the app already knows
 * (longest match first). Everything else is left as plain text.
 */
class Segmenter(words: List<VocabEntity>) {

    private val dict: Map<String, VocabEntity> = buildMap {
        // Kana-only spellings first, so a kanji spelling of the same word wins.
        words.forEach { w ->
            if (w.reading.length >= 2 && !w.reading.contains(' ')) putIfAbsent(w.reading, w)
        }
        words.forEach { w -> if (w.japanese.isNotBlank()) put(w.japanese, w) }
    }
    private val maxLen = dict.keys.maxOfOrNull { it.length }?.coerceAtMost(10) ?: 1

    fun segment(text: String): NewsSentence {
        val tokens = mutableListOf<NewsToken>()
        val plain = StringBuilder()
        fun flush() {
            if (plain.isNotEmpty()) tokens += NewsToken(plain.toString())
            plain.clear()
        }
        var i = 0
        while (i < text.length) {
            var match: VocabEntity? = null
            var len = minOf(maxLen, text.length - i)
            while (len >= 1) {
                val w = dict[text.substring(i, i + len)]
                // Single kana are too ambiguous (particles, word endings); single kanji are fine.
                if (w != null && (len > 1 || text[i].isKanji())) {
                    match = w
                    break
                }
                len--
            }
            if (match != null) {
                flush()
                tokens += NewsToken(
                    text = text.substring(i, i + len), kana = match.reading,
                    base = match.japanese, baseKana = match.reading,
                    english = match.meaning, bangla = match.meaningBn,
                )
                i += len
            } else {
                plain.append(text[i])
                i++
            }
        }
        flush()
        return NewsSentence(tokens)
    }

    /** Splits a paragraph into sentences after 。！？ and line breaks. */
    fun sentences(text: String): List<NewsSentence> =
        text.split(Regex("(?<=[。！？])|\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { segment(it) }

    private fun Char.isKanji() = this in '一'..'鿿' || this == '々'
}
