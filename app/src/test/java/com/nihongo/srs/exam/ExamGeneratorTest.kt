package com.nihongo.srs.exam

import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.ui.exam.ExamGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExamGeneratorTest {

    private fun word(i: Int) = VocabEntity(
        id = i.toLong(), japanese = "語$i", reading = "ご$i", romaji = "go$i",
        meaning = "meaning $i", meaningBn = "অর্থ $i"
    )

    private val all = (1..40).map(::word)
    private val gen = ExamGenerator(Random(1))

    @Test
    fun atLeastTwentyQuestionsEvenWithFewWords() {
        val qs = gen.build(all.take(5), all)
        assertEquals(ExamGenerator.MIN_QUESTIONS, qs.size)
        assertEquals(5, qs.map { it.word.id }.toSet().size)
    }

    @Test
    fun capsLongExams() {
        val many = (1..80).map(::word)
        assertEquals(ExamGenerator.MAX_QUESTIONS, gen.build(many, many).size)
    }

    @Test
    fun optionsContainTheAnswerOnce() {
        gen.build(all.take(10), all).forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(q.word, q.options[q.correctIndex])
            assertEquals(4, q.options.map { it.id }.toSet().size)
        }
    }

    @Test
    fun retryAsksSameWordAnotherWay() {
        val q = gen.build(all.take(3), all).first()
        val r = gen.retry(q, all)
        assertEquals(q.word, r.word)
        assertTrue(r.isRetry)
        assertTrue(r.type != q.type)
    }
}
