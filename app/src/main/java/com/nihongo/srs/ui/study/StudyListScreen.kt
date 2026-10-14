package com.nihongo.srs.ui.study

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabWithProgress
import com.nihongo.srs.data.exampleRomaji
import com.nihongo.srs.srs.SpacedRepetitionEngine
import com.nihongo.srs.ui.ExampleSentence
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.ChoiceChip
import com.nihongo.srs.ui.components.EmptyState
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SoftCard

/** My Words tab: learned words and their sentences, everything visible (no flipping). */
@Composable
fun StudyListScreen(
    onManage: () -> Unit,
    vm: StudyListViewModel = viewModel(factory = StudyListViewModel.Factory),
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val sentencesOnly by vm.sentencesOnly.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    AppScreen(
        title = "My words",
        subtitle = "আমার শেখা শব্দ ও বাক্য",
        actions = {
            FilledTonalIconButton(onClick = onManage, modifier = Modifier.testTag("manage")) {
                Icon(Icons.Rounded.Add, "Add / Manage words")
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                        }
                    },
                    placeholder = { Text("Search: বাংলা, English, 日本語, romaji") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("words_search"),
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChoiceChip("Today • আজ", filter == StudyFilter.TODAY, { vm.filter.value = StudyFilter.TODAY })
                    ChoiceChip("All • সব", filter == StudyFilter.ALL, { vm.filter.value = StudyFilter.ALL })
                    ChoiceChip("Sentences • বাক্য", sentencesOnly, { vm.sentencesOnly.value = !sentencesOnly })
                }
                Text(
                    if (sentencesOnly)
                        "Read each sentence out loud 3 times. Then cover the Japanese and say it from the Bangla.\n" +
                            "প্রতিটি বাক্য ৩ বার জোরে পড়ুন। তারপর বাংলা দেখে জাপানিতে বলুন।"
                    else if (query.isNotBlank()) "${items.size} found in all words • ${items.size}টি পাওয়া গেছে"
                    else "${items.size} words • ${items.size}টি শব্দ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (items.isEmpty()) {
                item {
                    if (query.isNotBlank()) {
                        EmptyState("🔍", "No word found", "কোনো শব্দ পাওয়া যায়নি। Ask in Banglish দিয়ে চেষ্টা করুন।")
                    } else if (filter == StudyFilter.TODAY) {
                        EmptyState("🌱", "Nothing learned today yet", "আজ এখনো কিছু শেখা হয়নি। হোম থেকে নতুন শব্দ শিখুন।")
                    } else {
                        EmptyState("📖", "No words learned yet", "এখনো কোনো শব্দ শেখা হয়নি।")
                    }
                }
            }
            items(items, key = { it.vocab.id }) { item ->
                if (sentencesOnly) {
                    SentenceRow(item.vocab, onSpeak = { slow -> vm.speakExample(item.vocab, slow) })
                } else {
                    WordRow(
                        item,
                        onSpeakWord = { vm.speakWord(item.vocab) },
                        onSpeakExample = { vm.speakExample(item.vocab, slow = true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WordRow(item: VocabWithProgress, onSpeakWord: () -> Unit, onSpeakExample: () -> Unit) {
    val v = item.vocab
    SoftCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(v.japanese, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(10.dp))
                    if (v.reading != v.japanese) {
                        Text(v.reading, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
                Text(v.romaji, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(v.meaning, style = MaterialTheme.typography.titleMedium)
                Text(v.meaningBn, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = onSpeakWord) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen") }
        }
        item.progress?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(v.level)
                Pill(SpacedRepetitionEngine.stageLabel(it.stage, it.isMastered),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ExampleSentence(v, onSpeak = onSpeakExample)
    }
}

@Composable
private fun SentenceRow(v: VocabEntity, onSpeak: (slow: Boolean) -> Unit) {
    if (v.example.isBlank()) return
    SoftCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(v.example, style = MaterialTheme.typography.titleLarge)
                Text(v.exampleRomaji, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(v.exampleEn, style = MaterialTheme.typography.bodyLarge)
                Text(v.exampleBn, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(onClick = { onSpeak(false) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen") }
                FilledTonalIconButton(onClick = { onSpeak(true) }) { Icon(Icons.Rounded.SlowMotionVideo, "Slow") }
            }
        }
        Pill("${v.japanese} = ${v.meaningBn}")
    }
}
