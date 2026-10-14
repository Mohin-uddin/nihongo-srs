package com.nihongo.srs.ui.progress

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.LevelProgress
import com.nihongo.srs.data.StudyDay
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ProgressBar
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.theme.Extra
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/** Progress tab: completion per level, streak, SRS stages, history. */
@Composable
fun ProgressScreen(
    vm: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory),
) {
    val s by vm.state.collectAsStateWithLifecycle()

    AppScreen(title = "My progress", subtitle = "আমার অগ্রগতি") { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Headline numbers
            SoftCard(padding = 16.dp) {
                Row {
                    Stat("${s.streak}", "Day streak", "টানা দিন", Modifier.weight(1f), Extra.streak)
                    Stat("${s.wordsLearned}", "Learned", "শেখা শব্দ", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                    Stat("${s.wordsMastered}", "Mastered", "আয়ত্ত", Modifier.weight(1f), Extra.correct)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row {
                    Stat("${s.studyDays}", "Study days", "মোট দিন", Modifier.weight(1f))
                    Stat("${s.bestStreak}", "Best streak", "সেরা টানা", Modifier.weight(1f))
                    Stat("${s.accuracy}%", "Accuracy", "সঠিকতা", Modifier.weight(1f))
                }
            }

            // Completion per level
            SectionTitle("Course completion", "কোর্স কতটুকু শেষ")
            SoftCard {
                if (s.levels.isEmpty()) Text("Loading… • লোড হচ্ছে…")
                s.levels.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LevelRow(p)
                }
                Text(
                    "Learned = started with flashcards. Mastered = passed the last review.\n" +
                        "শেখা = শুরু করেছেন, আয়ত্ত = শেষ রিভিউ পাস করেছেন।",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // SRS stages
            SectionTitle("Where your words are", "শব্দগুলো কোন ধাপে")
            SoftCard {
                val labels = listOf(
                    "Stage 0 · tomorrow", "Stage 1 · day 4", "Stage 2 · day 7", "Stage 3 · day 14"
                )
                val max = (s.stages.maxOfOrNull { it.count } ?: 0).coerceAtLeast(s.wordsMastered).coerceAtLeast(1)
                labels.forEachIndexed { i, label ->
                    BarRow(label, s.stages.firstOrNull { it.stage == i }?.count ?: 0, max)
                }
                BarRow("Mastered · আয়ত্ত", s.wordsMastered, max, color = Extra.correct)
            }

            // Last 7 days
            SectionTitle("Last 7 days", "গত ৭ দিন")
            SoftCard {
                WeekChart(s.week)
                val week = s.week
                Text(
                    "This week: ${week.sumOf { it.learned }} new words, ${week.sumOf { it.reviewed }} reviews, " +
                        "${week.sumOf { it.exams }} exams, ${week.sumOf { it.spoken }} phrases spoken\n" +
                        "এই সপ্তাহে: ${week.sumOf { it.learned }} নতুন শব্দ, ${week.sumOf { it.reviewed }} রিভিউ",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Exam history
            SectionTitle("Recent exams", "সাম্প্রতিক পরীক্ষা")
            SoftCard {
                if (s.exams.isEmpty()) {
                    Text("No exams yet. Take one from Practice! • এখনো কোনো পরীক্ষা দেননি।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val fmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
                s.exams.forEachIndexed { i, e ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (e.scope) {
                                    "TODAY" -> "Today's exam"
                                    "ALL" -> "Full exam"
                                    else -> "Kana quiz"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(fmt.format(Date(e.timestamp)), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Pill(
                            "${e.percent}%",
                            color = if (e.percent >= 80) Extra.correctSoft else MaterialTheme.colorScheme.primaryContainer,
                            textColor = if (e.percent >= 80) Extra.correct else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Stat(value: String, en: String, bn: String, modifier: Modifier, color: Color = Color.Unspecified) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(en, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(bn, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$learnedPct%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        ProgressBar(learnedPct / 100f)
        Text("${p.learned} / ${p.total} learned • শেখা  ·  ${p.total - p.learned} left • বাকি",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProgressBar(masteredPct / 100f, color = Extra.correct, height = 6.dp)
        Text("${p.mastered} / ${p.total} mastered • আয়ত্ত ($masteredPct%)",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BarRow(label: String, value: Int, max: Int, color: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(130.dp))
        ProgressBar(value / max.toFloat(), Modifier.weight(1f), color = color, height = 12.dp)
        Text("$value", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End)
    }
}

/** Bar chart: words learned + reviews per day. */
@Composable
private fun WeekChart(week: List<StudyDay>) {
    val max = (week.maxOfOrNull { it.learned + it.reviewed } ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(130.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        week.forEach { d ->
            val total = d.learned + d.reviewed
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$total", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    Modifier.fillMaxWidth().height((84f * total / max).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (total > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest)
                )
                Text(
                    LocalDate.parse(d.date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
