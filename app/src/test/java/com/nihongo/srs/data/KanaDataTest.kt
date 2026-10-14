package com.nihongo.srs.data

import org.junit.Assert.assertEquals
import org.junit.Test

class KanaDataTest {
    @Test fun chartSizes() {
        assertEquals(71, KanaData.all(katakana = false).size)
        assertEquals(71, KanaData.all(katakana = true).size)
        assertEquals("カ", KanaData.katakanaRows[1][0]!!.char)
    }
}
