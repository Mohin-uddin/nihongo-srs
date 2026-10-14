package com.nihongo.srs.ui.players

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.Player
import com.nihongo.srs.data.PlayerStore
import com.nihongo.srs.ui.TtsBanner
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.BigButton
import com.nihongo.srs.ui.components.BigOutlinedButton
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.EmptyState
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SessionProgress
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.exam.OptionCard
import com.nihongo.srs.ui.exam.OptionState
import com.nihongo.srs.ui.exam.QuestionType
import com.nihongo.srs.ui.theme.Extra

/** A colour per player, so each person is easy to spot. */
@Composable
fun playerColor(p: Player): Color = when ((p.id - 1) % 4) {
    0 -> MaterialTheme.colorScheme.primary
    1 -> MaterialTheme.colorScheme.secondary
    2 -> Extra.correct
    else -> Extra.streak
}

@Composable
fun Avatar(p: Player, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(playerColor(p)),
        contentAlignment = Alignment.Center,
    ) {
        Text(p.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp)
    }
}

/** Scoreboard: who has more points, who is studying now, and the quiz battle. */
@Composable
fun PlayersScreen(
    onBack: () -> Unit,
    onBattle: () -> Unit,
    vm: PlayersViewModel = viewModel(factory = PlayersViewModel.Factory),
) {
    val players by vm.players.collectAsStateWithLifecycle()
    val activeId by vm.activeId.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Player?>(null) }
    var adding by remember { mutableStateOf(false) }

    AppScreen(title = "Scoreboard", subtitle = "কে কত পয়েন্ট পেল", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LeaderCard(players)
            SectionTitle("Players", "যে পড়ছে তাকে বেছে নিন, পয়েন্ট তার নামে যাবে")
            players.sortedByDescending { it.points }.forEach { p ->
                PlayerRow(p, p.id == activeId, onSelect = { vm.setActive(p.id) }, onEdit = { editing = p })
            }
            if (players.size < PlayerStore.MAX_PLAYERS) {
                BigOutlinedButton(
                    if (players.size == 1) "Add your partner" else "Add a player",
                    "আরেকজন যোগ করুন (যেমন: স্ত্রী)",
                    onClick = { adding = true }, icon = Icons.Rounded.PersonAdd,
                    modifier = Modifier.testTag("players_add"),
                )
            }
            BigButton(
                "Play Quiz Battle", "কুইজ যুদ্ধ খেলুন • ২ জন, ১টি ফোন",
                onClick = onBattle, icon = Icons.Rounded.SportsEsports,
                container = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("players_battle"),
            )
            SectionTitle("How to earn points", "কীভাবে পয়েন্ট পাবেন")
            SoftCard(padding = 16.dp) {
                pointRules.forEach { (en, bn, pts) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Bilingual(en, bn, Modifier.weight(1f), enStyle = MaterialTheme.typography.bodyLarge)
                        Pill("+$pts", color = Extra.correctSoft, textColor = Extra.correct)
                    }
                }
            }
            Text(
                "You share one word list on this phone. For separate review schedules, install the app on each phone.",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    editing?.let { p ->
        NameDialog(
            title = "Edit player • নাম বদলান", initial = p.name,
            onDismiss = { editing = null },
            onSave = { vm.rename(p.id, it); editing = null },
            onDelete = if (players.size > 1) ({ vm.remove(p.id); editing = null }) else null,
        )
    }
    if (adding) {
        NameDialog(
            title = "New player • নতুন খেলোয়াড়", initial = "",
            onDismiss = { adding = false },
            onSave = { vm.add(it); adding = false },
        )
    }
}

