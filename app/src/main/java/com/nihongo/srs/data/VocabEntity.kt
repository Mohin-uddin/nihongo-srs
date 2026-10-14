package com.nihongo.srs.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One Japanese word or phrase.
 *
 * `meaning` holds the English meaning and `meaningBn` the Bengali (বাংলা) meaning,
 * so a Bangla speaker can learn through both languages.
 *
 * The unique index on (japanese, reading) lets us preload the N5 list many times
 * without creating duplicates (inserts use OnConflictStrategy.IGNORE).
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

    /** Furigana reading in kana, e.g. "みず". */
    val reading: String,

    /** Romaji so beginners can read it before they know kana, e.g. "mizu". */
    val romaji: String,

    /** English meaning, e.g. "water". */
    val meaning: String,

    /** Bengali meaning, e.g. "পানি / জল". */
    val meaningBn: String,

    /** JLPT level, e.g. "N5". */
    val level: String = "N5",

    /** WORD for vocabulary, PHRASE for everyday speaking phrases. */
    val type: String = TYPE_WORD,
) {
    companion object {
        const val TYPE_WORD = "WORD"
        const val TYPE_PHRASE = "PHRASE"
    }
}
