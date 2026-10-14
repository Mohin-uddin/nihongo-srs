package com.nihongo.srs.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.LevelProgress
import com.nihongo.srs.data.Player
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ProgressBar
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.exam.ExamScope
import com.nihongo.srs.ui.players.Avatar
import com.nihongo.srs.ui.theme.Extra

/**
 * The "আজ" (Today) tab: only today's three steps, in order.
 * Everything else lives in its own tab so this page stays simple.
 */
@Composable
fun HomeScreen(
    onStartReview: () -> Unit,
    onLearnNew: (level: String) -> Unit,
    onExam: (ExamScope) -> Unit,
    onPlayers: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val players by vm.players.collectAsStateWithLifecycle()
    val activeId by vm.activeId.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshDay() }

    // Android 13+: ask once for permission to show the daily reminder.
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && vm.shouldAskNotificationPermission) {
            vm.onNotificationPermissionAsked()
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val player = players.firstOrNull { it.id == activeId } ?: players.first()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = ScreenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Greeting(player, state.streak, onPlayers)

        SectionTitle("আজকের ৩টি কাজ", "Today's 3 steps · ওপর থেকে নিচে করুন")

        val reviewDone = state.dueToday == 0
        StepCard(
            n = 1, done = reviewDone,
            title = "পুরনো শব্দ রিভিউ",
            sub = if (reviewDone) "আজ রিভিউ বাকি নেই ✓" else "${state.dueToday}টি শব্দ আজ মনে করার পালা",
            en = "Review",
            button = if (reviewDone) null else "রিভিউ শুরু",
            onClick = onStartReview,
            tag = "start_review",
        )

        val learned = state.today.learned
        val learnDone = learned >= HomeUiState.GOAL_NEW_WORDS
        StepCard(
            n = 2, done = learnDone,
            title = "নতুন ১০টি শব্দ শিখুন",
            sub = "আজ শিখেছেন $learned/${HomeUiState.GOAL_NEW_WORDS}। নিচে একটি লেভেল বেছে নিন",
            en = "Learn new words",
            button = null, onClick = {}, tag = "step_learn",
        ) {
            LevelRow("N5", "শুরুর লেভেল", state.level("N5"), state.newN5, MaterialTheme.colorScheme.primary) { onLearnNew("N5") }
            LevelRow("N4", "দ্বিতীয় লেভেল", state.level("N4"), state.newN4, MaterialTheme.colorScheme.secondary) { onLearnNew("N4") }
            if ((state.level(VocabEntity.LEVEL_MINE)?.total ?: 0) > 0) {
                LevelRow("My", "আমার যোগ করা শব্দ", state.level(VocabEntity.LEVEL_MINE), state.newMine, Extra.correct) {
                    onLearnNew(VocabEntity.LEVEL_MINE)
                }
            }
        }

        val examDone = state.today.exams >= 1
        StepCard(
            n = 3, done = examDone,
            title = "ছোট পরীক্ষা দিন",
            sub = when {
                examDone -> "আজ পরীক্ষা হয়ে গেছে ✓"
                learned == 0 && state.today.reviewed == 0 -> "আগে ১ বা ২ নম্বর কাজ করুন"
                else -> "আজ যা শিখলেন তা থেকে ২০টি প্রশ্ন"
            },
            en = "Quick exam",
            button = if (examDone) "আবার দিন" else "পরীক্ষা শুরু",
            enabled = learned > 0 || state.today.reviewed > 0,
            onClick = { onExam(ExamScope.TODAY) },
            tag = "home_exam",
        )

        if (reviewDone && learnDone && examDone) {
            SoftCard(color = Extra.correctSoft, border = false) {
                Bilingual("お疲れ様! আজকের সব কাজ শেষ!", "Great job! Try the News or Play tab for more.",
                    enStyle = MaterialTheme.typography.titleLarge, color = Extra.correct)
            }
        }

        SoftCard(color = MaterialTheme.colorScheme.primaryContainer, border = false, padding = 16.dp) {
            Text("নিচের ট্যাবগুলো", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "📰 খবর: জাপানি খবর পড়ুন ও শুনুন\n🎮 খেলা: স্কোর আর কুইজ যুদ্ধ\n📖 শব্দ: সব শব্দের বই\n☰ আরও: কানা, কথা বলা, অগ্রগতি, সেটিংস",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Greeting(player: Player, streak: Int, onPlayers: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(CircleShape).clickable(onClick = onPlayers).testTag("home_player")) { Avatar(player, 48.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("こんにちは, ${player.name}!", style = MaterialTheme.typography.headlineSmall)
            Text("আজ জাপানি শিখি • Let's learn", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill("${player.points} পয়েন্ট", icon = Icons.Rounded.EmojiEvents,
                color = Extra.streak.copy(alpha = 0.15f), textColor = Extra.streak)
            Pill("$streak দিন", icon = Icons.Rounded.LocalFireDepartment,
                color = MaterialTheme.colorScheme.secondaryContainer, textColor = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** One numbered step of today's plan. */
@Composable
private fun StepCard(
    n: Int,
    done: Boolean,
    title: String,
    sub: String,
    en: String,
    button: String?,
    onClick: () -> Unit,
    tag: String,
    enabled: Boolean = true,
    extra: @Composable () -> Unit = {},
) {
    SoftCard(color = if (done) Extra.correctSoft.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (done) Extra.correct else MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (done) Icon(Icons.Rounded.Check, null, tint = Color.White)
                else Text("$n", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text("$en · $sub", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        extra()
        if (button != null) {
            Button(
                onClick = onClick, enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(tag),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text(button, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun LevelRow(
    badge: String,
    bn: String,
    progress: LevelProgress?,
    newCount: Int,
    tint: Color,
    onClick: () -> Unit,
) {
    val total = progress?.total ?: 0
    val learned = progress?.learned ?: 0
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(enabled = newCount > 0, onClick = onClick)
            .padding(12.dp)
            .testTag("learn_$badge"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Text(badge, style = MaterialTheme.typography.titleMedium, color = tint) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(bn, style = MaterialTheme.typography.titleMedium)
            ProgressBar(if (total == 0) 0f else learned / total.toFloat(), color = tint, height = 6.dp)
            Text("$learned / $total শেখা", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Text("$newCount new", style = MaterialTheme.typography.labelLarge, color = tint)
    }
}
