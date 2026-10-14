package com.nihongo.srs.ui.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabWithProgress
import com.nihongo.srs.srs.SpacedRepetitionEngine
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.BigButton
import com.nihongo.srs.ui.components.BigOutlinedButton
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SoftCard

/** Add a word manually, preload JLPT N5, search, delete or reset words. */
@Composable
fun ManageScreen(
    onBack: () -> Unit,
    vm: ManageViewModel = viewModel(factory = ManageViewModel.Factory),
) {
    val form by vm.form.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val words by vm.words.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var toDelete by remember { mutableStateOf<VocabEntity?>(null) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    AppScreen(title = "Add / Manage words", subtitle = "শব্দ যোগ ও পরিচালনা", onBack = onBack, snackbar = snackbar) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SoftCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Bilingual("Add a word", "নতুন শব্দ যোগ করুন", enStyle = MaterialTheme.typography.titleLarge)
                        Field("Japanese (漢字/かな) *", form.japanese) { v -> vm.updateForm { it.copy(japanese = v) } }
                        Field("Reading (かな) *", form.reading) { v -> vm.updateForm { it.copy(reading = v) } }
                        Field("Romaji (e.g. mizu)", form.romaji) { v -> vm.updateForm { it.copy(romaji = v) } }
                        Field("English meaning", form.meaning) { v -> vm.updateForm { it.copy(meaning = v) } }
                        Field("বাংলা অর্থ (Bengali meaning)", form.meaningBn) { v -> vm.updateForm { it.copy(meaningBn = v) } }
                        Field("Example sentence (optional) • উদাহরণ বাক্য", form.example) { v -> vm.updateForm { it.copy(example = v) } }
                        Field("Sentence in English (optional)", form.exampleEn) { v -> vm.updateForm { it.copy(exampleEn = v) } }
                        Field("বাক্যের বাংলা অর্থ (optional)", form.exampleBn) { v -> vm.updateForm { it.copy(exampleBn = v) } }
                        BigButton("Save", "সেভ করুন", onClick = vm::save, icon = Icons.Rounded.Check)
                    }
                }
            }
            item {
                BigOutlinedButton("Reload N5 + N4 words & phrases", "N5 + N4 আবার লোড করুন",
                    onClick = { vm.preloadN5() }, icon = Icons.Rounded.Download)
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    label = { Text("Search • খুঁজুন") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${words.size} words • ${words.size}টি শব্দ", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            items(words, key = { it.vocab.id }) { item ->
                WordRow(
                    item = item,
                    onSpeak = { vm.speak(item.vocab) },
                    onReset = { vm.resetProgress(item.vocab) },
                    onDelete = { toDelete = item.vocab }
                )
            }
        }
    }

    toDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete ${v.japanese}?") },
            text = { Text("This also deletes its progress. • এর অগ্রগতিও মুছে যাবে।") },
            confirmButton = {
                TextButton(onClick = { vm.delete(v); toDelete = null }) { Text("Delete • মুছুন") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel • বাতিল") } }
        )
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WordRow(
    item: VocabWithProgress,
    onSpeak: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val v = item.vocab
    val p = item.progress
    SoftCard(padding = 12.dp) {
        Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${v.japanese}  ${v.reading}", style = MaterialTheme.typography.titleMedium)
                Text("${v.meaning} • ${v.meaningBn}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    v.level + " · " + (p?.let { SpacedRepetitionEngine.stageLabel(it.stage, it.isMastered) } ?: "Not started • শুরু হয়নি"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen") }
            if (p != null) IconButton(onClick = onReset) { Icon(Icons.Rounded.Refresh, "Reset") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Delete") }
        }
    }
}
