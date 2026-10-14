package com.nihongo.srs.ui.ask

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.ai.AskResult
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.BigButton
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.ChoiceChip
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SoftCard

private val EXAMPLES = listOf("ami kal office e jabo", "pani khabo", "apnar nam ki?", "eta koto taka?", "ধন্যবাদ")

@Composable
fun AskScreen(
    onBack: () -> Unit,
    vm: AskViewModel = viewModel(factory = AskViewModel.Factory),
) {
    val input by vm.input.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val hasKey by vm.hasKey.collectAsStateWithLifecycle()
    val matches by vm.localMatches.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editingKey by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    AppScreen(
        title = "Ask in Banglish",
        subtitle = "বাংলিশ বা বাংলায় লিখুন, জাপানি দেখুন",
        onBack = onBack,
        snackbar = snackbar,
        actions = {
            IconButton(onClick = { editingKey = !editingKey }) { Icon(Icons.Rounded.Key, "Gemini API key") }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!hasKey || editingKey) {
                KeyCard(hasKey) { key -> vm.saveKey(key); editingKey = false }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { vm.input.value = it },
                placeholder = { Text("e.g. ami kal office e jabo") },
                label = { Text("Banglish / বাংলা / English") },
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { vm.ask() }),
                trailingIcon = {
                    FilledIconButton(onClick = vm::ask, enabled = input.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Rounded.Send, "Ask")
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("ask_input"),
            )
            if (state is AskState.Idle && matches.isEmpty()) {
                Text("Try one • চেষ্টা করুন", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EXAMPLES.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { ex ->
                                ChoiceChip(ex, selected = false, onClick = { vm.input.value = ex; vm.ask() })
                            }
                        }
                    }
                }
            }

            when (val s = state) {
                AskState.Idle -> Unit
                AskState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                is AskState.Error -> SoftCard(color = MaterialTheme.colorScheme.secondaryContainer, border = false) {
                    Text(s.message, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                is AskState.Answer -> AnswerView(s.result, vm)
            }

            if (matches.isNotEmpty()) {
                SectionTitle("Already in your app", "অ্যাপে আগে থেকেই আছে")
                matches.forEach { LocalMatch(it, onSpeak = { vm.speak(it.reading) }) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun KeyCard(hasKey: Boolean, onSave: (String) -> Unit) {
    val context = LocalContext.current
    var key by rememberSaveable { mutableStateOf("") }
    SoftCard(color = MaterialTheme.colorScheme.primaryContainer, border = false) {
        Bilingual(
            if (hasKey) "Change your Gemini API key" else "Add your free Gemini API key",
            "Banglish থেকে জাপানি বানাতে Gemini লাগে। Key শুধু আপনার ফোনেই থাকবে।",
            enStyle = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton("Save key", "সেভ করুন", onClick = { onSave(key); key = "" }, enabled = key.isNotBlank())
        TextButton(onClick = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
            }
        }) { Text("Get a free key at aistudio.google.com • ফ্রি key নিন") }
    }
}

@Composable
private fun AnswerView(r: AskResult, vm: AskViewModel) {
    SoftCard(padding = 22.dp) {
        if (r.understood.isNotBlank()) {
            Pill("You meant • আপনি বলেছেন: ${r.understood}",
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(r.japanese, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
        Text(r.kana, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(r.romaji, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Bilingual(r.english, r.bangla, enStyle = MaterialTheme.typography.titleMedium,
            bnStyle = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { vm.speak(r.kana) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen") }
            FilledTonalIconButton(onClick = { vm.speak(r.kana, slow = true) }) { Icon(Icons.Rounded.SlowMotionVideo, "Slow") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.saveSentence(r) }) {
                Icon(Icons.Rounded.BookmarkAdd, null)
                Spacer(Modifier.width(6.dp))
                Text("Save sentence")
            }
        }
    }

    if (r.words.isNotEmpty()) {
        SectionTitle("Words in this sentence", "এই বাক্যের শব্দগুলো")
        SoftCard(padding = 8.dp) {
            r.words.forEachIndexed { i, w ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(w.japanese, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(8.dp))
                            Text(w.kana, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${w.bangla} · ${w.english}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.speak(w.kana) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen") }
                    FilledTonalIconButton(onClick = { vm.saveWord(w, r) }) { Icon(Icons.Rounded.Add, "Save word") }
                }
            }
        }
        Text("Tap + to add a word to \"My own words\" and learn it with flashcards. • + চাপলে শব্দটি শেখার তালিকায় যোগ হবে।",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (r.tip.isNotBlank()) {
        SoftCard(color = MaterialTheme.colorScheme.primaryContainer, border = false) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(r.tip, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun LocalMatch(v: VocabEntity, onSpeak: () -> Unit) {
    SoftCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(v.japanese, style = MaterialTheme.typography.titleLarge)
                    if (v.reading != v.japanese) {
                        Spacer(Modifier.width(8.dp))
                        Text(v.reading, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("${v.meaningBn} · ${v.meaning}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
            }
            FilledTonalIconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen") }
        }
    }
}
