package com.nihongo.srs.ui.review

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.srs.SpacedRepetitionEngine
import com.nihongo.srs.ui.ExampleSentence
import com.nihongo.srs.ui.TtsBanner
import com.nihongo.srs.ui.WordAnswer
import com.nihongo.srs.ui.theme.CorrectGreen
import com.nihongo.srs.ui.theme.ForgotRed

/**
 * Flashcard screen used for both reviewing due words and learning new ones.
 *
 * Front: the Japanese word. Tap to flip (3D animation) and reveal the
 * reading, romaji, English and Bengali meaning. Pronunciation plays on flip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    onBack: () -> Unit,
    vm: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tts by vm.ttsStatus.collectAsStateWithLifecycle()

    val title = when (state.mode) {
        SessionMode.REVIEW -> "Review • রিভিউ"
        SessionMode.LEARN -> "Learn new • নতুন শিখুন"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TtsBanner(tts)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.isFinished -> SessionSummary(state, onBack)
                else -> {
                    val card = state.current!!
                    LinearProgressIndicator(
                        progress = { state.index / state.total.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${state.index + 1} / ${state.total}",
                        style = MaterialTheme.typography.labelLarge
                    )

                    FlipCard(
                        vocab = card.vocab,
                        reverse = card.reverse,
                        stageText = card.progress?.let {
                            SpacedRepetitionEngine.stageLabel(it.stage, it.isMastered)
                        } ?: "New • নতুন",
                        isFlipped = state.isFlipped,
                        onFlip = vm::flip,
                        onSpeak = { vm.speakCurrent(slow = false) },
                        onSpeakSlow = { vm.speakCurrent(slow = true) },
                        onSpeakExample = vm::speakExample,
                        modifier = Modifier.weight(1f)
                    )

                    when (state.mode) {
                        SessionMode.REVIEW -> if (state.isFlipped) {
                            ReviewButtons(
                                enabled = !state.isBusy,
                                onForgot = { vm.answer(false) },
                                onKnew = { vm.answer(true) }
                            )
                        } else {
                            Button(onClick = vm::flip, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                                Text("Say it out loud, then show answer\nজোরে বলুন, তারপর উত্তর দেখুন",
                                    textAlign = TextAlign.Center)
                            }
                        }
                        SessionMode.LEARN -> LearnButtons(
                            enabled = !state.isBusy,
                            onSkip = vm::skip,
                            onLearned = vm::markLearned
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(
    vocab: VocabEntity,
    reverse: Boolean,
    stageText: String,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSpeak: () -> Unit,
    onSpeakSlow: () -> Unit,
    onSpeakExample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )
    val showBack = rotation > 90f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(onClick = onFlip),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                // Un-mirror the back side's content.
                .graphicsLayer { rotationY = if (showBack) 180f else 0f }
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(stageText) })
                Spacer(Modifier.weight(1f))
                // On a reverse card the sound would give the answer away, so it waits for the flip.
                if (showBack || !reverse) {
                    IconButton(onClick = onSpeakSlow) { Icon(Icons.Default.SlowMotionVideo, "Slow") }
                    IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Listen") }
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
            ) {
                when {
                    showBack -> {
                        WordAnswer(vocab, showJapanese = true)
                        ExampleSentence(vocab, onSpeak = onSpeakExample)
                    }
                    reverse -> {
                        Text("How do you say this in Japanese?\nজাপানিতে কী বলে?",
                            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary)
                        Text("🇬🇧 ${vocab.meaning}", style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center)
                        Text("🇧🇩 ${vocab.meaningBn}", style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center)
                    }
                    else -> {
                        Text(
                            vocab.japanese,
                            fontSize = if (vocab.japanese.length > 6) 36.sp else 56.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text("What does it mean? • এর মানে কী?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewButtons(enabled: Boolean, onForgot: () -> Unit, onKnew: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onForgot,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = ForgotRed),
            modifier = Modifier.weight(1f).height(56.dp)
        ) {
            Icon(Icons.Default.Close, null)
            Text(" Forgot\nভুলে গেছি", textAlign = TextAlign.Center)
        }
        Button(
            onClick = onKnew,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = CorrectGreen),
            modifier = Modifier.weight(1f).height(56.dp)
        ) {
            Icon(Icons.Default.Check, null)
            Text(" I Knew It\nমনে আছে", textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LearnButtons(enabled: Boolean, onSkip: () -> Unit, onLearned: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onSkip, enabled = enabled, modifier = Modifier.weight(1f).height(56.dp)) {
            Text("Skip\nপরে", textAlign = TextAlign.Center)
        }
        Button(onClick = onLearned, enabled = enabled, modifier = Modifier.weight(2f).height(56.dp)) {
            Icon(Icons.Default.Check, null)
            Text(" Learned, review tomorrow\nশিখেছি, কাল রিভিউ", textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SessionSummary(state: ReviewUiState, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.total == 0) {
            Text("Nothing here right now 🎉", style = MaterialTheme.typography.headlineSmall)
            Text("এখন কিছু নেই", style = MaterialTheme.typography.titleMedium)
        } else {
            Text("お疲れ様! Well done!", style = MaterialTheme.typography.headlineSmall)
            Text("সাবাশ! আজকের সেশন শেষ", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            when (state.mode) {
                SessionMode.REVIEW -> {
                    Text("✅ Knew • মনে ছিল: ${state.correctCount}")
                    Text("❌ Forgot • ভুলেছি: ${state.forgotCount} (review tomorrow • কাল আবার)")
                }
                SessionMode.LEARN ->
                    Text("📚 Learned • শিখেছি: ${state.correctCount} (first review tomorrow • কাল প্রথম রিভিউ)")
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Back to Home • হোমে ফিরুন") }
    }
}
