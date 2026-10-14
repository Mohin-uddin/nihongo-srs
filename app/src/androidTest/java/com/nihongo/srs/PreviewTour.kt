package com.nihongo.srs

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    private fun back() {
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitForIdle()
    }

    /** Make the words learned in this tour due today, so the Review screen has cards. */
    private fun makeLearnedWordsDue() = runBlocking {
        val dao = (ApplicationProvider.getApplicationContext<NihongoApp>()).database.vocabDao()
        dao.getLearned().mapNotNull { it.progress }.forEach { dao.upsertProgress(it.copy(nextReview = 0)) }
    }

    @Test
    fun tour() {
        shell("mkdir -p $DIR")

        // Home
        waitFor("N5\n")
        shot("home")
        runCatching { node("Daily 25-minute plan").performScrollTo() }
        shot("home_bottom")
        runCatching { node("Words to review today").performScrollTo() }

        // Learn new words
        tap("N5\n")
        waitFor("Learned, review tomorrow")
        shot("learn_new_word")
        repeat(4) { tap("Learned, review tomorrow"); Thread.sleep(300) }
        back()

        // Review (with reverse cards)
        makeLearnedWordsDue()
        waitFor("Start Review")
        Thread.sleep(1500)
        tap("Start Review")
        waitFor("show answer")
        shot("review_question")
        tap("show answer")
        shot("review_answer")
        tap("I Knew It")
        waitFor("show answer")
        shot("review_question_2")
        back()

        // My words + sentences
        tap("My words & sentences")
        shot("my_words")
        tap("Sentences • বাক্য")
        shot("my_sentences")
        back()

        // Exam
        tap("Today's exam")
        waitFor("Question 1")
        shot("exam_question")
        rule.onNodeWithTag("option0").performClick()
        shot("exam_answer")
        back()

        // Daily conversation
        tap("Daily conversation")
        shot("conversation")
        back()

        // Kana
        tap("Hiragana & Katakana")
        shot("kana_chart")
        tap("Start kana quiz")
        shot("kana_quiz")
        rule.onNodeWithTag("kana0").performClick()
        shot("kana_quiz_answer")
        back() // close quiz
        back()

        // Progress
        tap("My progress")
        waitFor("Course completion")
        shot("progress")
        runCatching { node("Recent exams").performScrollTo() }
        shot("progress_bottom")
        back()

        // Manage
        tap("Add / Manage words")
        shot("manage")
        back()
    }

    companion object {
        const val DIR = "/sdcard/Pictures/nihongo-preview"
    }
}
