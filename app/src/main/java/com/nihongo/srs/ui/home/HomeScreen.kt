package com.nihongo.srs.ui.home

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.ui.exam.ExamScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartReview: () -> Unit,
    onLearnNew: (level: String) -> Unit,
    onMyWords: () -> Unit,
    onExam: (ExamScope) -> Unit,
    onManage: () -> Unit,
    onSpeak: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshDay() }

    // Android 13+: ask once for permission to show the daily reminder.
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && vm.shouldAskNotificationPermission) {
            vm.onNotificationPermissionAsked()
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("日本語 SRS • জাপানি শিখি") }) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Due today
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Words to review today • আজকের রিভিউ", style = MaterialTheme.typography.titleMedium)
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
                            "Nothing due. Learn new words below! • আজ রিভিউ বাকি নেই",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Learning", "শিখছি", state.learning, Modifier.weight(1f))
                StatCard("Mastered", "আয়ত্ত", state.mastered, Modifier.weight(1f))
            }

            // 2. Learn new words
            Section("Learn new words • নতুন শব্দ শিখুন") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onLearnNew("N5") }, enabled = state.newN5 > 0,
                        modifier = Modifier.weight(1f)) {
                        Text("N5\n${state.newN5} new", textAlign = TextAlign.Center)
                    }
                    FilledTonalButton(onClick = { onLearnNew("N4") }, enabled = state.newN4 > 0,
                        modifier = Modifier.weight(1f)) {
                        Text("N4\n${state.newN4} new", textAlign = TextAlign.Center)
                    }
                    if (state.newMine > 0) {
                        FilledTonalButton(onClick = { onLearnNew(VocabEntity.LEVEL_MINE) },
                            modifier = Modifier.weight(1f)) {
                            Text("Mine\n${state.newMine} new", textAlign = TextAlign.Center)
                        }
                    }
                }
                Text("10 words per session, with example sentences • প্রতিবার ১০টি শব্দ, উদাহরণসহ",
                    style = MaterialTheme.typography.bodySmall)
            }

            // 3. Practice
            Section("Practice • অনুশীলন") {
                FilledTonalButton(onClick = onMyWords, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, null)
                    Text("  My words & sentences • আমার শব্দ ও বাক্য")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onExam(ExamScope.TODAY) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Quiz, null)
                        Text(" Today's exam\nআজকের পরীক্ষা", textAlign = TextAlign.Center)
                    }
                    FilledTonalButton(onClick = { onExam(ExamScope.ALL) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Quiz, null)
                        Text(" Full exam\nসব শব্দ", textAlign = TextAlign.Center)
                    }
                }
                FilledTonalButton(onClick = onSpeak, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Forum, null)
                    Text("  Daily conversation • প্রতিদিনের কথা")
                }
            }

            // 4. Reminder
            Section("Daily reminder • প্রতিদিনের রিমাইন্ডার") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null)
                    Text(
                        if (reminder.enabled) "  Every day at ${reminder.label}" else "  Off • বন্ধ",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = reminder.enabled, onCheckedChange = { vm.setReminder(it) })
                }
                if (reminder.enabled) {
                    TextButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m -> vm.setReminder(true, h, m) },
                            reminder.hour, reminder.minute, false
                        ).show()
                    }) { Text("Change time • সময় বদলান") }
                }
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
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
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
            Text("$en • $bn", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A daily routine built around active recall, so words stick and turn into speaking. */
@Composable
private fun DailyPlanCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Daily 25-minute plan • প্রতিদিন ২৫ মিনিট", style = MaterialTheme.typography.titleMedium)
            Text("1. Review due cards. Answer out loud BEFORE you flip. • উল্টানোর আগে জোরে উত্তর বলুন")
            Text("2. Learn 10 new words and read their sentences aloud. • নতুন ১০টি শব্দ ও বাক্য জোরে পড়ুন")
            Text("3. Take today's exam (20+ questions). • আজকের পরীক্ষা দিন")
            Text("4. Daily conversation: listen, repeat 3 times, check with the mic. • শুনুন, ৩ বার বলুন")
            Text(
                "Why do I forget? Seeing a word feels easy, but remembering it yourself is what makes it stick. " +
                    "That's why some cards show the Bangla and ask for the Japanese, and why the exam re-asks wrong answers.\n" +
                    "দেখলে মনে হয় পারি, কিন্তু নিজে মনে করে বলতে পারলেই শেখা পাকা হয়। তাই কিছু কার্ডে বাংলা দেখিয়ে জাপানি জিজ্ঞেস করা হয়।",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
