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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
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
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.BigButton
import com.nihongo.srs.ui.components.BigOutlinedButton
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.EmptyState
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SessionProgress
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.theme.Extra

/**
 * Flashcard screen used for both reviewing due words and learning new ones.
 *
 * Front: the Japanese word (or, on reverse cards, the meaning). Tap to flip and
 * reveal the reading, romaji, English and Bengali meaning plus an example.
 */
@Composable
fun FlashcardScreen(
    onBack: () -> Unit,
    vm: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tts by vm.ttsStatus.collectAsStateWithLifecycle()

    val (title, subtitle) = when (state.mode) {
        SessionMode.REVIEW -> "Review" to "রিভিউ"
        SessionMode.LEARN -> "Learn new words" to "নতুন শব্দ শিখুন"
    }

    AppScreen(title = title, subtitle = subtitle, onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TtsBanner(tts)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.isFinished -> SessionSummary(state, onBack, onPrevious = vm::goBack)
                else -> {
                    val card = state.current!!
                    SessionProgress(state.index, state.total)
                    if (state.canGoBack) PreviousCardButton(state.mode, vm::goBack)

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
                            BigButton(
                                "Say it out loud, then show answer", "জোরে বলুন, তারপর উত্তর দেখুন",
                                onClick = vm::flip, icon = Icons.Rounded.Visibility,
                                modifier = Modifier.testTag("show_answer"),
                            )
                        }
                        SessionMode.LEARN -> LearnButtons(
                            enabled = !state.isBusy,
                            onSkip = vm::skip,
                            onLearned = vm::markLearned
                        )
                    }
                    Spacer(Modifier.height(4.dp))
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
    val shape = RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .shadow(10.dp, shape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            .clip(shape)
            .clickable(onClick = onFlip),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                // Un-mirror the back side's content.
                .graphicsLayer { rotationY = if (showBack) 180f else 0f }
                .padding(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Pill(stageText)
                Spacer(Modifier.weight(1f))
                // On a reverse card the sound would give the answer away, so it waits for the flip.
                if (showBack || !reverse) {
                    FilledTonalIconButton(onClick = onSpeakSlow) { Icon(Icons.Rounded.SlowMotionVideo, "Slow") }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalIconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen") }
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                when {
                    showBack -> {
                        WordAnswer(vocab, showJapanese = true)
                        ExampleSentence(vocab, onSpeak = onSpeakExample)
                    }
                    reverse -> {
                        Pill("How do you say this in Japanese? • জাপানিতে কী বলে?",
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(vocab.meaning, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Text(vocab.meaningBn, style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    else -> {
                        Text(
                            vocab.japanese,
                            fontSize = if (vocab.japanese.length > 6) 40.sp else 64.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text("What does it mean? • এর মানে কী?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!showBack) {
                    Text("Tap the card to flip • কার্ডে চাপ দিন", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun ReviewButtons(enabled: Boolean, onForgot: () -> Unit, onKnew: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigButton("Forgot", "ভুলে গেছি", onClick = onForgot, enabled = enabled, icon = Icons.Rounded.Close,
            container = Extra.wrongSoft, content = Extra.wrong, modifier = Modifier.weight(1f).testTag("forgot"))
        BigButton("I Knew It", "মনে আছে", onClick = onKnew, enabled = enabled, icon = Icons.Rounded.Check,
            container = Extra.correct, content = Color.White, modifier = Modifier.weight(1f).testTag("knew"))
    }
}

@Composable
private fun LearnButtons(enabled: Boolean, onSkip: () -> Unit, onLearned: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigOutlinedButton("Skip", "পরে", onClick = onSkip, enabled = enabled, modifier = Modifier.weight(1f))
        BigButton("Learned, review tomorrow", "শিখেছি, কাল রিভিউ", onClick = onLearned, enabled = enabled,
            icon = Icons.Rounded.Check, modifier = Modifier.weight(2f).testTag("learned"))
    }
}

/** Go back one card and undo the answer given there, for when a button was pressed by mistake. */
@Composable
private fun PreviousCardButton(mode: SessionMode, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.testTag("previous_card")) {
        Icon(Icons.AutoMirrored.Rounded.Undo, null)
        Spacer(Modifier.width(8.dp))
        Text(
            when (mode) {
                SessionMode.LEARN -> "আগের শব্দে ফিরুন · ভুল করে চাপলে আবার নতুন হবে"
                SessionMode.REVIEW -> "আগের শব্দে ফিরুন · আগের উত্তর বাতিল হবে"
            },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SessionSummary(state: ReviewUiState, onBack: () -> Unit, onPrevious: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        if (state.total == 0) {
            EmptyState("🎉", "Nothing here right now", "এখন কিছু নেই")
        } else {
            Text("🎉", fontSize = 64.sp)
            Bilingual("お疲れ様! Well done!", "সাবাশ! আজকের সেশন শেষ",
                enStyle = MaterialTheme.typography.headlineMedium, align = TextAlign.Center,
                horizontalAlignment = Alignment.CenterHorizontally)
            when (state.mode) {
                SessionMode.REVIEW -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResultTile("${state.correctCount}", "Knew", "মনে ছিল", Extra.correct, Extra.correctSoft, Modifier.weight(1f))
                    ResultTile("${state.forgotCount}", "Forgot", "কাল আবার আসবে", Extra.wrong, Extra.wrongSoft, Modifier.weight(1f))
                }
                SessionMode.LEARN ->
                    ResultTile("${state.correctCount}", "Words learned", "প্রথম রিভিউ কাল",
                        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, Modifier.fillMaxWidth())
            }
        }
        BigButton("Back to Home", "হোমে ফিরুন", onClick = onBack)
        if (state.canGoBack) {
            BigOutlinedButton("Previous word", "আগের শব্দে ফিরুন", onClick = onPrevious,
                icon = Icons.AutoMirrored.Rounded.Undo, modifier = Modifier.testTag("previous_card"))
        }
    }
}

@Composable
private fun ResultTile(value: String, en: String, bn: String, color: Color, soft: Color, modifier: Modifier) {
    SoftCard(modifier, color = soft, border = false) {
        Text(value, style = MaterialTheme.typography.displayMedium, color = color)
        Bilingual(en, bn, color = color)
    }
}
