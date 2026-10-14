package com.nihongo.srs.ui.speak

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.ui.TtsBanner
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.BigButton
import com.nihongo.srs.ui.components.BigOutlinedButton
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.ChoiceChip
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ProgressBar
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SessionProgress
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.theme.Extra

/**
 * Speaking practice (shadowing):
 *  1. Listen (normal or slow)  2. Repeat out loud 3 times  3. Tap the mic to check.
 */
@Composable
fun SpeakScreen(
    onBack: () -> Unit,
    vm: SpeakViewModel = viewModel(factory = SpeakViewModel.Factory),
) {
    val phrases by vm.phrases.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val tts by vm.ttsStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.startSpeaking() else vm.onMicPermissionDenied() }

    fun onMicClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.startSpeaking() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    AppScreen(title = "Daily conversation", subtitle = "প্রতিদিনের কথা", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TtsBanner(tts)

            // Topic chips: Greetings, Shopping, Work, ...
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChoiceChip("All • সব", selected = category == null, onClick = { vm.selectCategory(null) })
                categories.forEach { c ->
                    ChoiceChip(c, selected = category == c, onClick = { vm.selectCategory(c) })
                }
            }

            val phrase = phrases.getOrNull(state.index)
            if (phrase == null) {
                Text("Loading phrases… • লোড হচ্ছে…")
            } else {
                SessionProgress(state.index, phrases.size)

                SoftCard(padding = 24.dp) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (phrase.category.isNotBlank()) Pill(phrase.category)
                        Text(phrase.japanese, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, lineHeight = 40.sp)
                        if (phrase.reading != phrase.japanese) {
                            Text(phrase.reading, style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                        }
                        Text(phrase.romaji, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        if (state.showMeaning) {
                            Bilingual(phrase.meaning, phrase.meaningBn, align = TextAlign.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                enStyle = MaterialTheme.typography.titleLarge,
                                bnStyle = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = vm::toggleMeaning) {
                            Text(if (state.showMeaning) "Hide meaning • অর্থ লুকান" else "Show meaning • অর্থ দেখুন")
                        }
                    }
                }

                // Step 1: listen
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigOutlinedButton("Listen", "শুনুন", onClick = { vm.listen(slow = false) },
                        icon = Icons.AutoMirrored.Rounded.VolumeUp, modifier = Modifier.weight(1f))
                    BigOutlinedButton("Slow", "ধীরে", onClick = { vm.listen(slow = true) },
                        icon = Icons.Rounded.SlowMotionVideo, modifier = Modifier.weight(1f))
                }

                // Step 2 + 3: speak and check
                val listening = state.mic is MicState.Listening
                BigButton(
                    if (listening) "Listening… speak now" else "Speak & check",
                    if (listening) "এখন বলুন" else "বলুন ও যাচাই করুন",
                    onClick = { onMicClick() },
                    enabled = vm.micAvailable && !listening,
                    icon = Icons.Rounded.Mic,
                    container = MaterialTheme.colorScheme.secondary,
                    content = MaterialTheme.colorScheme.onSecondary,
                )
                if (!vm.micAvailable) {
                    Text(
                        "Mic check isn't available on this phone. You can still listen and repeat. " +
                            "• এই ফোনে মাইক যাচাই নেই, তবে শুনে শুনে বলতে পারবেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MicResult(state.mic)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = vm::previous) {
                        Icon(Icons.AutoMirrored.Rounded.NavigateBefore, null); Text("Previous • আগে")
                    }
                    TextButton(onClick = vm::next) {
                        Text("Next • পরে"); Icon(Icons.AutoMirrored.Rounded.NavigateNext, null)
                    }
                }
            }

            SpeakingTips()
        }
    }
}

@Composable
private fun MicResult(mic: MicState) {
    when (mic) {
        MicState.Idle, MicState.Listening -> Unit
        is MicState.Error -> Text(mic.message, color = MaterialTheme.colorScheme.error)
        is MicState.Result -> SoftCard(padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Phone heard • ফোন শুনেছে: ${mic.heard.ifBlank { "—" }}")
                ProgressBar(
                    mic.score / 100f,
                    color = if (mic.score >= 80) Extra.correct else MaterialTheme.colorScheme.primary,
                )
                Text(
                    when {
                        mic.score >= 80 -> "${mic.score}% 素晴らしい! Excellent! • দারুণ!"
                        mic.score >= 50 -> "${mic.score}% Close! Listen slowly and try again • প্রায় হয়েছে, আবার চেষ্টা করুন"
                        else -> "${mic.score}% Keep practicing • আরও অনুশীলন করুন"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** How to actually start speaking Japanese, in English + Bengali. */
@Composable
private fun SpeakingTips() {
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Bilingual("How to start speaking", "কিভাবে কথা বলা শিখবেন", enStyle = MaterialTheme.typography.titleLarge)
            Text("• Shadowing: play a phrase and say it at the same time, copying the rhythm. • শুনতে শুনতে একসাথে বলুন, সুর নকল করুন।")
            Text("• Say every word out loud, never only in your head. • মনে মনে নয়, জোরে বলুন।")
            Text("• Japanese sounds are flat and even. Give each kana the same length. • প্রতিটি অক্ষর সমান লম্বা করে বলুন।")
            Text("• Long vowels (おう, ええ) and small っ change meaning. Listen slowly for them. • লম্বা স্বর আর ছোট っ খেয়াল করুন।")
            Text("• Talk to yourself: describe your day with words you know. • নিজের সাথে জাপানিতে কথা বলুন।")
            Text("• Learn kana (ひらがな, カタカナ) early so you don't depend on romaji. • তাড়াতাড়ি কানা শিখে নিন।")
        }
    }
}
