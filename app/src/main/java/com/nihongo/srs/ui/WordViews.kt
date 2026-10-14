package com.nihongo.srs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.exampleRomaji

/** Word, reading, romaji and both meanings, centred. */
@Composable
fun WordAnswer(vocab: VocabEntity, showJapanese: Boolean) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (showJapanese) {
            Text(
                vocab.japanese,
                fontSize = if (vocab.japanese.length > 6) 36.sp else 54.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        if (vocab.reading != vocab.japanese) {
            Text(vocab.reading, style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
        if (vocab.romaji.isNotBlank()) {
            Text(vocab.romaji, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        MeaningRow("EN", vocab.meaning)
        MeaningRow("বাং", vocab.meaningBn)
    }
}

@Composable
private fun MeaningRow(tag: String, text: String) {
    if (text.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}

/** The example sentence for a word, with a play button. Nothing if the word has none. */
@Composable
fun ExampleSentence(vocab: VocabEntity, onSpeak: () -> Unit, modifier: Modifier = Modifier) {
    if (vocab.example.isBlank()) return
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("EXAMPLE • উদাহরণ", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            FilledTonalIconButton(onClick = onSpeak) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen to sentence")
            }
        }
        Text(vocab.example, style = MaterialTheme.typography.titleLarge)
        val romaji = vocab.exampleRomaji
        if (romaji.isNotBlank()) {
            Text(romaji, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
        if (vocab.exampleEn.isNotBlank()) Text(vocab.exampleEn, style = MaterialTheme.typography.bodyLarge)
        if (vocab.exampleBn.isNotBlank()) {
            Text(vocab.exampleBn, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
