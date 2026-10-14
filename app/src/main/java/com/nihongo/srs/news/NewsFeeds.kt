package com.nihongo.srs.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

/** One NHK news story: headline and its short summary. */
data class NewsItem(
    val title: String,
    val summary: String,
    val link: String,
    val published: ZonedDateTime?,
)

/** One NHK radio news podcast episode. */
data class RadioEpisode(
    val title: String,
    val audioUrl: String,
    val published: ZonedDateTime?,
    /** e.g. "00:04:58". */
    val duration: String,
    /** List of main stories, when NHK gives one. */
    val description: String,
)

/**
 * Free public feeds from NHK (Japan's public broadcaster).
 * News: the NHK news RSS. Audio: the "NHKラジオニュース" podcast.
 */
object NewsFeeds {

    /** Main headlines first, then a few topics. Missing feeds are skipped. */
    private val NEWS_FEEDS = listOf(
        "https://news.web.nhk/n-data/conf/na/rss/cat0.xml",
        "https://news.web.nhk/n-data/conf/na/rss/cat1.xml",
        "https://news.web.nhk/n-data/conf/na/rss/cat3.xml",
        "https://news.web.nhk/n-data/conf/na/rss/cat7.xml",
    )
    private const val NEWS_FALLBACK = "https://www3.nhk.or.jp/rss/news/cat0.xml"
    const val RADIO_FEED = "https://www.nhk.or.jp/s-media/news/podcast/list/v1/all.xml"

    class FeedException(message: String) : Exception(message)

    suspend fun headlines(): List<NewsItem> = withContext(Dispatchers.IO) {
        val items = NEWS_FEEDS.flatMap { url -> runCatching { parseNews(download(url)) }.getOrDefault(emptyList()) }
            .ifEmpty { runCatching { parseNews(download(NEWS_FALLBACK)) }.getOrDefault(emptyList()) }
        if (items.isEmpty()) throw FeedException("Couldn't load the news. Check the internet. • নিউজ লোড হয়নি, ইন্টারনেট দেখুন")
        items.distinctBy { it.link }.sortedByDescending { it.published }
    }

    suspend fun radio(): List<RadioEpisode> = withContext(Dispatchers.IO) {
        try {
            parseRadio(download(RADIO_FEED))
        } catch (e: Exception) {
            throw FeedException("Couldn't load the radio news. Check the internet. • রেডিও নিউজ লোড হয়নি")
        }
    }

    private fun download(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) NihongoSRS")
        }
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun items(xml: String): List<Element> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.trim().byteInputStream(Charsets.UTF_8))
        val nodes = doc.getElementsByTagName("item")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Element.text(tag: String): String =
        getElementsByTagName(tag).item(0)?.textContent?.trim().orEmpty()

    private fun date(s: String): ZonedDateTime? =
        runCatching { ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME) }.getOrNull()

    /** Pure, so it can be unit tested. */
    fun parseNews(xml: String): List<NewsItem> = items(xml).map {
        NewsItem(
            title = it.text("title"),
            summary = it.text("description").replace(Regex("\\n\\s*\\n"), "\n"),
            link = it.text("link"),
            published = date(it.text("pubDate")),
        )
    }.filter { it.title.isNotBlank() }

    /** Pure, so it can be unit tested. */
    fun parseRadio(xml: String): List<RadioEpisode> = items(xml).mapNotNull {
        val url = (it.getElementsByTagName("enclosure").item(0) as? Element)?.getAttribute("url").orEmpty()
        if (url.isBlank()) null
        else RadioEpisode(
            title = it.text("title"),
            audioUrl = url,
            published = date(it.text("pubDate")),
            duration = it.text("itunes:duration").ifBlank { it.text("duration") },
            description = it.text("description"),
        )
    }
}
