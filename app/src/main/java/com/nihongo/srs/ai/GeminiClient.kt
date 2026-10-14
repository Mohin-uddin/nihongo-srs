package com.nihongo.srs.ai

import com.nihongo.srs.news.NewsReading
import com.nihongo.srs.news.NewsSentence
import com.nihongo.srs.news.NewsToken
import com.nihongo.srs.news.ReadMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** One word of the translated sentence. */
data class AskWord(
    val japanese: String,
    val kana: String,
    val romaji: String,
    val bangla: String,
    val english: String,
)

/** Gemini's answer for something the user typed in Banglish / Bangla / English. */
data class AskResult(
    val understood: String,
    val japanese: String,
    val kana: String,
    val romaji: String,
    val english: String,
    val bangla: String,
    val words: List<AskWord>,
    val tip: String,
)

/**
 * Tiny client for the Gemini API (no extra libraries: HttpURLConnection + org.json).
 * The API key is the user's own, typed into the app and kept on the phone.
 */
object GeminiClient {

    /** Tried in order; the next one is used when a model is busy or unavailable. */
    private val MODELS = listOf("gemini-2.5-flash", "gemini-flash-latest", "gemini-flash-lite-latest")

    private const val INSTRUCTIONS =
        "You help a Bangladeshi beginner learn to speak Japanese. The user writes in Banglish " +
            "(Bangla written in Latin letters), Bangla script, or English. It can be one word, a phrase or a sentence. " +
            "Translate it into natural, polite (desu/masu) beginner-level Japanese that a person would really say. " +
            "Reply only with JSON matching the schema. 'understood' is what you understood the user means, in Bangla script. " +
            "'kana' is the full reading in hiragana/katakana with a space between words. 'romaji' is Hepburn. " +
            "'bangla' is the meaning in Bangla script and 'english' in English. " +
            "'words' lists the important words of the Japanese (at most 6, skip particles) in dictionary form, " +
            "with reading and meaning. 'tip' is one short, useful tip in Bangla about grammar or usage, or empty."

    /** Thrown with a message already written for the user (English • Bangla). */
    class AskException(message: String) : Exception(message)

    suspend fun ask(apiKey: String, text: String): AskResult =
        generate(apiKey, INSTRUCTIONS, text, askSchema(), timeoutMs = 45_000, parse = ::parse)

    /**
     * Turns an NHK story into tappable sentences: every word with its reading and meaning.
     * [mode] EASY_* rewrites it in easy Japanese first; ORIGINAL keeps the real text.
     */
    suspend fun readNews(apiKey: String, title: String, summary: String, mode: ReadMode): NewsReading {
        val input = "MODE: ${mode.name}\nTITLE: $title\nTEXT: $summary"
        return generate(apiKey, newsInstructions(mode), input, newsSchema(), timeoutMs = 60_000, parse = ::parseNews)
    }

    /** Calls Gemini with a JSON schema, trying the next model when one is busy. */
    private suspend fun <T> generate(
        apiKey: String,
        instructions: String,
        text: String,
        schema: JSONObject,
        timeoutMs: Int,
        parse: (String) -> T,
    ): T = withContext(Dispatchers.IO) {
        var lastError: AskException? = null
        for (model in MODELS) {
            try {
                return@withContext parse(call(model, apiKey, requestBody(model, instructions, text, schema), timeoutMs))
            } catch (e: Retry) {
                lastError = AskException(e.message ?: "Busy")
            }
        }
        throw lastError ?: AskException("Gemini is busy, try again • Gemini ব্যস্ত, আবার চেষ্টা করুন")
    }

    /** Model busy / missing: try the next model. */
    private class Retry(message: String) : Exception(message)

