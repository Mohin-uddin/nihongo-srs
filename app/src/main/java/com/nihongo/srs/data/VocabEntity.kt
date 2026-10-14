package com.nihongo.srs.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One Japanese word or phrase.
 *
 * `meaning` holds the English meaning and `meaningBn` the Bengali (বাংলা) meaning,
 * so a Bangla speaker can learn through both languages. Words also carry one
 * example sentence that uses the word, in Japanese, kana, English and Bangla.
 *
 * The unique index on (japanese, reading) prevents duplicate words.
 */
@Entity(
    tableName = "vocab",
    indices = [Index(value = ["japanese", "reading"], unique = true)]
)
data class VocabEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Kanji / Kana as written, e.g. "水". */
    val japanese: String,

    /** Furigana reading in kana, e.g. "みず". Phrases use spaces between words. */
    val reading: String,

    /** Romaji so beginners can read it before they know kana, e.g. "mizu". */
    val romaji: String,

    /** English meaning, e.g. "water". */
    val meaning: String,

    /** Bengali meaning, e.g. "পানি / জল". */
    val meaningBn: String,

    /** JLPT level: "N5", "N4", or "Speaking" for phrases. */
    val level: String = "N5",

    /** WORD for vocabulary, PHRASE for everyday speaking phrases. */
    val type: String = TYPE_WORD,

    // ---- Added in DB version 2 ----

    /** Example sentence using this word, e.g. "水を飲みます。" */
    @ColumnInfo(defaultValue = "") val example: String = "",
    /** Example in kana with spaces between words, e.g. "みず を のみます。" */
    @ColumnInfo(defaultValue = "") val exampleReading: String = "",
    @ColumnInfo(defaultValue = "") val exampleEn: String = "",
    @ColumnInfo(defaultValue = "") val exampleBn: String = "",
    /** Topic for phrases, e.g. "Shopping • কেনাকাটা". */
    @ColumnInfo(defaultValue = "") val category: String = "",
) {
    companion object {
        const val TYPE_WORD = "WORD"
        const val TYPE_PHRASE = "PHRASE"

        /** Level given to words the user adds in Manage. */
        const val LEVEL_MINE = "Mine"
    }
}

/** Romaji for the example sentence (generated from its kana). */
val VocabEntity.exampleRomaji: String
    get() = if (exampleReading.isBlank()) "" else KanaRomaji.convert(exampleReading)

/** "English • বাংলা" in one line. */
val VocabEntity.meaningBoth: String
    get() = listOf(meaning, meaningBn).filter { it.isNotBlank() }.joinToString(" • ")
