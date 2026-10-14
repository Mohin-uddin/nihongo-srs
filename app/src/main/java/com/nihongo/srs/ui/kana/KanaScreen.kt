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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.nihongo.srs.data.KanaData
import com.nihongo.srs.ui.TtsBanner
import com.nihongo.srs.ui.theme.CorrectGreen
import com.nihongo.srs.ui.theme.ForgotRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaScreen(
    onBack: () -> Unit,
    vm: KanaViewModel = viewModel(factory = KanaViewModel.Factory),
) {
    val katakana by vm.katakana.collectAsStateWithLifecycle()
    val quiz by vm.quiz.collectAsStateWithLifecycle()
    val tts by vm.ttsStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (quiz == null) "Kana chart • কানা" else "Kana quiz • কানা পরীক্ষা") },
                navigationIcon = {
                    IconButton(onClick = { if (quiz != null) vm.closeQuiz() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TtsBanner(tts)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !katakana, enabled = quiz == null,
                    onClick = { vm.katakana.value = false }, label = { Text("ひらがな Hiragana") })
                FilterChip(selected = katakana, enabled = quiz == null,
                    onClick = { vm.katakana.value = true }, label = { Text("カタカナ Katakana") })
            }
            val q = quiz
            if (q == null) Chart(katakana, vm) else Quiz(q, vm)
        }
    }
}

@Composable
private fun Chart(katakana: Boolean, vm: KanaViewModel) {
    Text(
        "Tap a letter to hear it. Learn hiragana first, then katakana (used for foreign words). " +
            "• অক্ষরে চাপ দিলে শুনবেন। আগে হিরাগানা, পরে কাতাকানা শিখুন।",
        style = MaterialTheme.typography.bodySmall
    )
    Button(onClick = vm::startQuiz, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Quiz, null)
        Text("  Start kana quiz (20) • কানা পরীক্ষা")
    }
    val rows = if (katakana) KanaData.katakanaRows else KanaData.hiraganaRows
    rows.forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { k ->
                Box(Modifier.weight(1f).aspectRatio(1f)) {
                    if (k != null) {
                        Card(Modifier.fillMaxSize().clickable { vm.speak(k) }) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(k.char, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Text(k.romaji, style = MaterialTheme.typography.labelSmall)
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
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${q.percent}%", fontSize = 64.sp, fontWeight = FontWeight.Bold,
                color = if (q.percent >= 80) CorrectGreen else MaterialTheme.colorScheme.primary)
            Text("${q.firstTryCorrect} / ${q.total} correct on the first try • প্রথমবারে সঠিক")
        }
        Button(onClick = vm::startQuiz, modifier = Modifier.fillMaxWidth()) { Text("Try again • আবার") }
        OutlinedButton(onClick = vm::closeQuiz, modifier = Modifier.fillMaxWidth()) { Text("Back to chart • চার্টে ফিরুন") }
        return
    }
    val cur = q.current ?: return
    LinearProgressIndicator(progress = { q.index / q.questions.size.toFloat() }, modifier = Modifier.fillMaxWidth())
    Text("Question ${q.index + 1} / ${q.questions.size}" + if (cur.isRetry) "  •  Try again • আবার" else "")
    Card(Modifier.fillMaxWidth()) {
        Text(cur.kana.char, fontSize = 80.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
    Text("How do you read it? • এটা কীভাবে পড়ে?", style = MaterialTheme.typography.titleSmall)
    cur.options.forEachIndexed { i, opt ->
        val color = when {
            q.selected == null -> Color.Unspecified
            opt == cur.kana.romaji -> CorrectGreen
            opt == q.selected -> ForgotRed
            else -> Color.Unspecified
        }
        OutlinedButton(
            onClick = { vm.select(opt) },
            enabled = q.selected == null || color != Color.Unspecified,
            border = if (color == Color.Unspecified) null else BorderStroke(3.dp, color),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("kana$i")
        ) { Text(opt, style = MaterialTheme.typography.titleLarge, color = color) }
    }
    if (q.selected != null) {
        Button(onClick = vm::next, modifier = Modifier.fillMaxWidth()) { Text("Next • পরের প্রশ্ন") }
    }
}
