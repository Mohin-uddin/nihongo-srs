package com.nihongo.srs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.exampleRomaji

/** Reading, romaji and both meanings of a word, centred. */
@Composable
fun WordAnswer(vocab: VocabEntity, showJapanese: Boolean) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showJapanese) {
            Text(
                vocab.japanese,
                fontSize = if (vocab.japanese.length > 6) 34.sp else 52.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Text(vocab.reading, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (vocab.romaji.isNotBlank()) {
            Text(
                vocab.romaji, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
        }
        if (vocab.meaning.isNotBlank()) {
            Text("🇬🇧 ${vocab.meaning}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
        if (vocab.meaningBn.isNotBlank()) {
            Text("🇧🇩 ${vocab.meaningBn}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}

/** The example sentence for a word, with a play button. Nothing if the word has none. */
@Composable
fun ExampleSentence(vocab: VocabEntity, onSpeak: () -> Unit, modifier: Modifier = Modifier) {
    if (vocab.example.isBlank()) return
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Example • উদাহরণ", style = MaterialTheme.typography.labelMedium)
                Text(vocab.example, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val romaji = vocab.exampleRomaji
                if (romaji.isNotBlank()) {
                    Text(romaji, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (vocab.exampleEn.isNotBlank()) Text("🇬🇧 ${vocab.exampleEn}", style = MaterialTheme.typography.bodyMedium)
                if (vocab.exampleBn.isNotBlank()) Text("🇧🇩 ${vocab.exampleBn}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Listen to sentence") }
        }
    }
}