@Composable
private fun LeaderCard(players: List<Player>) {
    val sorted = players.sortedByDescending { it.points }
    val leader = sorted.first()
    val second = sorted.getOrNull(1)
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Extra.heroStart, Extra.heroEnd)))
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LEADER • এগিয়ে আছে", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👑", fontSize = 36.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(leader.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("${leader.points} points • পয়েন্ট", style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.85f))
                }
            }
            Text(
                when {
                    second == null -> "Add your partner and compete! • সঙ্গীকে যোগ করে প্রতিযোগিতা করুন!"
                    leader.points == second.points -> "It's a tie! • সমান সমান!"
                    else -> "${leader.name} is ahead of ${second.name} by ${leader.points - second.points} points • " +
                        "${leader.points - second.points} পয়েন্ট এগিয়ে"
                },
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun PlayerRow(p: Player, active: Boolean, onSelect: () -> Unit, onEdit: () -> Unit) {
    SoftCard(
        onClick = onSelect, padding = 14.dp,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("player_${p.id}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Pill("Studying now", color = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Text("Today +${p.todayPoints()} · ${p.gamesWon} wins • আজ, জয়",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${p.points}", style = MaterialTheme.typography.headlineSmall, color = playerColor(p))
                Text("points", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit ${p.name}") }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(name, { name = it.take(20) }, singleLine = true,
                label = { Text("Name • নাম") }, modifier = Modifier.testTag("player_name"))
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save • সেভ") } },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Remove", color = Extra.wrong) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** Two players, one phone: take turns answering, then see who won. */
@Composable
fun BattleScreen(
    onBack: () -> Unit,
    onPlayers: () -> Unit,
    vm: BattleViewModel = viewModel(factory = BattleViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tts by vm.ttsStatus.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshPlayers() }

    AppScreen(title = "Quiz Battle", subtitle = "কুইজ যুদ্ধ", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.phase) {
                BattlePhase.SETUP -> Setup(state, vm, onPlayers)
                BattlePhase.HANDOFF -> Handoff(state, vm)
                BattlePhase.QUESTION -> {
                    TtsBanner(tts)
                    BattleQuestion(state, vm)
                }
                BattlePhase.FINISHED -> Finished(state, vm, onBack)
            }
        }
    }
}

