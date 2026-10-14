package com.nihongo.srs.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartReview: () -> Unit,
    onLearnNew: () -> Unit,
    onManage: () -> Unit,
    onSpeak: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshDay() }

    Scaffold(topBar = { TopAppBar(title = { Text("日本語 SRS • জাপানি শিখি") }) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Big "due today" card
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Words to review today", style = MaterialTheme.typography.titleMedium)
                    Text("আজকের রিভিউ", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${state.dueToday}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = onStartReview,
                        enabled = state.dueToday > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Text("  Start Review • রিভিউ শুরু")
                    }
                    if (state.dueToday == 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Nothing due. Learn new words below! • আজ কিছু বাকি নেই",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Learning", "শিখছি", state.learning, Modifier.weight(1f))
                StatCard("Mastered", "আয়ত্ত", state.mastered, Modifier.weight(1f))
                StatCard("New", "নতুন", state.newWords, Modifier.weight(1f))
            }

            FilledTonalButton(
                onClick = onLearnNew,
                enabled = state.newWords > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null)
                Text("  Learn 10 new words • ১০টি নতুন শব্দ শিখুন")
            }
            FilledTonalButton(onClick = onSpeak, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Mic, null)
                Text("  Speaking practice • কথা বলার অনুশীলন")
            }
            OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.EditNote, null)
                Text("  Add / Manage words • শব্দ যোগ করুন")
            }

            DailyPlanCard()
        }
    }
}

@Composable
private fun StatCard(en: String, bn: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(en, style = MaterialTheme.typography.labelMedium)
            Text(bn, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** A short daily routine so the learner actually starts speaking. */
@Composable
private fun DailyPlanCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Daily 20-minute plan • প্রতিদিন ২০ মিনিট", style = MaterialTheme.typography.titleMedium)
            Text("1. Review all due cards first (5 min) • আগে রিভিউ শেষ করুন")
            Text("2. Learn 10 new words, say each one out loud (5 min) • নতুন শব্দ জোরে বলুন")
            Text("3. Speaking: listen, repeat 3 times, check with the mic (10 min) • শুনুন, ৩ বার বলুন")
            Text(
                "Tip: never skip a day. A late review still counts. • একদিনও বাদ দেবেন না।",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
