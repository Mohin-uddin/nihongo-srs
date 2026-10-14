package com.nihongo.srs.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabWithProgress
import com.nihongo.srs.data.exampleRomaji
import com.nihongo.srs.srs.SpacedRepetitionEngine
import com.nihongo.srs.ui.ExampleSentence

/** My Words: learned words and their sentences, everything visible (no flipping). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyListScreen(
    onBack: () -> Unit,
    vm: StudyListViewModel = viewModel(factory = StudyListViewModel.Factory),
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val sentencesOnly by vm.sentencesOnly.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Words • আমার শব্দ") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter == StudyFilter.TODAY,
                        onClick = { vm.filter.value = StudyFilter.TODAY },
                        label = { Text("Today • আজ") }
                    )
                    FilterChip(
                        selected = filter == StudyFilter.ALL,
                        onClick = { vm.filter.value = StudyFilter.ALL },
                        label = { Text("All • সব") }
                    )
                    FilterChip(
                        selected = sentencesOnly,
                        onClick = { vm.sentencesOnly.value = !sentencesOnly },
                        label = { Text("Sentences • বাক্য") }
                    )
                }
                Text(
                    if (sentencesOnly)
                        "Read each sentence out loud 3 times. Then cover the Japanese and say it from the Bangla. " +
                            "• প্রতিটি বাক্য ৩ বার জোরে পড়ুন। তারপর বাংলা দেখে জাপানিতে বলুন।"
                    else "${items.size} words • ${items.size} টি শব্দ",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        if (filter == StudyFilter.TODAY)
                            "Nothing learned today yet. Tap \"Learn new words\" on Home. • আজ এখনো কিছু শেখা হয়নি।"
                        else "No words learned yet. • এখনো কোনো শব্দ শেখা হয়নি।",
                        modifier = Modifier.padding(top = 24.dp)
                    )
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
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${v.japanese}  ${v.reading}", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text(v.romaji, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("🇬🇧 ${v.meaning}   🇧🇩 ${v.meaningBn}")
                    item.progress?.let {
                        Text(
                            "${v.level} • " + SpacedRepetitionEngine.stageLabel(it.stage, it.isMastered),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onSpeakWord) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Listen") }
            }
            ExampleSentence(v, onSpeak = onSpeakExample)
        }
    }
}

@Composable
private fun SentenceRow(v: VocabEntity, onSpeak: (slow: Boolean) -> Unit) {
    if (v.example.isBlank()) return
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(v.example, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(v.exampleRomaji, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("🇬🇧 ${v.exampleEn}")
                Text("🇧🇩 ${v.exampleBn}")
                Text("Word • শব্দ: ${v.japanese} = ${v.meaningBn}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Column {
                IconButton(onClick = { onSpeak(false) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Listen") }
                IconButton(onClick = { onSpeak(true) }) { Icon(Icons.Default.SlowMotionVideo, "Slow") }
            }
        }
    }
}
