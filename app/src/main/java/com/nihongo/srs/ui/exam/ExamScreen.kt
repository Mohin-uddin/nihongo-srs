package com.nihongo.srs.ui.exam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.ui.TtsBanner
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.BigButton
import com.nihongo.srs.ui.components.BigOutlinedButton
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.EmptyState
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ProgressRing
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SessionProgress
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.theme.Extra

@Composable
fun ExamScreen(
    onBack: () -> Unit,
    vm: ExamViewModel = viewModel(factory = ExamViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tts by vm.ttsStatus.collectAsStateWithLifecycle()

    AppScreen(
        title = if (state.scope == ExamScope.TODAY) "Today's exam" else "Full exam",
        subtitle = if (state.scope == ExamScope.TODAY) "আজকের পরীক্ষা" else "পূর্ণ পরীক্ষা",
        onBack = onBack,
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TtsBanner(tts)
            when {
                state.isLoading -> Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.isEmpty -> EmptyState(
                    "📝",
                    if (state.scope == ExamScope.TODAY) "You haven't learned any words today yet"
                    else "Learn some words first",
                    if (state.scope == ExamScope.TODAY) "আজ এখনো কোনো শব্দ শেখা হয়নি। আগে কিছু শব্দ শিখুন।"
                    else "আগে কিছু শব্দ শিখুন, তারপর পরীক্ষা দিন।",
                ) { BigButton("Back", "ফিরে যান", onClick = onBack) }
                state.isFinished -> ExamResult(state, onBack)
                else -> QuestionView(state, vm)
            }
        }
    }
}

@Composable
private fun QuestionView(state: ExamUiState, vm: ExamViewModel) {
    val q = state.current ?: return
    SessionProgress(state.index, state.questions.size, label = "Question ${state.index + 1} / ${state.questions.size}")
    if (q.isRetry) {
        Pill("Try again • আবার চেষ্টা", color = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer)
    }

    SoftCard(padding = 24.dp) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            when (q.type) {
                QuestionType.JP_TO_MEANING -> {
                    Prompt("What does this mean?", "এর মানে কী?")
                    Text(q.word.japanese, fontSize = 48.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (state.selected != null) {
                        Text("${q.word.reading} · ${q.word.romaji}", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
                QuestionType.MEANING_TO_JP -> {
                    Prompt("Which is the Japanese?", "জাপানি কোনটি?")
                    Text(q.word.meaning, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(q.word.meaningBn, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                QuestionType.LISTEN -> {
                    Prompt("Listen and choose the meaning", "শুনে মানে বাছুন")
                    Surface(
                        onClick = vm::replay,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(84.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Play again",
                                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                        }
                    }
                    if (state.selected != null) {
                        Text("${q.word.japanese}  ${q.word.reading}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    q.options.forEachIndexed { i, option ->
        val showJp = q.type == QuestionType.MEANING_TO_JP
        OptionCard(
            letter = "ABCD".getOrElse(i) { ' ' }.toString(),
            main = if (showJp) option.japanese else option.meaning,
            sub = if (showJp) option.reading.takeIf { state.selected != null && it != option.japanese }
            else option.meaningBn,
            state = when {
                state.selected == null -> OptionState.NORMAL
                i == q.correctIndex -> OptionState.CORRECT
                i == state.selected -> OptionState.WRONG
                else -> OptionState.DIMMED
            },
            enabled = state.selected == null,
            onClick = { vm.select(i) },
            tag = "option$i"
        )
    }

    if (state.selected != null) {
        val correct = state.selected == q.correctIndex
        SoftCard(color = if (correct) Extra.correctSoft else Extra.wrongSoft, border = false, padding = 16.dp) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null,
                    tint = if (correct) Extra.correct else Extra.wrong)
                Spacer(Modifier.width(10.dp))
                if (correct) {
                    Bilingual("Correct!", "সঠিক!", color = Extra.correct)
                } else {
                    Bilingual(
                        "${q.word.japanese} (${q.word.reading}) = ${q.word.meaning}",
                        "${q.word.meaningBn} • এই শব্দটি শেষে আবার আসবে",
                        color = Extra.wrong,
                    )
                }
            }
        }
        BigButton("Next", "পরের প্রশ্ন", onClick = vm::next, modifier = Modifier.testTag("exam_next"))
    }
}

@Composable
private fun Prompt(en: String, bn: String) {
    Pill("$en • $bn", color = MaterialTheme.colorScheme.surfaceContainerHigh,
        textColor = MaterialTheme.colorScheme.onSurfaceVariant)
}

private enum class OptionState { NORMAL, CORRECT, WRONG, DIMMED }

@Composable
private fun OptionCard(
    letter: String,
    main: String,
    sub: String?,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    val accent = when (state) {
        OptionState.CORRECT -> Extra.correct
        OptionState.WRONG -> Extra.wrong
        else -> MaterialTheme.colorScheme.primary
    }
    val bg = when (state) {
        OptionState.CORRECT -> Extra.correctSoft
        OptionState.WRONG -> Extra.wrongSoft
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when (state) {
        OptionState.CORRECT, OptionState.WRONG -> BorderStroke(2.dp, accent)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(MaterialTheme.shapes.small)
                    .then(Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = MaterialTheme.shapes.small,
                    color = if (state == OptionState.NORMAL || state == OptionState.DIMMED)
                        MaterialTheme.colorScheme.primaryContainer else accent,
                    modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(letter, style = MaterialTheme.typography.labelLarge,
                            color = if (state == OptionState.NORMAL || state == OptionState.DIMMED)
                                MaterialTheme.colorScheme.primary else Color.White)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            val alpha = if (state == OptionState.DIMMED) 0.45f else 1f
            Column(Modifier.weight(1f)) {
                Text(main, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                if (!sub.isNullOrBlank()) {
                    Text(sub, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                }
            }
        }
    }
}

@Composable
private fun ExamResult(state: ExamUiState, onBack: () -> Unit) {
    val good = state.scorePercent >= 80
    SoftCard(padding = 24.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProgressRing(state.scorePercent / 100f, size = 140.dp, stroke = 12.dp,
                color = if (good) Extra.correct else MaterialTheme.colorScheme.primary) {
                Text("${state.scorePercent}%", style = MaterialTheme.typography.headlineLarge)
            }
            Bilingual(
                when {
                    state.scorePercent >= 90 -> "素晴らしい! Excellent!"
                    state.scorePercent >= 70 -> "Good job! Keep going."
                    else -> "Keep practicing, you'll get there."
                },
                when {
                    state.scorePercent >= 90 -> "অসাধারণ!"
                    state.scorePercent >= 70 -> "ভালো হয়েছে!"
                    else -> "আরও অনুশীলন করুন।"
                },
                enStyle = MaterialTheme.typography.titleLarge, align = TextAlign.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            )
            Text("${state.firstTryCorrect} / ${state.totalOriginal} correct on the first try • প্রথমবারে সঠিক",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
    if (state.wrongWords.isNotEmpty()) {
        SectionTitle("Words to practice", "যেগুলো ভুল হয়েছে, কাল আবার রিভিউতে আসবে")
        state.wrongWords.forEach { WrongWordRow(it) }
    }
    Spacer(Modifier.height(4.dp))
    BigButton("Back to Home", "হোমে ফিরুন", onClick = onBack)
}

@Composable
private fun WrongWordRow(v: VocabEntity) {
    SoftCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(v.japanese, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Bilingual("${v.reading} · ${v.meaning}", v.meaningBn, Modifier.weight(1f),
                enStyle = MaterialTheme.typography.bodyLarge)
        }
    }
}
