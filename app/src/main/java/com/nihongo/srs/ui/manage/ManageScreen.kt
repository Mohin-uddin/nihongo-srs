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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabWithProgress
import com.nihongo.srs.srs.SpacedRepetitionEngine

/** Add a word manually, preload JLPT N5, search, delete or reset words. */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage words • শব্দ") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Add a word • নতুন শব্দ যোগ করুন", style = MaterialTheme.typography.titleMedium)
                        Field("Japanese (漢字/かな) *", form.japanese) { v -> vm.updateForm { it.copy(japanese = v) } }
                        Field("Reading (かな) *", form.reading) { v -> vm.updateForm { it.copy(reading = v) } }
                        Field("Romaji (e.g. mizu)", form.romaji) { v -> vm.updateForm { it.copy(romaji = v) } }
                        Field("English meaning", form.meaning) { v -> vm.updateForm { it.copy(meaning = v) } }
                        Field("বাংলা অর্থ (Bengali meaning)", form.meaningBn) { v -> vm.updateForm { it.copy(meaningBn = v) } }
                        Button(onClick = vm::save, modifier = Modifier.fillMaxWidth()) {
                            Text("Save • সেভ করুন")
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { vm.preloadN5() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, null)
                    Text("  Preload JLPT N5 words • N5 শব্দ লোড করুন")
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Search • খুঁজুন") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${words.size} words", style = MaterialTheme.typography.labelMedium)
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
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text("${v.japanese}  (${v.reading})", fontWeight = FontWeight.Bold)
                Text("${v.meaning} • ${v.meaningBn}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    p?.let { SpacedRepetitionEngine.stageLabel(it.stage, it.isMastered) } ?: "Not started • শুরু হয়নি",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Listen") }
            if (p != null) IconButton(onClick = onReset) { Icon(Icons.Default.Refresh, "Reset") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
}