@Composable
private fun Setup(state: BattleState, vm: BattleViewModel, onPlayers: () -> Unit) {
    SoftCard(padding = 20.dp) {
        Text("⚔️", fontSize = 44.sp)
        Bilingual(
            "Take turns on one phone. ${BattleViewModel.ROUNDS} questions each. " +
                "Right answer +10, three in a row +5 bonus. The winner gets +20 on the scoreboard.",
            "একটি ফোনে পালা করে খেলুন। প্রত্যেকে ${BattleViewModel.ROUNDS}টি প্রশ্ন। সঠিক উত্তরে +১০, " +
                "পরপর ৩টি সঠিক হলে +৫ বোনাস। বিজয়ী স্কোরবোর্ডে +২০ পাবে।",
            enStyle = MaterialTheme.typography.bodyLarge,
        )
    }
    if (state.players.size < 2) {
        EmptyState("👫", "Add a second player first", "আগে আরেকজন খেলোয়াড় যোগ করুন") {
            BigButton("Add player", "খেলোয়াড় যোগ করুন", onClick = onPlayers, icon = Icons.Rounded.PersonAdd)
        }
        return
    }
    SectionTitle("Who is playing?", "কে কে খেলবে? দুজন বেছে নিন")
    state.players.forEach { p ->
        val chosen = p.id in state.chosen
        SoftCard(
            onClick = { vm.toggleChoose(p.id) }, padding = 14.dp,
            color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(p)
                Spacer(Modifier.width(12.dp))
                Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (chosen) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null,
                    tint = if (chosen) Extra.correct else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
    if (state.notEnoughWords) {
        Text("Not enough words yet • এখনো যথেষ্ট শব্দ নেই", color = Extra.wrong)
    }
    BigButton(
        "Start battle", "খেলা শুরু", onClick = vm::start, enabled = state.chosen.size == 2 && !state.isLoading,
        icon = Icons.Rounded.SportsEsports, container = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.testTag("battle_start"),
    )
}

@Composable
private fun ScoreStrip(state: BattleState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        state.sides.forEachIndexed { i, side ->
            val turn = i == state.turn && state.phase != BattlePhase.FINISHED
            SoftCard(
                Modifier.weight(1f), padding = 12.dp,
                color = if (turn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(side.player, 34.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(side.player.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(if (side.streak >= 2) "🔥 ${side.streak}" else " ", style = MaterialTheme.typography.labelSmall)
                    }
                    Text("${side.score}", style = MaterialTheme.typography.headlineSmall, color = playerColor(side.player))
                }
            }
        }
    }
}

@Composable
private fun Handoff(state: BattleState, vm: BattleViewModel) {
    val side = state.sides[state.turn]
    ScoreStrip(state)
    SessionProgress(state.index, state.questions.size,
        label = "Round ${state.round} / ${BattleViewModel.ROUNDS}")
    SoftCard(padding = 28.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(side.player, 84.dp)
            Bilingual(
                "${side.player.name}'s turn",
                "এবার ${side.player.name}-এর পালা। ফোনটি দিন!",
                enStyle = MaterialTheme.typography.headlineMedium, align = TextAlign.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            )
            Button(
                onClick = vm::ready,
                colors = ButtonDefaults.buttonColors(containerColor = playerColor(side.player)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag("battle_ready"),
            ) { Text("I'm ready • আমি প্রস্তুত", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun BattleQuestion(state: BattleState, vm: BattleViewModel) {
    val q = state.current ?: return
    ScoreStrip(state)
    SoftCard(padding = 22.dp) {
        Column(Modifier.fillMaxWidth().heightIn(min = 130.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
            when (q.type) {
                QuestionType.JP_TO_MEANING -> {
                    Pill("What does this mean? • এর মানে কী?", color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(q.word.japanese, fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (state.selected != null) Text("${q.word.reading} · ${q.word.romaji}",
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                }
                QuestionType.MEANING_TO_JP -> {
                    Pill("Which is the Japanese? • জাপানি কোনটি?", color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(q.word.meaning, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(q.word.meaningBn, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                QuestionType.LISTEN -> {
                    Pill("Listen and choose • শুনে বাছুন", color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(onClick = vm::replay, shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Play again",
                                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(38.dp))
                        }
                    }
                    if (state.selected != null) Text("${q.word.japanese}  ${q.word.reading}", fontSize = 26.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    q.options.forEachIndexed { i, option ->
        val showJp = q.type == QuestionType.MEANING_TO_JP
        OptionCard(
            letter = "ABCD".getOrElse(i) { ' ' }.toString(),
            main = if (showJp) option.japanese else option.meaning,
            sub = if (showJp) null else option.meaningBn,
            state = when {
                state.selected == null -> OptionState.NORMAL
                i == q.correctIndex -> OptionState.CORRECT
                i == state.selected -> OptionState.WRONG
                else -> OptionState.DIMMED
            },
            enabled = state.selected == null,
            onClick = { vm.select(i) },
            tag = "battle_option$i",
        )
    }
    if (state.selected != null) {
        val correct = state.selected == q.correctIndex
        SoftCard(color = if (correct) Extra.correctSoft else Extra.wrongSoft, border = false, padding = 14.dp) {
            Bilingual(
                if (correct) "Correct! +${state.lastGain}" else "Not this time",
                if (correct) "সঠিক!" else "${q.word.japanese} = ${q.word.meaning} • ${q.word.meaningBn}",
                color = if (correct) Extra.correct else Extra.wrong,
            )
        }
        val last = state.index + 1 >= state.questions.size
        BigButton(if (last) "See who won" else "Next player", if (last) "ফলাফল দেখুন" else "পরের জন",
            onClick = vm::next, modifier = Modifier.testTag("battle_next"))
    }
}

@Composable
private fun Finished(state: BattleState, vm: BattleViewModel, onBack: () -> Unit) {
    val winner = state.winner
    SoftCard(padding = 26.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = Extra.streak, modifier = Modifier.size(72.dp))
            Bilingual(
                if (winner != null) "${winner.player.name} wins! 🎉" else "It's a draw!",
                if (winner != null) "${winner.player.name} জিতেছে! স্কোরবোর্ডে +20" else "সমান সমান! দুজনেই ভালো খেলেছেন",
                enStyle = MaterialTheme.typography.headlineMedium, align = TextAlign.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            )
        }
    }
    state.sides.sortedByDescending { it.score }.forEach { side ->
        SoftCard(padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(side.player)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(side.player.name, style = MaterialTheme.typography.titleMedium)
                    Text("${side.correct} / ${BattleViewModel.ROUNDS} correct • সঠিক",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${side.score}", style = MaterialTheme.typography.headlineMedium, color = playerColor(side.player))
            }
        }
    }
    if (state.isLoading) CircularProgressIndicator()
    BigButton("Play again", "আবার খেলুন", onClick = vm::playAgain, icon = Icons.Rounded.SportsEsports,
        container = MaterialTheme.colorScheme.secondary, modifier = Modifier.testTag("battle_again"))
    BigOutlinedButton("Done", "শেষ", onClick = onBack)
}
