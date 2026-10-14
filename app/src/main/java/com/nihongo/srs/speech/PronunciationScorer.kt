package com.nihongo.srs.speech

/**
 * Scores how close what the recognizer heard is to the target phrase.
 *
 * We compare against both the written form (kanji) and the kana reading,
 * after removing spaces and punctuation, and take the best candidate.
 * Score is 0..100 based on character edit distance.
 */
object PronunciationScorer {

    private val stripRegex = Regex("[\\s、。！？!?.,・「」ー〜~]")

    fun score(heard: List<String>, japanese: String, reading: String): Int {
        val targets = listOf(normalize(japanese), normalize(reading)).filter { it.isNotEmpty() }
        if (targets.isEmpty()) return 0
        return heard.maxOfOrNull { h ->
            val n = normalize(h)
            targets.maxOf { t -> similarity(n, t) }
        } ?: 0
    }

    /** Lowercase, remove punctuation/spaces, convert katakana to hiragana. */
    fun normalize(s: String): String = buildString {
        for (c in s.lowercase().replace(stripRegex, "")) {
            append(if (c in 'ァ'..'ヶ') c - 0x60 else c)
        }
    }

    private fun similarity(a: String, b: String): Int {
        if (a.isEmpty() && b.isEmpty()) return 100
        val dist = levenshtein(a, b)
        val maxLen = maxOf(a.length, b.length)
        return ((1.0 - dist.toDouble() / maxLen) * 100).toInt().coerceIn(0, 100)
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[b.length]
    }
}
