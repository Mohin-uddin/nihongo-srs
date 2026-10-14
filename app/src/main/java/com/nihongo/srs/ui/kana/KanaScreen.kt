package com.nihongo.srs.ui.kana

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.KanaData
import com.nihongo.srs.ui.TtsBanner
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.BigButton
import com.nihongo.srs.ui.components.BigOutlinedButton
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.ChoiceChip
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ProgressRing
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SessionProgress
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.theme.Extra

@Composable
fun KanaScreen(
    onBack: () -> Unit,
    vm: KanaViewModel = viewModel(factory = KanaViewModel.Factory),
) {
    val katakana by vm.katakana.collectAsStateWithLifecycle()
    val quiz by vm.quiz.collectAsStateWithLifecycle()
    val tts by vm.ttsStatus.collectAsStateWithLifecycle()

    AppScreen(
        title = if (quiz == null) "Kana chart" else "Kana quiz",
        subtitle = if (quiz == null) "হিরাগানা ও কাতাকানা" else "কানা পরীক্ষা",
        onBack = { if (quiz != null) vm.closeQuiz() else onBack() },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TtsBanner(tts)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("ひらがな Hiragana", selected = !katakana, enabled = quiz == null,
                    onClick = { vm.katakana.value = false })
                ChoiceChip("カタカナ Katakana", selected = katakana, enabled = quiz == null,
                    onClick = { vm.katakana.value = true })
            }
            val q = quiz
            if (q == null) Chart(katakana, vm) else Quiz(q, vm)
        }
    }
}

@Composable
private fun Chart(katakana: Boolean, vm: KanaViewModel) {
    Text(
        "Tap a letter to hear it. Learn hiragana first, then katakana (used for foreign words).\n" +
            "অক্ষরে চাপ দিলে শুনবেন। আগে হিরাগানা, পরে কাতাকানা শিখুন।",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    BigButton("Start kana quiz (20)", "কানা পরীক্ষা শুরু", onClick = vm::startQuiz, icon = Icons.Rounded.Quiz,
        modifier = Modifier.testTag("kana_quiz_start"))
    SoftCard(padding = 12.dp) {
        val rows = if (katakana) KanaData.katakanaRows else KanaData.hiraganaRows
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    Box(Modifier.weight(1f).aspectRatio(0.9f)) {
                        if (k != null) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small)
                                    .clickable { vm.speak(k) },
                            ) {
                                Column(
                                    Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(k.char, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Text(k.romaji, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Quiz(q: KanaQuizState, vm: KanaViewModel) {
    if (q.isFinished) {
        SoftCard(padding = 24.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressRing(q.percent / 100f, size = 140.dp, stroke = 12.dp,
                    color = if (q.percent >= 80) Extra.correct else MaterialTheme.colorScheme.primary) {
                    Text("${q.percent}%", style = MaterialTheme.typography.headlineLarge)
                }
                Text("${q.firstTryCorrect} / ${q.total} correct on the first try • প্রথমবারে সঠিক",
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BigButton("Try again", "আবার চেষ্টা", onClick = vm::startQuiz)
        BigOutlinedButton("Back to chart", "চার্টে ফিরুন", onClick = vm::closeQuiz)
        return
    }
    val cur = q.current ?: return
    SessionProgress(q.index, q.questions.size, label = "Question ${q.index + 1} / ${q.questions.size}")
    if (cur.isRetry) {
        Pill("Try again • আবার", color = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer)
    }
    SoftCard(padding = 20.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(cur.kana.char, fontSize = 96.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("How do you read it? • এটা কীভাবে পড়ে?", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    // 2 x 2 answer grid
    cur.options.chunked(2).forEachIndexed { r, pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEachIndexed { c, opt ->
                val i = r * 2 + c
                val isAnswer = opt == cur.kana.romaji
                val picked = opt == q.selected
                val accent = when {
                    q.selected == null -> null
                    isAnswer -> Extra.correct
                    picked -> Extra.wrong
                    else -> null
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = when {
                        accent == Extra.correct -> Extra.correctSoft
                        accent == Extra.wrong -> Extra.wrongSoft
                        else -> MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(if (accent != null) 2.dp else 1.dp,
                        accent ?: MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp).clip(MaterialTheme.shapes.medium)
                        .clickable(enabled = q.selected == null) { vm.select(opt) }.testTag("kana$i"),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                        Text(opt, style = MaterialTheme.typography.headlineSmall,
                            color = accent ?: MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
    if (q.selected != null) {
        BigButton("Next", "পরের প্রশ্ন", onClick = vm::next)
    }
}