    /** Returns the response body of a successful call. */
    private fun call(model: String, apiKey: String, request: JSONObject, timeoutMs: Int): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            conn.outputStream.use { it.write(request.toString().toByteArray()) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            when {
                code in 200..299 -> return body
                code == 400 && body.contains("API_KEY_INVALID") || code == 401 || code == 403 ->
                    throw AskException("The API key is not valid • API key সঠিক নয়")
                code == 400 || code == 404 || code == 429 || code >= 500 ->
                    throw Retry("Gemini is busy, try again in a minute • Gemini ব্যস্ত, একটু পরে চেষ্টা করুন")
                else -> throw AskException("Gemini error $code • সমস্যা হয়েছে, আবার চেষ্টা করুন")
            }
        } catch (e: IOException) {
            throw AskException("No internet connection • ইন্টারনেট সংযোগ নেই")
        } finally {
            conn.disconnect()
        }
    }

    private fun str() = JSONObject().put("type", "STRING")

    private fun obj(vararg fields: Pair<String, JSONObject>): JSONObject {
        val props = JSONObject()
        fields.forEach { (k, v) -> props.put(k, v) }
        return JSONObject().put("type", "OBJECT").put("properties", props)
            .put("required", JSONArray(fields.map { it.first }))
    }

    private fun array(items: JSONObject) = JSONObject().put("type", "ARRAY").put("items", items)

    private fun askSchema(): JSONObject {
        val word = obj("japanese" to str(), "kana" to str(), "romaji" to str(), "bangla" to str(), "english" to str())
        return obj(
            "understood" to str(), "japanese" to str(), "kana" to str(), "romaji" to str(),
            "english" to str(), "bangla" to str(), "words" to array(word), "tip" to str(),
        )
    }

    private fun newsSchema(): JSONObject {
        val token = obj(
            "text" to str(), "kana" to str(), "base" to str(), "baseKana" to str(),
            "english" to str(), "bangla" to str(),
        )
        val sentence = obj("tokens" to array(token), "english" to str(), "bangla" to str())
        return obj("title" to sentence, "sentences" to array(sentence))
    }

    private fun newsInstructions(mode: ReadMode): String {
        val rewrite = when (mode) {
            ReadMode.EASY_N5 -> "First rewrite the story in very easy Japanese for a JLPT N5 learner: " +
                "3 to 6 short sentences, polite desu/masu form, the most basic words and grammar. " +
                "Keep the key facts (who, what, where, numbers). Explain hard ideas with simple words. " +
                "Rewrite the title the same way."
            ReadMode.EASY_N4 -> "First rewrite the story in easy Japanese for a JLPT N4 learner: " +
                "4 to 7 short sentences, polite desu/masu form, common words and N4 grammar. " +
                "Keep the key facts (who, what, where, numbers). Rewrite the title the same way."
            ReadMode.ORIGINAL -> "Keep the original Japanese title and text exactly as given; do not change or " +
                "drop any character. Only split the text into sentences."
        }
        return "You prepare NHK news for a Bangladeshi learner of Japanese. The input has a TITLE and TEXT. " +
            rewrite + " Then split the title and every sentence into tokens: each word, each particle and " +
            "each punctuation mark is its own token, and joining the token texts must give the sentence exactly. " +
            "Keep a verb together with its ending (e.g. 発表しました is one token). For each token give: " +
            "'kana' = its reading in hiragana (katakana for katakana words); 'base' = dictionary form " +
            "(e.g. 発表する) and 'baseKana' = its reading; 'english' = short English meaning; " +
            "'bangla' = short meaning in Bangla script. For particles explain the job in a few words " +
            "(e.g. english 'object marker', bangla 'কর্ম চিহ্ন'). For punctuation and numbers-only tokens, " +
            "leave the meanings empty. Each sentence also has a natural full 'english' and 'bangla' translation. " +
            "Reply only with JSON matching the schema."
    }

    private fun requestBody(model: String, instructions: String, text: String, schema: JSONObject): JSONObject {
        val config = JSONObject()
            .put("temperature", 0.3)
            .put("responseMimeType", "application/json")
            .put("responseSchema", schema)
        // Gemini 2.5 thinks before answering by default; these tasks don't need it and run ~3x faster without.
        if (model.startsWith("gemini-2.5")) config.put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
        return JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instructions))))
            .put(
                "contents", JSONArray().put(
                    JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", text)))
                )
            )
            .put("generationConfig", config)
    }

    /** The model's JSON text inside a response body. */
    private fun answerJson(body: String): JSONObject {
        val root = JSONObject(body)
        val parts = root.optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw AskException("Gemini gave no answer, try rewording • উত্তর পাওয়া যায়নি, অন্যভাবে লিখুন")
        val text = (0 until parts.length()).joinToString("") { parts.getJSONObject(it).optString("text") }
        return JSONObject(text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```"))
    }

    /** Reads a news answer: either a full response body or the cached answer JSON. */
    fun parseNews(body: String): NewsReading {
        val o = JSONObject(body).let { if (it.has("candidates")) answerJson(body) else it }
        fun sentence(s: JSONObject?): NewsSentence {
            val arr = s?.optJSONArray("tokens") ?: JSONArray()
            val tokens = (0 until arr.length()).map { i ->
                val t = arr.getJSONObject(i)
                NewsToken(
                    text = t.optString("text"), kana = t.optString("kana"),
                    base = t.optString("base"), baseKana = t.optString("baseKana"),
                    english = t.optString("english"), bangla = t.optString("bangla"),
                )
            }.filter { it.text.isNotEmpty() }
            return NewsSentence(tokens, s?.optString("english").orEmpty(), s?.optString("bangla").orEmpty())
        }
        val list = o.optJSONArray("sentences") ?: JSONArray()
        return NewsReading(
            title = sentence(o.optJSONObject("title")),
            sentences = (0 until list.length()).map { sentence(list.getJSONObject(it)) }.filter { it.tokens.isNotEmpty() },
            fromAi = true,
        )
    }

    /** The news answer as compact JSON, for the on-phone cache. */
    fun newsToJson(r: NewsReading): String {
        fun sentence(s: NewsSentence) = JSONObject()
            .put("english", s.english).put("bangla", s.bangla)
            .put("tokens", JSONArray(s.tokens.map {
                JSONObject().put("text", it.text).put("kana", it.kana).put("base", it.base)
                    .put("baseKana", it.baseKana).put("english", it.english).put("bangla", it.bangla)
            }))
        return JSONObject().put("title", sentence(r.title))
            .put("sentences", JSONArray(r.sentences.map(::sentence))).toString()
    }

    /** Pulls the JSON answer out of Gemini's response. Pure, so it can be unit tested. */
    fun parse(body: String): AskResult {
        val o = answerJson(body)
        val words = o.optJSONArray("words") ?: JSONArray()
        return AskResult(
            understood = o.optString("understood"),
            japanese = o.optString("japanese"),
            kana = o.optString("kana"),
            romaji = o.optString("romaji"),
            english = o.optString("english"),
            bangla = o.optString("bangla"),
            words = (0 until words.length()).map { i ->
                val w = words.getJSONObject(i)
                AskWord(
                    w.optString("japanese"), w.optString("kana"), w.optString("romaji"),
                    w.optString("bangla"), w.optString("english"),
                )
            }.filter { it.japanese.isNotBlank() },
            tip = o.optString("tip"),
        )
    }
}
