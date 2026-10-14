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
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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

    private fun has(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun waitTag(tag: String, ms: Long = 20_000) = runCatching { rule.waitUntil(ms) { has(tag) } }.isSuccess

    /** System back (closes sheets and dialogs too). */
    private fun systemBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
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

        // First launch: the guide explains every tab and asks for a name
        waitTag("guide_start", 30_000)
        shot("guide")
        rule.onNodeWithTag("guide_name").performTextInput("Mohin")
        tapTag("guide_start")

        // Today tab (first launch fills the database, so wait for it)
        rule.waitUntil(30_000) { wordsLoaded() }
        shot("home")
        runCatching { node("নিচের ট্যাবগুলো").performScrollTo() }
        shot("home_bottom")

        // Learn new words
        tapTag("learn_N5")
        waitFor("Learned, review tomorrow")
        shot("learn_new_word")
        repeat(5) { tapTag("learned"); Thread.sleep(300) }
        // A mis-tap: go back, and the 5th word is new again.
        tapTag("previous_card")
        Thread.sleep(800)
        shot("learn_went_back")
        back()

        // Review (with reverse cards)
        makeLearnedWordsDue()
        Thread.sleep(1500)
        tapTag("start_review")
        waitFor("show answer")
        shot("review_question")
        tapTag("show_answer")
        shot("review_answer")
        tapTag("knew")
        waitFor("show answer")
        shot("review_question_2")
        back()

        // Exam from step 3
        tapTag("home_exam")
        waitFor("Question 1")
        shot("exam_question")
        rule.onNodeWithTag("option0").performClick()
        rule.waitForIdle()
        shot("exam_answer")
        back()

        // News tab: list, a story with tappable words, the word sheet, radio
        tapTag("tab_news")
        if (waitTag("news_item_0", 30_000)) {
            shot("news_list")
            tapTag("news_item_0")
            waitTag("news_title", 30_000)
            shot("news_article")
            runCatching {
                tapTag("token_-1_0")
                Thread.sleep(800)
                shot("news_word")
                systemBack()
            }
            // The sheet may close or the screen may go back, depending on the Android version.
            if (!has("news_radio")) back()
        } else {
            shot("news_list_offline")
        }
        tapTag("news_radio")
        if (waitTag("radio_episode_0", 30_000)) {
            shot("radio")
            tapTag("radio_episode_0")
            Thread.sleep(4000)
            shot("radio_playing")
            runCatching { rule.onNodeWithTag("radio_play").performClick() }
        }
        back()

        // Play tab: add a second player, then a quiz battle
        tapTag("tab_players")
        shot("scoreboard")
        tapTag("players_add")
        rule.onNodeWithTag("player_name").performTextInput("Wife")
        tap("Save • সেভ")
        shot("scoreboard_two_players")
        tapTag("players_battle")
        shot("battle_setup")
        tapTag("battle_start")
        var turns = 0
        while (turns < 20 && !has("battle_again") && waitTag("battle_ready", 10_000)) {
            tapTag("battle_ready")
            waitTag("battle_option0")
            if (turns == 0) shot("battle_question")
            // Mix right and wrong answers so the scores differ.
            val pick = if (turns % 3 == 1) 1 else 0
            rule.onNodeWithTag("battle_option$pick").performClick()
            rule.waitForIdle()
            if (turns == 1) shot("battle_answer")
            tapTag("battle_next")
            turns++
        }
        waitTag("battle_again")
        shot("battle_result")
        back()
        shot("scoreboard_after_battle")

        // Words tab: the whole word book, search in Bangla, sentences
        tapTag("tab_words")
        shot("my_words")
        rule.onNodeWithTag("words_search").performTextInput("বন্ধু")
        rule.waitForIdle()
        shot("search_bangla")
        rule.onNodeWithTag("words_search").performTextClearance()
        rule.waitForIdle()
        tap("Sentences • বাক্য")
        shot("my_sentences")

        // More tab
        tapTag("tab_practice")
        shot("more")
        tapTag("practice_ask")
        shot("ask_banglish")
        back()
        tapTag("practice_speak")
        shot("conversation")
        back()
        tapTag("practice_kana")
        shot("kana_chart")
        tapTag("kana_quiz_start")
        shot("kana_quiz")
        rule.onNodeWithTag("kana0").performClick()
        rule.waitForIdle()
        shot("kana_quiz_answer")
        back() // close quiz
        back()
        tapTag("more_progress")
        waitFor("Course completion")
        shot("progress")
        runCatching { node("Recent exams").performScrollTo() }
        shot("progress_bottom")
        back()
        tapTag("more_manage")
        shot("manage")
        back()
    }

    companion object {
        const val DIR = "/sdcard/Pictures/nihongo-preview"
    }
}
