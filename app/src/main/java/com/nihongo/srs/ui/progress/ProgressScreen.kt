package com.nihongo.srs.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.LevelProgress
import com.nihongo.srs.data.StudyDay
import com.nihongo.srs.ui.theme.CorrectGreen
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/** "How far have I got?" — completion per level, streak, SRS stages, history. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    vm: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory),
) {
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My progress • আমার অগ্রগতি") },
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
            // Headline numbers
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("🔥 ${s.streak}", "Day streak\nটানা দিন", Modifier.weight(1f))
                Stat("${s.wordsLearned}", "Words learned\nশেখা শব্দ", Modifier.weight(1f))
                Stat("${s.wordsMastered}", "Mastered\nআয়ত্ত", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("${s.studyDays}", "Study days\nমোট দিন", Modifier.weight(1f))
                Stat("${s.bestStreak}", "Best streak\nসেরা টানা", Modifier.weight(1f))
                Stat("${s.accuracy}%", "Review accuracy\nসঠিকতা", Modifier.weight(1f))
            }

            // Completion per level
            SectionCard("Course completion • কোর্স কতটুকু শেষ") {
                if (s.levels.isEmpty()) Text("Loading… • লোড হচ্ছে…")
                s.levels.forEach { LevelRow(it) }
                Text(
                    "Learned = started with flashcards. Mastered = passed the Day 14 review. " +
                        "• শেখা = শুরু করেছেন, আয়ত্ত = ১৪তম দিনের রিভিউ পাস করেছেন।",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // SRS stages
            SectionCard("Where your words are • শব্দগুলো কোন ধাপে") {
                val labels = listOf(
                    "Stage 0 • review tomorrow", "Stage 1 • Day 4", "Stage 2 • Day 7", "Stage 3 • Day 14"
                )
                val max = (s.stages.maxOfOrNull { it.count } ?: 0).coerceAtLeast(s.wordsMastered).coerceAtLeast(1)
                labels.forEachIndexed { i, label ->
                    BarRow(label, s.stages.firstOrNull { it.stage == i }?.count ?: 0, max)
                }
                BarRow("Mastered • আয়ত্ত", s.wordsMastered, max, color = CorrectGreen)
            }

            // Last 7 days
            SectionCard("Last 7 days • গত ৭ দিন") {
                WeekChart(s.week)
                val week = s.week
                Text(
                    "This week: ${week.sumOf { it.learned }} new words, ${week.sumOf { it.reviewed }} reviews, " +
                        "${week.sumOf { it.exams }} exams, ${week.sumOf { it.spoken }} phrases spoken\n" +
                        "এই সপ্তাহে: ${week.sumOf { it.learned }} নতুন শব্দ, ${week.sumOf { it.reviewed }} রিভিউ",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Exam history
            SectionCard("Recent exams • সাম্প্রতিক পরীক্ষা") {
                if (s.exams.isEmpty()) Text("No exams yet. Take one from Home! • এখনো কোনো পরীক্ষা দেননি।")
                val fmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
                s.exams.forEach { e ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (e.scope) {
                                "TODAY" -> "Today's exam"
                                "ALL" -> "Full exam"
                                else -> "Kana quiz"
                            } + " · " + fmt.format(Date(e.timestamp)),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("${e.percent}%", fontWeight = FontWeight.Bold,
                            color = if (e.percent >= 80) CorrectGreen else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun LevelRow(p: LevelProgress) {
    val name = when (p.level) {
        "Mine" -> "My words • নিজের শব্দ"
        else -> "JLPT ${p.level}"
    }
    val learnedPct = if (p.total == 0) 0 else p.learned * 100 / p.total
    val masteredPct = if (p.total == 0) 0 else p.mastered * 100 / p.total
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$learnedPct% learned", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(progress = { learnedPct / 100f }, modifier = Modifier.fillMaxWidth().height(10.dp))
        Text("${p.learned} / ${p.total} learned • শেখা  ·  ${p.total - p.learned} left • বাকি",
            style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(
            progress = { masteredPct / 100f }, color = CorrectGreen,
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
        Text("${p.mastered} / ${p.total} mastered • আয়ত্ত ($masteredPct%)", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BarRow(label: String, value: Int, max: Int, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(150.dp))
        Box(Modifier.weight(1f).height(14.dp)) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(value / max.toFloat())
                    .clip(RoundedCornerShape(4.dp)).background(color)
            )
        }
        Text("  $value", style = MaterialTheme.typography.labelMedium)
    }
}

/** Simple bar chart: words learned + reviews per day. */
@Composable
private fun WeekChart(week: List<StudyDay>) {
    val max = (week.maxOfOrNull { it.learned + it.reviewed } ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        week.forEach { d ->
            val total = d.learned + d.reviewed
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$total", style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier.fillMaxWidth().height((80f * total / max).dp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (total > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
                Text(
                    LocalDate.parse(d.date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
