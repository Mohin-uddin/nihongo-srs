package com.nihongo.srs.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.srs.data.KanaRomaji
import com.nihongo.srs.data.Points
import com.nihongo.srs.news.NewsItem
import com.nihongo.srs.news.NewsSentence
import com.nihongo.srs.news.NewsToken
import com.nihongo.srs.news.ReadMode
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.BigButton
import com.nihongo.srs.ui.components.BigOutlinedButton
import com.nihongo.srs.ui.components.Bilingual
import com.nihongo.srs.ui.components.ChoiceChip
import com.nihongo.srs.ui.components.EmptyState
import com.nihongo.srs.ui.components.MenuTile
import com.nihongo.srs.ui.components.Pill
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.theme.Extra
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a")

/** List of today's NHK headlines. */
@Composable
fun NewsListScreen(
    onBack: () -> Unit,
    onOpen: (NewsItem) -> Unit,
    onRadio: () -> Unit,
    onAddKey: () -> Unit,
    vm: NewsListViewModel = viewModel(factory = NewsListViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val headlines by vm.headlines.collectAsStateWithLifecycle()

    AppScreen(
        title = "Japanese news", subtitle = "আজকের জাপানি খবর • NHK", onBack = onBack,
        actions = { IconButton(onClick = { vm.refresh() }) { Icon(Icons.Rounded.Refresh, "Refresh") } },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MenuTile(
                Icons.Rounded.Headphones, "Listen: NHK radio news", "রেডিও খবর শুনুন", onRadio,
                Modifier.testTag("news_radio"),
                tint = MaterialTheme.colorScheme.secondary, tintBackground = MaterialTheme.colorScheme.secondaryContainer,
            )
            if (!vm.hasKey) {
                SoftCard(color = MaterialTheme.colorScheme.primaryContainer, border = false, padding = 16.dp) {
                    Bilingual(
                        "Add your free Gemini key to get an easy version of each story and the meaning of every word.",
                        "Gemini key দিলে প্রতিটি খবরের সহজ রূপ আর প্রতিটি শব্দের মানে দেখাবে। এখন শুধু অ্যাপে থাকা শব্দের মানে দেখাবে।",
                        enStyle = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    TextButton(onClick = onAddKey) {
                        Icon(Icons.Rounded.Key, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add key • key দিন")
                    }
                }
            }
            SectionTitle("Today's stories", "যেকোনো খবরে চাপ দিন, তারপর শব্দে চাপ দিয়ে মানে দেখুন")
            when {
                state.isLoading && headlines.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null && headlines.isEmpty() -> EmptyState("📡", state.error!!.substringBefore(" • "),
                    state.error!!.substringAfter(" • ", "")) {
                    BigOutlinedButton("Try again", "আবার চেষ্টা", onClick = { vm.refresh() })
                }
                else -> headlines.forEachIndexed { i, item -> HeadlineCard(item, i) { onOpen(item) } }
            }
            Text(
                "News from NHK (Japan's public broadcaster), shown for personal study.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeadlineCard(item: NewsItem, index: Int, onClick: () -> Unit) {
    SoftCard(onClick = onClick, padding = 16.dp, modifier = Modifier.testTag("news_item_$index")) {
        Text(item.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 26.sp))
        if (item.summary.isNotBlank()) {
            Text(item.summary, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Newspaper, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(
                item.published?.withZoneSameInstant(ZoneId.systemDefault())?.format(timeFormat) ?: "NHK",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text("Read ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** One story: tap any word for its Bangla + English meaning, and add it to your words. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    onBack: () -> Unit,
    onAddKey: () -> Unit,
    vm: ArticleViewModel = viewModel(factory = ArticleViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val known by vm.known.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    AppScreen(
        title = "Read the news", subtitle = "শব্দে চাপ দিন, মানে দেখুন", onBack = onBack, snackbar = snackbar,
        actions = {
            IconButton(onClick = vm::speakAll) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen to all") }
            IconButton(onClick = vm::stopSpeaking) { Icon(Icons.Rounded.Stop, "Stop") }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadMode.entries.forEach { m ->
                    ChoiceChip("${m.en} • ${m.bn}", selected = state.mode == m,
                        onClick = { if (vm.hasKey || m == ReadMode.ORIGINAL) vm.setMode(m) else onAddKey() })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Bilingual("Show reading above kanji", "কানজির উপরে উচ্চারণ", Modifier.weight(1f),
                    enStyle = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.furigana, onCheckedChange = { vm.toggleFurigana() })
            }

            val reading = state.reading
            when {
                state.isLoading -> Column(
                    Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Bilingual(
                        if (state.mode == ReadMode.ORIGINAL) "Preparing the words…" else "Making an easy version…",
                        "একটু অপেক্ষা করুন", horizontalAlignment = Alignment.CenterHorizontally,
                    )
                }
                reading == null -> EmptyState("📰", state.error?.substringBefore(" • ") ?: "Couldn't open",
                    state.error?.substringAfter(" • ", "") ?: "") {
                    BigOutlinedButton("Try again", "আবার চেষ্টা", onClick = vm::retry)
                }
                else -> {
                    if (state.error != null) {
                        SoftCard(color = Extra.wrongSoft, border = false, padding = 14.dp) {
                            Bilingual(state.error!!.substringBefore(" • "),
                                "${state.error!!.substringAfter(" • ", "")} • আসল খবর দেখানো হচ্ছে",
                                enStyle = MaterialTheme.typography.bodyLarge, color = Extra.wrong)
                            TextButton(onClick = vm::retry) { Text("Try again • আবার চেষ্টা") }
                        }
                    } else if (!reading.fromAi) {
                        SoftCard(color = MaterialTheme.colorScheme.primaryContainer, border = false, padding = 14.dp) {
                            Bilingual(
                                "Underlined words are in the app. Add a Gemini key to see every word.",
                                "দাগ দেওয়া শব্দগুলোর মানে দেখা যাবে। সব শব্দের জন্য Gemini key দিন।",
                                enStyle = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    SentenceCard(reading.title, -1, state, known, vm, isTitle = true)
                    reading.sentences.forEachIndexed { i, s -> SentenceCard(s, i, state, known, vm) }
                    Legend()
                    Text("Source: NHK • ${state.item?.link.orEmpty()}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    state.selected?.let { sel ->
        ModalBottomSheet(
            onDismissRequest = vm::clearSelection,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            WordSheet(sel, known, vm, onAddKey)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceCard(
    s: NewsSentence,
    index: Int,
    state: ArticleState,
    known: Set<String>,
    vm: ArticleViewModel,
    isTitle: Boolean = false,
) {
    SoftCard(padding = 16.dp, modifier = Modifier.testTag(if (isTitle) "news_title" else "news_sentence_$index")) {
        FlowRow(verticalArrangement = Arrangement.Bottom) {
            s.tokens.forEachIndexed { ti, t ->
                TokenView(
                    t, isTitle, state.furigana,
                    selected = state.selected?.token === t,
                    known = t.base in known || t.text in known,
                    modifier = Modifier.testTag("token_${index}_$ti"),
                ) { vm.select(t, s) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { vm.speak(s.text) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen", Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            if (s.english.isNotBlank() || s.bangla.isNotBlank()) {
                TextButton(onClick = { vm.toggleMeaning(index) }) {
                    Icon(Icons.Rounded.Translate, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (index in state.openMeanings) "Hide meaning" else "Meaning • মানে")
                }
            }
        }
        if (index in state.openMeanings) {
            Bilingual(s.english, s.bangla, enStyle = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** One tappable word, with its reading above when it has kanji. */
@Composable
private fun TokenView(
    t: NewsToken,
    isTitle: Boolean,
    furigana: Boolean,
    selected: Boolean,
    known: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val tappable = !t.isPunctuation
    val line = when {
        known -> Extra.correct
        t.hasMeaning -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    val hasKanji = t.text.any { it in '一'..'鿿' || it == '々' }
    Column(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .then(if (tappable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .drawBehind {
                if (tappable && line != Color.Transparent) {
                    val y = size.height - 1.dp.toPx()
                    drawLine(line, Offset(2.dp.toPx(), y), Offset(size.width - 2.dp.toPx(), y), 2.dp.toPx())
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (furigana) {
            Text(
                if (hasKanji) t.kana else " ", fontSize = 10.sp, lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(t.text, fontSize = if (isTitle) 24.sp else 21.sp, lineHeight = if (isTitle) 32.sp else 28.sp)
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("Green line = in your words", color = Extra.correctSoft, textColor = Extra.correct)
        Pill("Tap = meaning", color = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun WordSheet(sel: SelectedWord, known: Set<String>, vm: ArticleViewModel, onAddKey: () -> Unit) {
    val t = sel.token
    val base = t.base.ifBlank { t.text }
    val baseKana = t.baseKana.ifBlank { t.kana }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(base, fontSize = 36.sp, lineHeight = 42.sp)
                if (baseKana.isNotBlank() && baseKana != base) {
                    Text("$baseKana · ${KanaRomaji.convert(baseKana)}", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            FilledTonalIconButton(onClick = { vm.speak(baseKana.ifBlank { base }) }) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen")
            }
        }
        if (t.text != base) {
            Text("In the news: ${t.text}${if (t.kana.isNotBlank()) " (${t.kana})" else ""}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (t.hasMeaning) {
            SoftCard(color = MaterialTheme.colorScheme.surfaceContainerLow, border = false, padding = 16.dp) {
                Text(t.bangla, style = MaterialTheme.typography.headlineSmall)
                Text(t.english, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val already = base in known
            BigButton(
                if (already) "Already in your words" else "Add to My words  +${Points.NEWS_WORD}",
                if (already) "শব্দটি আগেই আছে" else "আমার শব্দে যোগ করুন, পরে রিভিউতে আসবে",
                onClick = { vm.addWord(sel) }, enabled = !already, icon = Icons.Rounded.Add,
                modifier = Modifier.testTag("news_add_word"),
            )
        } else {
            Bilingual(
                "This word isn't in the app yet. Add your free Gemini key to see every word's meaning.",
                "এই শব্দটি অ্যাপে নেই। সব শব্দের মানে দেখতে Gemini key দিন।",
                enStyle = MaterialTheme.typography.bodyLarge,
            )
            BigOutlinedButton("Add key", "key দিন", onClick = { vm.clearSelection(); onAddKey() }, icon = Icons.Rounded.Key)
        }
    }
}
