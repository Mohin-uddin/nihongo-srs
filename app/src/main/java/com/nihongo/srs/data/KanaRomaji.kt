package com.nihongo.srs.data

/**
 * Converts kana (hiragana / katakana) to Hepburn-style romaji, so beginners can
 * read example sentences before they know kana.
 *
 * Spaces in the input are kept, so "わたし は がくせい です" -> "watashi wa gakusei desu".
 * A standalone は / へ / を token is read as the particle: wa / e / o.
 * Kanji or other characters are copied unchanged.
 */
object KanaRomaji {

    private val digraphs = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho", "しぇ" to "she",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho", "ちぇ" to "che",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo", "じぇ" to "je",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
        "てぃ" to "ti", "でぃ" to "di", "うぃ" to "wi", "うぇ" to "we", "うぉ" to "wo",
        "ゔぁ" to "va", "ゔぃ" to "vi", "ゔぇ" to "ve", "ゔぉ" to "vo",
    )

    private val monographs = mapOf(
        "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
        "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
        "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
        "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
        "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
        "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
        "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
        "や" to "ya", "ゆ" to "yu", "よ" to "yo",
        "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
        "わ" to "wa", "を" to "o", "ん" to "n",
        "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
        "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
        "だ" to "da", "ぢ" to "ji", "づ" to "zu", "で" to "de", "ど" to "do",
        "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
        "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
        "ゔ" to "vu",
        "ぁ" to "a", "ぃ" to "i", "ぅ" to "u", "ぇ" to "e", "ぉ" to "o",
        "ゃ" to "ya", "ゅ" to "yu", "ょ" to "yo",
        "、" to ",", "。" to ".", "？" to "?", "！" to "!", "　" to " ",
    )

    private val particles = mapOf("は" to "wa", "へ" to "e", "を" to "o")

    fun convert(kana: String): String =
        kana.split(' ').joinToString(" ") { token ->
            particles[token] ?: convertToken(toHiragana(token))
        }
            .replace(Regex("\\s*,\\s*"), ", ")
            .replace(Regex("\\s+([.?!])"), "$1")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun convertToken(s: String): String {
        val out = StringBuilder()
        var i = 0
        var doubleNext = false
        while (i < s.length) {
            val c = s[i]
            when {
                c == 'っ' -> { doubleNext = true; i++; continue }
                c == 'ー' -> { out.lastVowel()?.let { out.append(it) }; i++; continue }
            }
            val two = if (i + 1 < s.length) s.substring(i, i + 2) else null
            val roman = two?.let { digraphs[it] }?.also { i += 2 }
                ?: monographs[c.toString()]?.also { i++ }
                ?: c.toString().also { i++ }
            if (doubleNext) {
                out.append(if (roman.startsWith("ch")) "t" else roman.take(1))
                doubleNext = false
            }
            out.append(roman)
        }
        return out.toString()
    }

    private fun StringBuilder.lastVowel(): Char? =
        lastOrNull()?.takeIf { it in "aeiou" }

    /** Katakana -> hiragana (ー stays as the long-vowel mark). */
    private fun toHiragana(s: String): String = buildString {
        for (c in s) append(if (c in 'ァ'..'ヶ') c - 0x60 else c)
    }
}
