package com.nihongo.srs.news

import com.nihongo.srs.data.VocabEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsTest {

    private val newsXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <rss xmlns:nhknews="http://news.web.nhk/rss/rss2.0/modules/nhknews/" version="2.0">
          <channel><title>NHKONEニュース</title>
            <item>
              <title>東京で雨</title>
              <link>https://news.web.nhk/newsweb/na/nd-1</link>
              <description>きょう東京で雨が降りました。</description>
              <pubDate>Wed, 14 Oct 2026 16:10:47 +0900</pubDate>
            </item>
          </channel>
        </rss>
    """

    private val radioXml = """
        <?xml version='1.0' encoding="UTF-8"?>
        <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" version="2.0"><channel>
          <item>
            <title>ニュース 正午のNHKニュース</title>
            <enclosure url="https://www.nhk.or.jp/s-media/news/podcast/audio/a_64k.mp3" length="1" type="audio/mp3" />
            <pubDate>Wed, 14 Oct 2026 12:15:00 +0900</pubDate>
            <itunes:duration>00:04:58</itunes:duration>
            <description>▽主なニュース</description>
          </item>
          <item><title>no audio</title></item>
        </channel></rss>
    """

    @Test
    fun parsesNhkNewsFeed() {
        val items = NewsFeeds.parseNews(newsXml)
        assertEquals(1, items.size)
        assertEquals("東京で雨", items[0].title)
        assertEquals("https://news.web.nhk/newsweb/na/nd-1", items[0].link)
        assertEquals(16, items[0].published?.hour)
    }

    @Test
    fun parsesRadioPodcastAndSkipsItemsWithoutAudio() {
        val eps = NewsFeeds.parseRadio(radioXml)
        assertEquals(1, eps.size)
        assertEquals("00:04:58", eps[0].duration)
        assertTrue(eps[0].audioUrl.endsWith(".mp3"))
    }

    private fun word(jp: String, reading: String, en: String) =
        VocabEntity(japanese = jp, reading = reading, romaji = "", meaning = en, meaningBn = "")

    @Test
    fun segmenterFindsKnownWordsLongestFirst() {
        val seg = Segmenter(listOf(word("東京", "とうきょう", "Tokyo"), word("雨", "あめ", "rain"), word("東", "ひがし", "east")))
        val s = seg.segment("東京で雨が降りました。")
        assertEquals("東京で雨が降りました。", s.text)
        assertEquals("東京", s.tokens[0].text)
        assertEquals("Tokyo", s.tokens[0].english)
        val rain = s.tokens.first { it.text == "雨" }
        assertEquals("rain", rain.english)
        assertTrue(s.tokens.none { it.text == "東" })
    }

    @Test
    fun segmenterSkipsSingleKanjiInsideBiggerWords() {
        val seg = Segmenter(listOf(word("月", "つき", "moon"), word("道", "みち", "road"), word("下", "した", "below")))
        val s = seg.segment("10月に鉄道ができました。道を歩きました。")
        assertTrue(s.tokens.none { it.text == "月" && it.hasMeaning })
        assertEquals(1, s.tokens.count { it.text == "道" && it.hasMeaning })
        assertTrue(s.tokens.none { it.text == "した" })
    }

    @Test
    fun segmenterSplitsSentences() {
        val seg = Segmenter(emptyList())
        val list = seg.sentences("一つ目です。二つ目です！\n三つ目")
        assertEquals(3, list.size)
        assertEquals("二つ目です！", list[1].text)
    }
}
