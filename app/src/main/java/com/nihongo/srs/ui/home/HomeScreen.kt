package com.nihongo.srs.ui.home

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.LevelProgress
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.GridTile
import com.nihongo.srs.ui.components.IconBadge
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ProgressBar
import com.nihongo.srs.ui.components.ProgressRing
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.exam.ExamScope
import com.nihongo.srs.ui.theme.Extra

@Composable
fun HomeScreen(
    onStartReview: () -> Unit,
    onLearnNew: (level: String) -> Unit,
    onExam: (ExamScope) -> Unit,
    onSpeak: () -> Unit,
    onKana: () -> Unit,
    onMyWords: () -> Unit,
    onProgress: () -> Unit,
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

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = ScreenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Greeting(state.streak)
        ReviewHero(state.dueToday, onStartReview)
        StatsStrip(state)
        GoalsCard(state, onProgress)

        SectionTitle("Learn new words", "নতুন শব্দ শিখুন • প্রতিবার ১০টি")
        LevelCard("N5", "Beginner", "প্রাথমিক স্তর", state.level("N5"), state.newN5,
            MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer) { onLearnNew("N5") }
        LevelCard("N4", "Elementary", "দ্বিতীয় স্তর", state.level("N4"), state.newN4,
            MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer) { onLearnNew("N4") }
        if ((state.level(VocabEntity.LEVEL_MINE)?.total ?: 0) > 0) {
            LevelCard("My", "My own words", "নিজের যোগ করা শব্দ", state.level(VocabEntity.LEVEL_MINE),
                state.newMine, Extra.correct, Extra.correctSoft) { onLearnNew(VocabEntity.LEVEL_MINE) }
        }

        SectionTitle("Practice", "অনুশীলন")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridTile(Icons.Rounded.Quiz, "Today's exam", "আজকের পরীক্ষা", { onExam(ExamScope.TODAY) },
                Modifier.weight(1f).testTag("home_exam"))
            GridTile(Icons.Rounded.Forum, "Conversation", "প্রতিদিনের কথা", onSpeak,
                Modifier.weight(1f).testTag("home_speak"),
                tint = MaterialTheme.colorScheme.secondary, tintBackground = MaterialTheme.colorScheme.secondaryContainer)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridTile(Icons.Rounded.Translate, "Kana", "হিরাগানা ও কাতাকানা", onKana,
                Modifier.weight(1f).testTag("home_kana"),
                tint = Extra.streak, tintBackground = Extra.streak.copy(alpha = 0.15f))
            GridTile(Icons.AutoMirrored.Rounded.MenuBook, "My words", "আমার শব্দ ও বাক্য", onMyWords,
                Modifier.weight(1f).testTag("home_words"),
                tint = Extra.correct, tintBackground = Extra.correctSoft)
        }

        ReminderCard(
            enabled = reminder.enabled,
            label = reminder.label,
            onToggle = { vm.setReminder(it) },
            onChangeTime = {
                TimePickerDialog(context, { _, h, m -> vm.setReminder(true, h, m) },
                    reminder.hour, reminder.minute, false).show()
            }
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Greeting(streak: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("こんにちは!", style = MaterialTheme.typography.headlineLarge)
            Text("Let's learn Japanese today • আজ জাপানি শিখি",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Pill(
            "$streak", icon = Icons.Rounded.LocalFireDepartment,
            color = Extra.streak.copy(alpha = 0.15f), textColor = Extra.streak,
        )
    }
}

/** Gradient card with today's review count. A red "sun" circle nods to the Japanese flag. */
@Composable
private fun ReviewHero(due: Int, onStart: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Extra.heroStart, Extra.heroEnd)))
    ) {
        val sun = MaterialTheme.colorScheme.secondary
        Canvas(Modifier.matchParentSize()) {
            drawCircle(sun.copy(alpha = 0.9f), radius = size.minDimension * 0.22f,
                center = Offset(size.width * 0.86f, size.height * 0.24f))
            drawCircle(Color.White.copy(alpha = 0.06f), radius = size.minDimension * 0.75f,
                center = Offset(size.width * 0.95f, size.height * 1.05f))
        }
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("TODAY'S REVIEW • আজকের রিভিউ", style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$due", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 60.sp)
                Spacer(Modifier.width(8.dp))
                Text(if (due == 1) "word due" else "words due", style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 10.dp))
            }
            Text(
                if (due > 0) "Say each answer out loud before you check. • উত্তর দেখার আগে জোরে বলুন।"
                else "All caught up! Learn new words below. • আজ রিভিউ বাকি নেই।",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onStart,
                enabled = due > 0,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = Extra.heroStart,
                    disabledContainerColor = Color.White.copy(alpha = 0.18f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("start_review"),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("Start Review • রিভিউ শুরু", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun StatsStrip(state: HomeUiState) {
    SoftCard(padding = 16.dp) {
        Row {
            Stat("${state.learning}", "Learning", "শিখছি", Modifier.weight(1f))
            Stat("${state.mastered}", "Mastered", "আয়ত্ত", Modifier.weight(1f))
            Stat("${state.today.learned}", "New today", "আজ নতুন", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(value: String, en: String, bn: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(en, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(bn, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GoalsCard(state: HomeUiState, onProgress: () -> Unit) {
    val goals = state.goals
    val done = goals.count { it.done }
    SoftCard(onClick = onProgress, modifier = Modifier.testTag("home_progress")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(done / goals.size.toFloat(), size = 58.dp,
                color = if (done == goals.size) Extra.correct else MaterialTheme.colorScheme.primary) {
                Text("$done/${goals.size}", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.width(14.dp))
            Bilingual(
                "Today's goals",
                if (done == goals.size) "সব লক্ষ্য পূর্ণ! お疲れ様!" else "আজকের লক্ষ্য",
                Modifier.weight(1f), enStyle = MaterialTheme.typography.titleLarge,
            )
            Text("Progress ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        goals.forEach { g ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (g.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                    tint = if (g.done) Extra.correct else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Bilingual(g.en, g.bn, Modifier.weight(1f),
                    enStyle = MaterialTheme.typography.bodyLarge, bnStyle = MaterialTheme.typography.bodyMedium)
                if (g.count != null) {
                    Text(g.count, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    badge: String,
    en: String,
    bn: String,
    progress: LevelProgress?,
    newCount: Int,
    tint: Color,
    tintSoft: Color,
    onClick: () -> Unit,
) {
    val total = progress?.total ?: 0
    val learned = progress?.learned ?: 0
    SoftCard(
        onClick = if (newCount > 0) onClick else null,
        modifier = Modifier.testTag("learn_$badge"),
        padding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(tintSoft),
                contentAlignment = Alignment.Center
            ) { Text(badge, style = MaterialTheme.typography.titleLarge, color = tint) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Bilingual(if (badge == "My") en else "JLPT $badge · $en", bn, Modifier.weight(1f))
                    Text("$newCount new", style = MaterialTheme.typography.labelLarge, color = tint)
                }
                ProgressBar(if (total == 0) 0f else learned / total.toFloat(), color = tint, height = 8.dp)
                Text("$learned / $total learned • শেখা হয়েছে", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReminderCard(enabled: Boolean, label: String, onToggle: (Boolean) -> Unit, onChangeTime: () -> Unit) {
    SoftCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.NotificationsActive, MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primaryContainer)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).clickable(enabled = enabled, onClick = onChangeTime)) {
                Text("Daily reminder", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (enabled) "Every day at $label · tap to change" else "Off • বন্ধ",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("প্রতিদিনের রিমাইন্ডার", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
