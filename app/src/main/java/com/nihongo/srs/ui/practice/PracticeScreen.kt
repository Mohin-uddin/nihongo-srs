package com.nihongo.srs.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.MenuTile
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.exam.ExamScope
import com.nihongo.srs.ui.theme.Extra

/** All practice modes in one place, plus the daily study plan. */
@Composable
fun PracticeScreen(
    onExam: (ExamScope) -> Unit,
    onSpeak: () -> Unit,
    onKana: () -> Unit,
    onReview: () -> Unit,
    onAsk: () -> Unit,
) {
    AppScreen(title = "Practice", subtitle = "অনুশীলন") { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Test yourself", "নিজেকে যাচাই করুন")
            MenuTile(Icons.Rounded.Quiz, "Today's exam", "আজকের শেখা শব্দ থেকে ২০+ প্রশ্ন",
                { onExam(ExamScope.TODAY) }, Modifier.testTag("practice_exam_today"))
            MenuTile(Icons.Rounded.AllInclusive, "Full exam", "সব শেখা শব্দ থেকে পরীক্ষা",
                { onExam(ExamScope.ALL) }, Modifier.testTag("practice_exam_all"),
                tint = Extra.correct, tintBackground = Extra.correctSoft)
            MenuTile(Icons.Rounded.Replay, "Review due words", "আজকের রিভিউ কার্ড",
                onReview, tint = MaterialTheme.colorScheme.secondary,
                tintBackground = MaterialTheme.colorScheme.secondaryContainer)

            SectionTitle("Speak and read", "বলা ও পড়া")
            MenuTile(Icons.Rounded.AutoAwesome, "Ask in Banglish", "বাংলিশে লিখুন, জাপানি বাক্য দেখুন",
                onAsk, Modifier.testTag("practice_ask"),
                tint = MaterialTheme.colorScheme.secondary, tintBackground = MaterialTheme.colorScheme.secondaryContainer)
            MenuTile(Icons.Rounded.Forum, "Daily conversation", "প্রতিদিনের কথা, মাইকে যাচাই",
                onSpeak, Modifier.testTag("practice_speak"),
                tint = MaterialTheme.colorScheme.secondary, tintBackground = MaterialTheme.colorScheme.secondaryContainer)
            MenuTile(Icons.Rounded.Translate, "Hiragana & Katakana", "কানা চার্ট ও কুইজ",
                onKana, Modifier.testTag("practice_kana"),
                tint = Extra.streak, tintBackground = Extra.streak.copy(alpha = 0.15f))

            SectionTitle("Daily plan", "প্রতিদিন ২৫ মিনিট")
            SoftCard {
                PlanStep(1, "Review due cards. Answer out loud before you check.", "উত্তর দেখার আগে জোরে বলুন")
                PlanStep(2, "Learn 10 new words and read their sentences aloud.", "নতুন ১০টি শব্দ ও বাক্য জোরে পড়ুন")
                PlanStep(3, "Take today's exam.", "আজকের পরীক্ষা দিন")
                PlanStep(4, "Conversation: listen, repeat 3 times, check with the mic.", "শুনুন, ৩ বার বলুন, মাইকে যাচাই করুন")
            }
            SoftCard(color = MaterialTheme.colorScheme.primaryContainer, border = false) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Bilingual(
                        "Why do I forget? Seeing a word feels easy, but recalling it yourself is what makes it stick. " +
                            "That's why some cards show the Bangla and ask for the Japanese.",
                        "দেখলে মনে হয় পারি, কিন্তু নিজে মনে করে বলতে পারলেই শেখা পাকা হয়।",
                        enStyle = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PlanStep(n: Int, en: String, bn: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Text("$n", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(12.dp))
        Bilingual(en, bn, Modifier.weight(1f), enStyle = MaterialTheme.typography.bodyLarge)
    }
}
