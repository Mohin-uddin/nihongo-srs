package com.nihongo.srs.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the bundled word lists parse and have no duplicates or missing fields. */
class StarterContentTest {

    private fun asset(name: String) = File("src/main/assets/$name").readText()

    @Test
    fun wordListsParseCompletely() {
        val n5 = StarterContent.parseWords(asset(StarterContent.N5_FILE), "N5")
        val n4 = StarterContent.parseWords(asset(StarterContent.N4_FILE), "N4")
        assertTrue("N5 size ${n5.size}", n5.size >= 600)
        assertTrue("N4 size ${n4.size}", n4.size >= 100)
        (n5 + n4).forEach { v ->
            listOf(v.japanese, v.reading, v.romaji, v.meaning, v.meaningBn,
                v.example, v.exampleReading, v.exampleEn, v.exampleBn).forEach {
                assertTrue("Empty field in ${v.japanese}", it.isNotBlank())
            }
        }
        val all = n5 + n4
        assertEquals("Duplicate words", all.size, all.map { it.japanese }.toSet().size)
    }

    @Test
    fun phrasesParseWithCategories() {
        val phrases = StarterContent.parsePhrases(asset(StarterContent.PHRASES_FILE))
        assertTrue(phrases.size >= 60)
        assertTrue(phrases.all { it.category.isNotBlank() && it.romaji.isNotBlank() })
        assertEquals(phrases.size, phrases.map { it.japanese }.toSet().size)
    }
}
