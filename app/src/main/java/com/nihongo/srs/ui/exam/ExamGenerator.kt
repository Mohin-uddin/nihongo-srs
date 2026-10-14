package com.nihongo.srs.ui.exam

import com.nihongo.srs.data.VocabEntity
import kotlin.random.Random

enum class QuestionType {
    /** See Japanese, pick the meaning. */
    JP_TO_MEANING,
    /** See the meaning, pick the Japanese (active recall). */
    MEANING_TO_JP,
    /** Hear the word, pick the meaning (listening). */
    LISTEN,
}

data class Question(
    val word: VocabEntity,
    val type: QuestionType,
    val options: List<VocabEntity>,
    val correctIndex: Int,
    /** True when this is a repeat of a question answered wrongly earlier. */
    val isRetry: Boolean = false,
)

/**
 * Builds multiple-choice exams. Pure Kotlin so it can be unit tested.
 *
 * - At least [MIN_QUESTIONS] questions; if you know fewer words, each word is
 *   asked again with a different question type.
 * - At most [MAX_QUESTIONS] so one exam stays short enough to finish.
 */
class ExamGenerator(private val random: Random = Random.Default) {

    companion object {
        const val MIN_QUESTIONS = 20
        const val MAX_QUESTIONS = 50
        private const val OPTION_COUNT = 4
    }

    /**
     * @param pool words to test (the learned words).
     * @param distractorPool all words, used for the wrong options.
     */
    fun build(pool: List<VocabEntity>, distractorPool: List<VocabEntity>): List<Question> {
        if (pool.isEmpty()) return emptyList()
        val count = pool.size.coerceIn(MIN_QUESTIONS, MAX_QUESTIONS)
        val types = QuestionType.entries
        val questions = mutableListOf<Question>()
        var round = 0
        while (questions.size < count) {
            val shuffled = pool.shuffled(random)
            for ((i, word) in shuffled.withIndex()) {
                if (questions.size >= count) break
                // Rotate the question type so a repeated word is asked a different way.
                val type = types[(i + round) % types.size]
                questions += question(word, type, distractorPool)
            }
            round++
        }
        return questions
    }

    /** A new question for the same word, used to re-ask a wrong answer later. */
    fun retry(previous: Question, distractorPool: List<VocabEntity>): Question {
        val types = QuestionType.entries
        val next = types[(previous.type.ordinal + 1) % types.size]
        return question(previous.word, next, distractorPool).copy(isRetry = true)
    }

    fun question(word: VocabEntity, type: QuestionType, distractorPool: List<VocabEntity>): Question {
        val wrong = distractorPool
            .filter { it.id != word.id && it.japanese != word.japanese && it.meaning != word.meaning }
            .shuffled(random)
            .distinctBy { it.meaning }
            .take(OPTION_COUNT - 1)
        val options = (wrong + word).shuffled(random)
        return Question(word, type, options, options.indexOf(word))
    }
}
