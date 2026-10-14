package com.nihongo.srs.ui.news

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.news.RadioEpisode
import com.nihongo.srs.news.RadioState
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.BigOutlinedButton
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.ChoiceChip
import com.nihongo.srs.ui.components.EmptyState
import com.nihongo.srs.ui.components.IconBadge
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.theme.Extra
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a")

/** NHK radio news for listening practice. Keeps playing when you leave this screen. */
@Composable
fun RadioScreen(
    onBack: () -> Unit,
    vm: RadioViewModel = viewModel(factory = RadioViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val episodes by vm.episodes.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()

    AppScreen(
        title = "Radio news", subtitle = "জাপানি রেডিও খবর শুনুন • NHK", onBack = onBack,
        actions = { IconButton(onClick = { vm.refresh() }) { Icon(Icons.Rounded.Refresh, "Refresh") } },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayerCard(player, vm)
            SoftCard(color = MaterialTheme.colorScheme.primaryContainer, border = false, padding = 16.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Bilingual(
                        "This is real news at native speed. Don't try to understand everything: " +
                            "just listen for words you know. Try 0.75x speed.",
                        "সব বুঝতে হবে না। চেনা শব্দগুলো কান দিয়ে ধরার চেষ্টা করুন। প্রতিদিন শুনলে কান অভ্যস্ত হবে।",
                        enStyle = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            SectionTitle("Episodes", "পুরনো খবরও শুনতে পারেন")
            when {
                state.isLoading && episodes.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null && episodes.isEmpty() -> EmptyState("📻", state.error!!.substringBefore(" • "),
                    state.error!!.substringAfter(" • ", "")) {
                    BigOutlinedButton("Try again", "আবার চেষ্টা", onClick = { vm.refresh() })
                }
                else -> episodes.forEachIndexed { i, e ->
                    EpisodeRow(e, player, Modifier.testTag("radio_episode_$i")) { vm.play(e) }
                }
            }
            Text("Audio: NHKラジオニュース podcast, free from NHK.", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PlayerCard(p: RadioState, vm: RadioViewModel) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(Extra.heroStart, Extra.heroEnd)))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("NOW PLAYING • এখন চলছে", style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f))
            Text(
                p.episode?.title ?: "Pick an episode below • নিচ থেকে একটি বেছে নিন",
                style = MaterialTheme.typography.titleLarge, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            p.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFC9C9)) }

            var dragging by remember { mutableStateOf<Float?>(null) }
            val duration = p.durationMs.coerceAtLeast(1)
            Slider(
                value = dragging ?: (p.positionMs / duration.toFloat()),
                onValueChange = { dragging = it },
                onValueChangeFinished = { dragging?.let { vm.seekTo((it * duration).toInt()) }; dragging = null },
                enabled = p.episode != null && !p.isPreparing,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
            )
            Row {
                Text(time(p.positionMs), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f))
                Text(time(p.durationMs), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.skip(-10_000) }, enabled = p.episode != null) {
                    Icon(Icons.Rounded.Replay10, "Back 10 seconds", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                FilledIconButton(
                    onClick = vm::toggle,
                    enabled = p.episode != null,
                    modifier = Modifier.size(68.dp).testTag("radio_play"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White, contentColor = Extra.heroStart,
                        disabledContainerColor = Color.White.copy(alpha = 0.2f),
                    ),
                ) {
                    if (p.isPreparing) CircularProgressIndicator(Modifier.size(28.dp), color = Extra.heroStart)
                    else Icon(if (p.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (p.isPlaying) "Pause" else "Play", Modifier.size(38.dp))
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { vm.skip(10_000) }, enabled = p.episode != null) {
                    Icon(Icons.Rounded.Forward10, "Forward 10 seconds", tint = Color.White)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                listOf(0.75f, 1f).forEach { s ->
                    ChoiceChip(if (s == 1f) "1x normal" else "0.75x slow", selected = p.speed == s, onClick = { vm.setSpeed(s) })
                }
            }
            p.episode?.description?.takeIf { it.isNotBlank() }?.let {
                Text(it.lines().filter { l -> l.startsWith("▽") && !l.contains("らじる") }.joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
private fun EpisodeRow(e: RadioEpisode, p: RadioState, modifier: Modifier, onClick: () -> Unit) {
    val current = p.episode?.audioUrl == e.audioUrl
    SoftCard(
        modifier, onClick = onClick, padding = 14.dp,
        color = if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (current && p.isPlaying) Icons.Rounded.Pause else Icons.Rounded.Headphones,
                MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        e.published?.withZoneSameInstant(ZoneId.systemDefault())?.format(dayFormat),
                        e.duration.removePrefix("00:").takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun time(ms: Int): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
