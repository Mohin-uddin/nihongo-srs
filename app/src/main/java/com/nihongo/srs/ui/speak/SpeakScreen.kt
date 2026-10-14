package com.nihongo.srs.ui.speak

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.nihongo.srs.ui.theme.CorrectGreen

/**
 * Speaking practice (shadowing):
 *  1. Listen (normal or slow)  2. Repeat out loud 3 times  3. Tap the mic to check.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily conversation • প্রতিদিনের কথা") },
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

            // Topic chips: Greetings, Shopping, Work, ...
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = category == null, onClick = { vm.selectCategory(null) },
                    label = { Text("All • সব") })
                categories.forEach { c ->
                    FilterChip(selected = category == c, onClick = { vm.selectCategory(c) },
                        label = { Text(c) })
                }
            }

            val phrase = phrases.getOrNull(state.index)
            if (phrase == null) {
                Text("Loading phrases… • লোড হচ্ছে…")
            } else {
                Text("${state.index + 1} / ${phrases.size}", style = MaterialTheme.typography.labelLarge)

                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
                    Column(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(phrase.japanese, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        if (phrase.reading != phrase.japanese) {
                            Text(phrase.reading, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        }
                        Text(phrase.romaji, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        if (state.showMeaning) {
                            Text("🇬🇧 ${phrase.meaning}", textAlign = TextAlign.Center)
                            Text("🇧🇩 ${phrase.meaningBn}", textAlign = TextAlign.Center)
                        }
                        TextButton(onClick = vm::toggleMeaning) {
                            Text(if (state.showMeaning) "Hide meaning • অর্থ লুকান" else "Show meaning • অর্থ দেখুন")
                        }
                    }
                }

                // Step 1: listen
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { vm.listen(slow = false) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, null); Text(" Listen • শুনুন")
                    }
                    FilledTonalButton(onClick = { vm.listen(slow = true) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.SlowMotionVideo, null); Text(" Slow • ধীরে")
                    }
                }

                // Step 2 + 3: speak and check
                Button(
                    onClick = { onMicClick() },
                    enabled = vm.micAvailable && state.mic !is MicState.Listening,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Mic, null)
                    Text(
                        if (state.mic is MicState.Listening) "  Listening… speak now • এখন বলুন"
                        else "  Speak & check • বলুন ও যাচাই করুন"
                    )
                }
                if (!vm.micAvailable) {
                    Text(
                        "Mic check isn't available on this phone. You can still listen and repeat. " +
                            "• এই ফোনে মাইক যাচাই নেই, তবে শুনে শুনে বলতে পারবেন।",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                MicResult(state.mic)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = vm::previous) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, null); Text("Previous • আগে")
                    }
                    TextButton(onClick = vm::next) {
                        Text("Next • পরে"); Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
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
        is MicState.Result -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Phone heard • ফোন শুনেছে: ${mic.heard.ifBlank { "—" }}")
                LinearProgressIndicator(
                    progress = { mic.score / 100f },
                    color = if (mic.score >= 80) CorrectGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
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
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("How to start speaking • কিভাবে কথা বলা শিখবেন", style = MaterialTheme.typography.titleMedium)
            Text("• Shadowing: play a phrase and say it at the same time, copying the rhythm. • শুনতে শুনতে একসাথে বলুন, সুর নকল করুন।")
            Text("• Say every word out loud, never only in your head. • মনে মনে নয়, জোরে বলুন।")
            Text("• Japanese sounds are flat and even. Give each kana the same length. • প্রতিটি অক্ষর সমান লম্বা করে বলুন।")
            Text("• Long vowels (おう, ええ) and small っ change meaning. Listen slowly for them. • লম্বা স্বর আর ছোট っ খেয়াল করুন।")
            Text("• Talk to yourself: describe your day with words you know. • নিজের সাথে জাপানিতে কথা বলুন।")
            Text("• Learn kana (ひらがな, カタカナ) early so you don't depend on romaji. • তাড়াতাড়ি কানা শিখে নিন।")
        }
    }
}
