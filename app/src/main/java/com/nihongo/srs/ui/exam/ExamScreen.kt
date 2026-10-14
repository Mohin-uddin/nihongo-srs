package com.nihongo.srs.ui.exam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.meaningBoth
import com.nihongo.srs.ui.TtsBanner
import com.nihongo.srs.ui.theme.CorrectGreen
import com.nihongo.srs.ui.theme.ForgotRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    onBack: () -> Unit,
    vm: ExamViewModel = viewModel(factory = ExamViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tts by vm.ttsStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.scope == ExamScope.TODAY) "Today's exam • আজকের পরীক্ষা"
                    else "Full exam • পূর্ণ পরীক্ষা")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TtsBanner(tts)
            when {
                state.isLoading -> Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.isEmpty -> {
                    Text(
                        if (state.scope == ExamScope.TODAY)
                            "You haven't learned or reviewed any words today yet. Learn some first, then come back!\n" +
                                "আজ এখনো কোনো শব্দ শেখা হয়নি। আগে কিছু শব্দ শিখুন, তারপর পরীক্ষা দিন।"
                        else "Learn some words first, then take the exam.\nআগে কিছু শব্দ শিখুন।",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onBack) { Text("Back • ফিরে যান") }
                }
                state.isFinished -> ExamResult(state, onBack)
                else -> QuestionView(state, vm)
            }
        }
    }
}

@Composable
private fun QuestionView(state: ExamUiState, vm: ExamViewModel) {
    val q = state.current ?: return
    LinearProgressIndicator(
        progress = { state.index / state.questions.size.toFloat() },
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        "Question ${state.index + 1} / ${state.questions.size}" +
            if (q.isRetry) "  •  Try again • আবার চেষ্টা" else "",
        style = MaterialTheme.typography.labelLarge
    )

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (q.type) {
                QuestionType.JP_TO_MEANING -> {
                    Text("What does this mean? • এর মানে কী?", style = MaterialTheme.typography.titleSmall)
                    Text(q.word.japanese, fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (state.selected != null) Text("${q.word.reading} • ${q.word.romaji}")
                }
                QuestionType.MEANING_TO_JP -> {
                    Text("Which is the Japanese? • জাপানি কোনটি?", style = MaterialTheme.typography.titleSmall)
                    Text(q.word.meaningBoth, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                }
                QuestionType.LISTEN -> {
                    Text("Listen and choose the meaning • শুনে মানে বাছুন", style = MaterialTheme.typography.titleSmall)
                    FilledTonalButton(onClick = vm::replay) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, null)
                        Text("  Play again • আবার শুনুন")
                    }
                    if (state.selected != null) {
                        Text("${q.word.japanese}  ${q.word.reading}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    q.options.forEachIndexed { i, option ->
        val label = when (q.type) {
            QuestionType.MEANING_TO_JP -> "${option.japanese}   (${option.reading})".takeIf { state.selected != null }
                ?: option.japanese
            else -> option.meaningBoth
        }
        OptionButton(
            text = label,
            state = when {
                state.selected == null -> OptionState.NORMAL
                i == q.correctIndex -> OptionState.CORRECT
                i == state.selected -> OptionState.WRONG
                else -> OptionState.NORMAL
            },
            enabled = state.selected == null,
            onClick = { vm.select(i) },
            tag = "option$i"
        )
    }

    if (state.selected != null) {
        val correct = state.selected == q.correctIndex
        Text(
            if (correct) "✅ Correct! • সঠিক!"
            else "❌ Answer • সঠিক উত্তর: ${q.word.japanese} (${q.word.reading}) = ${q.word.meaningBoth}\n" +
                "You'll get this word again at the end. • এই শব্দটি শেষে আবার আসবে।",
            color = if (correct) CorrectGreen else ForgotRed,
            fontWeight = FontWeight.Bold
        )
        Button(onClick = vm::next, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Next • পরের প্রশ্ন")
        }
    }
}

private enum class OptionState { NORMAL, CORRECT, WRONG }

@Composable
private fun OptionButton(text: String, state: OptionState, enabled: Boolean, onClick: () -> Unit, tag: String) {
    val color = when (state) {
        OptionState.CORRECT -> CorrectGreen
        OptionState.WRONG -> ForgotRed
        OptionState.NORMAL -> Color.Unspecified
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled || state != OptionState.NORMAL,
        border = if (state == OptionState.NORMAL) null else BorderStroke(3.dp, color),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = if (state == OptionState.NORMAL) Color.Unspecified else color
        )
    }
}

@Composable
private fun ExamResult(state: ExamUiState, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("${state.scorePercent}%", fontSize = 64.sp, fontWeight = FontWeight.Bold,
            color = if (state.scorePercent >= 80) CorrectGreen else MaterialTheme.colorScheme.primary)
        Text("${state.firstTryCorrect} / ${state.totalOriginal} correct on the first try • প্রথমবারে সঠিক")
        Text(
            when {
                state.scorePercent >= 90 -> "素晴らしい! Excellent! • অসাধারণ!"
                state.scorePercent >= 70 -> "Good job! Keep going. • ভালো হয়েছে!"
                else -> "Keep practicing, you'll get there. • আরও অনুশীলন করুন।"
            },
            style = MaterialTheme.typography.titleMedium
        )
    }
    if (state.wrongWords.isNotEmpty()) {
        Text(
            "Words to practice (they'll come back in tomorrow's review) • যেগুলো ভুল হয়েছে (কাল আবার রিভিউতে আসবে):",
            style = MaterialTheme.typography.titleSmall
        )
        state.wrongWords.forEach { WrongWordRow(it) }
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Home • হোমে ফিরুন") }
}

@Composable
private fun WrongWordRow(v: VocabEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${v.japanese}  (${v.reading})", fontWeight = FontWeight.Bold)
            Text(v.meaningBoth)
        }
    }
}
