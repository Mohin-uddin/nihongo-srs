package com.nihongo.srs.news

import android.content.Context
import com.nihongo.srs.ai.GeminiClient
import com.nihongo.srs.data.AppPrefs
import com.nihongo.srs.data.VocabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Today's NHK headlines and radio episodes, plus prepared (tappable) articles.
 * Prepared articles are saved on the phone, so opening one again is instant and free.
 */
class NewsRepository(
    context: Context,
    private val prefs: AppPrefs,
    private val vocab: VocabRepository,
) {
    private val cacheDir = File(context.cacheDir, "news").apply { mkdirs() }

    private val _headlines = MutableStateFlow<List<NewsItem>>(emptyList())
    val headlines: StateFlow<List<NewsItem>> = _headlines.asStateFlow()
    private var loadedAt = 0L

    private val _episodes = MutableStateFlow<List<RadioEpisode>>(emptyList())
    val episodes: StateFlow<List<RadioEpisode>> = _episodes.asStateFlow()

    val hasKey: Boolean get() = prefs.geminiKey.isNotBlank()

    /** Reloads when forced or when the list is older than 15 minutes. */
    suspend fun refreshHeadlines(force: Boolean = false) {
        if (!force && _headlines.value.isNotEmpty() && System.currentTimeMillis() - loadedAt < 15 * 60_000) return
        _headlines.value = NewsFeeds.headlines()
        loadedAt = System.currentTimeMillis()
    }

    suspend fun refreshEpisodes() {
        _episodes.value = NewsFeeds.radio()
    }

    fun item(link: String): NewsItem? = _headlines.value.firstOrNull { it.link == link }

    private fun cacheFile(item: NewsItem, mode: ReadMode) =
        File(cacheDir, "${item.link.hashCode().toUInt()}_${mode.name}.json")

    fun cached(item: NewsItem, mode: ReadMode): NewsReading? = runCatching {
        cacheFile(item, mode).takeIf { it.exists() }?.readText()?.let(GeminiClient::parseNews)
    }.getOrNull()

    /** With a Gemini key: every word explained. Without: only words already in the app. */
    suspend fun read(item: NewsItem, mode: ReadMode): NewsReading {
        cached(item, mode)?.let { return it }
        val key = prefs.geminiKey
        if (key.isBlank()) return offline(item)
        val reading = GeminiClient.readNews(key, item.title, item.summary, mode)
        withContext(Dispatchers.IO) {
            cleanOldCache()
            cacheFile(item, mode).writeText(GeminiClient.newsToJson(reading))
        }
        return reading
    }

    suspend fun offline(item: NewsItem): NewsReading {
        val seg = Segmenter(vocab.getAllWords())
        return NewsReading(seg.segment(item.title), seg.sentences(item.summary), fromAi = false)
    }

    /** Keeps the cache small: prepared articles older than 14 days are removed. */
    private fun cleanOldCache() {
        val limit = System.currentTimeMillis() - 14L * 24 * 3600 * 1000
        cacheDir.listFiles()?.filter { it.lastModified() < limit }?.forEach { it.delete() }
    }
}
