package com.nihongo.srs.data

/**
 * Parses the bundled word / phrase lists in app/src/main/assets.
 *
 * Plain text, one item per line, fields separated by "|", lines starting with "#"
 * are comments. Keeping the content in text files makes it easy to add words
 * without touching Kotlin code. Pure Kotlin so it can be unit tested.
 */
object StarterContent {

    /** Bump this when the asset files change so existing installs get the new content. */
    const val VERSION = 3

    const val N5_FILE = "n5.txt"
    const val N4_FILE = "n4.txt"
    const val PHRASES_FILE = "phrases.txt"

    private fun lines(text: String) =
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }

    /** japanese|kana|romaji|en|bn|exJp|exKana|exEn|exBn */
    fun parseWords(text: String, level: String): List<VocabEntity> = lines(text).map { line ->
        val f = line.split("|").map { it.trim() }
        require(f.size == 9) { "Bad word line (${f.size} fields): $line" }
        VocabEntity(
            japanese = f[0], reading = f[1],
            romaji = f[2].ifEmpty { KanaRomaji.convert(f[1]) },
            meaning = f[3], meaningBn = f[4],
            example = f[5], exampleReading = f[6],
            exampleEn = f[7], exampleBn = f[8],
            level = level, type = VocabEntity.TYPE_WORD,
        )
    }.toList()

    /** category|japanese|kana|romaji|en|bn */
    fun parsePhrases(text: String): List<VocabEntity> = lines(text).map { line ->
        val f = line.split("|").map { it.trim() }
        require(f.size == 6) { "Bad phrase line (${f.size} fields): $line" }
        VocabEntity(
            category = f[0], japanese = f[1], reading = f[2],
            romaji = f[3].ifEmpty { KanaRomaji.convert(f[2]) },
            meaning = f[4], meaningBn = f[5],
            level = "Speaking", type = VocabEntity.TYPE_PHRASE,
        )
    }.toList()
}
