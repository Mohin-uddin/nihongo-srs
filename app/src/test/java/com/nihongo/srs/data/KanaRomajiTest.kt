package com.nihongo.srs.data

import org.junit.Assert.assertEquals
import org.junit.Test

class KanaRomajiTest {
    @Test fun basicWords() {
        assertEquals("mizu", KanaRomaji.convert("みず"))
        assertEquals("gakkou", KanaRomaji.convert("がっこう"))
        assertEquals("benkyou", KanaRomaji.convert("べんきょう"))
        assertEquals("chotto", KanaRomaji.convert("ちょっと"))
        assertEquals("matte", KanaRomaji.convert("まって"))
    }

    @Test fun katakanaAndLongVowel() {
        assertEquals("koohii", KanaRomaji.convert("コーヒー"))
        assertEquals("banguradeshu", KanaRomaji.convert("バングラデシュ"))
    }

    @Test fun sentencesUseParticleReadings() {
        assertEquals("watashi wa gakusei desu.", KanaRomaji.convert("わたし は がくせい です。"))
        assertEquals("ashita, eki e ikimasu.", KanaRomaji.convert("あした、えき へ いきます。"))
        assertEquals("mizu o nomimasu.", KanaRomaji.convert("みず を のみます。"))
    }
}
