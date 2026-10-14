package com.nihongo.srs.ui.news

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.ai.GeminiClient
import com.nihongo.srs.data.AppPrefs
import com.nihongo.srs.data.PlayerStore
import com.nihongo.srs.data.Points
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.news.NewsFeeds
import com.nihongo.srs.news.NewsItem
import com.nihongo.srs.news.NewsReading
import com.nihongo.srs.news.NewsRepository
import com.nihongo.srs.news.NewsSentence
import com.nihongo.srs.news.NewsToken
import com.nihongo.srs.news.RadioEpisode
import com.nihongo.srs.news.RadioPlayer
import com.nihongo.srs.news.ReadMode
import com.nihongo.srs.speech.JapaneseSpeaker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsListState(
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** Today's NHK headlines. */
class NewsListViewModel(private val news: NewsRepository) : ViewModel() {

    val headlines: StateFlow<List<NewsItem>> = news.headlines

    private val _state = MutableStateFlow(NewsListState(isLoading = news.headlines.value.isEmpty()))
    val state: StateFlow<NewsListState> = _state.asStateFlow()

    val hasKey: Boolean get() = news.hasKey

    init {
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) = viewModelScope.launch {
        _state.update { it.copy(isLoading = news.headlines.value.isEmpty() || force, error = null) }
        _state.value = try {
            news.refreshHeadlines(force)
            NewsListState(isLoading = false)
        } catch (e: NewsFeeds.FeedException) {
            NewsListState(isLoading = false, error = e.message)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { NewsListViewModel((this[APPLICATION_KEY] as NihongoApp).news) }
        }
    }
}

data class ArticleState(
    val item: NewsItem? = null,
    val mode: ReadMode = ReadMode.EASY_N5,
    val isLoading: Boolean = true,
    val reading: NewsReading? = null,
    val error: String? = null,
    val furigana: Boolean = true,
    /** Sentence indexes whose translation is shown. -1 = title. */
    val openMeanings: Set<Int> = emptySet(),
    val selected: SelectedWord? = null,
)

/** A tapped word and the sentence it was in. */
data class SelectedWord(val token: NewsToken, val sentence: NewsSentence)

/** One news story with every word tappable. */
class ArticleViewModel(
    private val news: NewsRepository,
    private val repo: VocabRepository,
    private val prefs: AppPrefs,
    private val speaker: JapaneseSpeaker,
    private val players: PlayerStore,
    link: String,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ArticleState(
            item = news.item(link),
            mode = if (news.hasKey) runCatching { ReadMode.valueOf(prefs.newsMode) }.getOrDefault(ReadMode.EASY_N5)
            else ReadMode.ORIGINAL,
        )
    )
    val state: StateFlow<ArticleState> = _state.asStateFlow()

    val hasKey: Boolean get() = news.hasKey

    /** Japanese spellings already in the app, so known words can be marked. */
    val known: StateFlow<Set<String>> = repo.observeAll(VocabEntity.TYPE_WORD)
        .map { list -> list.flatMap { listOf(it.vocab.japanese, it.vocab.reading) }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        load()
    }

    fun setMode(mode: ReadMode) {
        if (mode == _state.value.mode) return
        if (mode != ReadMode.ORIGINAL) prefs.newsMode = mode.name
        _state.update { it.copy(mode = mode, openMeanings = emptySet(), selected = null) }
        load()
    }

    fun retry() = load()

    private fun load() {
        val item = _state.value.item ?: run {
            _state.update { it.copy(isLoading = false, error = "This story is no longer loaded. Go back. • ফিরে যান") }
            return
        }
        val mode = _state.value.mode
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = try {
                Result.success(news.read(item, mode))
            } catch (e: GeminiClient.AskException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(Exception("Something went wrong, try again • সমস্যা হয়েছে, আবার চেষ্টা করুন"))
            }
            if (_state.value.mode != mode) return@launch
            result.onSuccess { r -> _state.update { it.copy(isLoading = false, reading = r) } }
                .onFailure { e ->
                    // Show the real text offline so there's still something to read.
                    val fallback = runCatching { news.offline(item) }.getOrNull()
                    _state.update { it.copy(isLoading = false, reading = fallback, error = e.message) }
                }
        }
    }

    fun toggleFurigana() = _state.update { it.copy(furigana = !it.furigana) }

    fun toggleMeaning(index: Int) = _state.update {
        it.copy(openMeanings = if (index in it.openMeanings) it.openMeanings - index else it.openMeanings + index)
    }

    fun select(token: NewsToken, sentence: NewsSentence) {
        if (token.isPunctuation) return
        _state.update { it.copy(selected = SelectedWord(token, sentence)) }
        speaker.speak(token.kana.ifBlank { token.text })
    }

    fun clearSelection() = _state.update { it.copy(selected = null) }

    fun speak(text: String, slow: Boolean = false) = speaker.speak(text.replace(" ", ""), slow)

    fun speakAll() {
        val r = _state.value.reading ?: return
        speaker.speak((listOf(r.title) + r.sentences).joinToString("。") { it.text }.replace("。。", "。"))
    }

    fun stopSpeaking() = speaker.stop()

    /** Saves the tapped word (dictionary form) to My own words, with the news sentence as its example. */
    fun addWord(w: SelectedWord) = viewModelScope.launch {
        val t = w.token
        val japanese = t.base.ifBlank { t.text }.trim()
        val reading = t.baseKana.ifBlank { t.kana }.replace(" ", "").trim()
        val added = repo.addVocab(
            VocabEntity(
                japanese = japanese,
                reading = reading.ifBlank { japanese },
                romaji = com.nihongo.srs.data.KanaRomaji.convert(reading.ifBlank { japanese }),
                meaning = t.english.trim(),
                meaningBn = t.bangla.trim(),
                example = w.sentence.text,
                exampleReading = w.sentence.tokens.joinToString(" ") { it.kana.ifBlank { it.text } }
                    .replace(Regex(" ([、。！？])"), "$1"),
                exampleEn = w.sentence.english,
                exampleBn = w.sentence.bangla,
                level = VocabEntity.LEVEL_MINE,
                category = "News • খবর",
            )
        )
        if (added) players.award(Points.NEWS_WORD)
        _messages.emit(
            if (added) "$japanese added to My own words (+${Points.NEWS_WORD}) • যোগ হয়েছে"
            else "$japanese is already in the app • শব্দটি আগেই আছে"
        )
        clearSelection()
    }

    override fun onCleared() {
        speaker.stop()
    }

    companion object {
        const val ARG_LINK = "link"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                val handle: SavedStateHandle = createSavedStateHandle()
                ArticleViewModel(
                    app.news, app.repository, app.prefs, app.speaker, app.players,
                    handle.get<String>(ARG_LINK).orEmpty(),
                )
            }
        }
    }
}

data class RadioListState(val isLoading: Boolean = true, val error: String? = null)

/** NHK radio news episodes for listening practice. */
class RadioViewModel(private val news: NewsRepository, private val radio: RadioPlayer) : ViewModel() {

    val episodes: StateFlow<List<RadioEpisode>> = news.episodes
    val player = radio.state

    private val _state = MutableStateFlow(RadioListState(isLoading = news.episodes.value.isEmpty()))
    val state: StateFlow<RadioListState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isLoading = news.episodes.value.isEmpty(), error = null) }
        _state.value = try {
            news.refreshEpisodes()
            RadioListState(isLoading = false)
        } catch (e: NewsFeeds.FeedException) {
            RadioListState(isLoading = false, error = e.message)
        }
    }

    fun play(e: RadioEpisode) = radio.play(e)
    fun toggle() = radio.toggle()
    fun seekTo(ms: Int) = radio.seekTo(ms)
    fun skip(ms: Int) = radio.skip(ms)
    fun setSpeed(s: Float) = radio.setSpeed(s)
    fun stop() = radio.stop()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                RadioViewModel(app.news, app.radio)
            }
        }
    }
}
