package com.nihongo.srs.data

/** One kana with its romaji. */
data class Kana(val char: String, val romaji: String)

/**
 * The hiragana chart (basic + dakuten). Katakana is derived from it.
 * `null` cells keep the 5-column a-i-u-e-o layout (e.g. the や and わ rows).
 */
object KanaData {

    private fun row(vararg pairs: String?): List<Kana?> =
        pairs.map { p -> p?.split(":")?.let { Kana(it[0], it[1]) } }

    val hiraganaRows: List<List<Kana?>> = listOf(
        row("あ:a", "い:i", "う:u", "え:e", "お:o"),
        row("か:ka", "き:ki", "く:ku", "け:ke", "こ:ko"),
        row("さ:sa", "し:shi", "す:su", "せ:se", "そ:so"),
        row("た:ta", "ち:chi", "つ:tsu", "て:te", "と:to"),
        row("な:na", "に:ni", "ぬ:nu", "ね:ne", "の:no"),
        row("は:ha", "ひ:hi", "ふ:fu", "へ:he", "ほ:ho"),
        row("ま:ma", "み:mi", "む:mu", "め:me", "も:mo"),
        row("や:ya", null, "ゆ:yu", null, "よ:yo"),
        row("ら:ra", "り:ri", "る:ru", "れ:re", "ろ:ro"),
        row("わ:wa", null, null, null, "を:wo"),
        row("ん:n", null, null, null, null),
        row("が:ga", "ぎ:gi", "ぐ:gu", "げ:ge", "ご:go"),
        row("ざ:za", "じ:ji", "ず:zu", "ぜ:ze", "ぞ:zo"),
        row("だ:da", "ぢ:ji", "づ:zu", "で:de", "ど:do"),
        row("ば:ba", "び:bi", "ぶ:bu", "べ:be", "ぼ:bo"),
        row("ぱ:pa", "ぴ:pi", "ぷ:pu", "ぺ:pe", "ぽ:po"),
    )

    val katakanaRows: List<List<Kana?>> = hiraganaRows.map { r ->
        r.map { k -> k?.copy(char = toKatakana(k.char)) }
    }

    fun all(katakana: Boolean): List<Kana> =
        (if (katakana) katakanaRows else hiraganaRows).flatten().filterNotNull()

    private fun toKatakana(s: String) = buildString { for (c in s) append(if (c in 'ぁ'..'ゖ') c + 0x60 else c) }
}
