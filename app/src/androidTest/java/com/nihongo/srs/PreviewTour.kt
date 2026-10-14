package com.nihongo.srs

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

/**
 * Not a pass/fail test: it walks through every screen of the real app on an
 * emulator (GitHub Actions) and saves a screenshot of each, so a new version
 * can be checked in the browser before installing it on a phone.
 */
@RunWith(AndroidJUnit4::class)
class PreviewTour {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private var shotNumber = 0

    private fun shell(cmd: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd)
        FileInputStream(pfd.fileDescriptor).use { it.readBytes() } // wait until it finishes
        pfd.close()
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        Thread.sleep(800) // let animations settle
        shotNumber++
        shell("screencap -p $DIR/%02d_%s.png".format(shotNumber, name))
    }

    private fun waitFor(text: String) {
        rule.waitUntil(20_000) {
            rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun node(text: String): SemanticsNodeInteraction {
        waitFor(text)
        return rule.onAllNodesWithText(text, substring = true)[0]
    }

    private fun tap(text: String) {
        val n = node(text)
        runCatching { n.performScrollTo() }
        n.performClick()
        rule.waitForIdle()
    }

    private fun tapTag(tag: String) {
        rule.waitUntil(20_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        val n = rule.onAllNodesWithTag(tag)[0]
        runCatching { n.performScrollTo() }
        n.performClick()
        rule.waitForIdle()
    }

    private fun back() {
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitForIdle()
    }

    /** Make the words learned in this tour due today, so the Review screen has cards. */
    private fun makeLearnedWordsDue() = runBlocking {
        val dao = (ApplicationProvider.getApplicationContext<NihongoApp>()).database.vocabDao()
        dao.getLearned().mapNotNull { it.progress }.forEach { dao.upsertProgress(it.copy(nextReview = 0)) }
    }

    /** True once a "N new" counter above zero is on screen, i.e. the word lists are loaded. */
    private fun wordsLoaded(): Boolean =
        rule.onAllNodesWithText(" new", substring = true).fetchSemanticsNodes().any { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty()
                .any { Regex("""[1-9]\d* new""").matches(it.text) }
        }

    @Test
    fun tour() {
        shell("mkdir -p $DIR")

        // Home (first launch fills the database, so wait for it)
        rule.waitUntil(30_000) { wordsLoaded() }
        shot("home")
        runCatching { node("Daily reminder").performScrollTo() }
        shot("home_bottom")

        // Learn new words
        tapTag("learn_N5")
        waitFor("Learned, review tomorrow")
        shot("learn_new_word")
        repeat(4) { tapTag("learned"); Thread.sleep(300) }
        back()

        // Review (with reverse cards)
        makeLearnedWordsDue()
        Thread.sleep(1500)
        runCatching { rule.onNodeWithTag("start_review").performScrollTo() }
        tapTag("start_review")
        waitFor("show answer")
        shot("review_question")
        tapTag("show_answer")
        shot("review_answer")
        tapTag("knew")
        waitFor("show answer")
        shot("review_question_2")
        back()

        // Words tab + sentences
        tapTag("tab_words")
        shot("my_words")
        tap("Sentences • বাক্য")
        shot("my_sentences")

        // Practice tab
        tapTag("tab_practice")
        shot("practice")

        // Exam
        tapTag("practice_exam_today")
        waitFor("Question 1")
        shot("exam_question")
        rule.onNodeWithTag("option0").performClick()
        rule.waitForIdle()
        shot("exam_answer")
        back()

        // Daily conversation
        tapTag("practice_speak")
        shot("conversation")
        back()

        // Kana
        tapTag("practice_kana")
        shot("kana_chart")
        tapTag("kana_quiz_start")
        shot("kana_quiz")
        rule.onNodeWithTag("kana0").performClick()
        rule.waitForIdle()
        shot("kana_quiz_answer")
        back() // close quiz
        back()

        // Progress tab
        tapTag("tab_progress")
        waitFor("Course completion")
        shot("progress")
        runCatching { node("Recent exams").performScrollTo() }
        shot("progress_bottom")

        // Manage
        tapTag("tab_words")
        tapTag("manage")
        shot("manage")
        back()
    }

    companion object {
        const val DIR = "/sdcard/Pictures/nihongo-preview"
    }
}
