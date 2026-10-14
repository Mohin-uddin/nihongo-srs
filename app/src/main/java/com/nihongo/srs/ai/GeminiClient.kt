package com.nihongo.srs.ai

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
    private val MODELS = listOf("gemini-flash-latest", "gemini-2.5-flash", "gemini-flash-lite-latest")

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

    suspend fun ask(apiKey: String, text: String): AskResult = withContext(Dispatchers.IO) {
        var lastError: AskException? = null
        for (model in MODELS) {
            try {
                return@withContext call(model, apiKey, text)
            } catch (e: Retry) {
                lastError = AskException(e.message ?: "Busy")
            }
        }
        throw lastError ?: AskException("Gemini is busy, try again • Gemini ব্যস্ত, আবার চেষ্টা করুন")
    }

    /** Model busy / missing: try the next model. */
    private class Retry(message: String) : Exception(message)

    private fun call(model: String, apiKey: String, text: String): AskResult {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            conn.outputStream.use { it.write(requestBody(text).toString().toByteArray()) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            when {
                code in 200..299 -> return parse(body)
                code == 400 && body.contains("API_KEY_INVALID") || code == 401 || code == 403 ->
                    throw AskException("The API key is not valid • API key সঠিক নয়")
                code == 404 || code == 429 || code >= 500 ->
                    throw Retry("Gemini is busy, try again in a minute • Gemini ব্যস্ত, একটু পরে চেষ্টা করুন")
                else -> throw AskException("Gemini error $code • সমস্যা হয়েছে, আবার চেষ্টা করুন")
            }
        } catch (e: IOException) {
            throw AskException("No internet connection • ইন্টারনেট সংযোগ নেই")
        } finally {
            conn.disconnect()
        }
    }

    private fun requestBody(text: String): JSONObject {
        fun str() = JSONObject().put("type", "STRING")
        val word = JSONObject().put("type", "OBJECT").put(
            "properties", JSONObject()
                .put("japanese", str()).put("kana", str()).put("romaji", str())
                .put("bangla", str()).put("english", str())
        ).put("required", JSONArray(listOf("japanese", "kana", "romaji", "bangla", "english")))
        val schema = JSONObject().put("type", "OBJECT").put(
            "properties", JSONObject()
                .put("understood", str()).put("japanese", str()).put("kana", str()).put("romaji", str())
                .put("english", str()).put("bangla", str()).put("tip", str())
                .put("words", JSONObject().put("type", "ARRAY").put("items", word))
        ).put(
            "required",
            JSONArray(listOf("understood", "japanese", "kana", "romaji", "english", "bangla", "words", "tip"))
        )
        return JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", INSTRUCTIONS))))
            .put(
                "contents", JSONArray().put(
                    JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", text)))
                )
            )
            .put(
                "generationConfig", JSONObject()
                    .put("temperature", 0.3)
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", schema)
            )
    }

    /** Pulls the JSON answer out of Gemini's response. Pure, so it can be unit tested. */
    fun parse(body: String): AskResult {
        val root = JSONObject(body)
        val parts = root.optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw AskException("Gemini gave no answer, try rewording • উত্তর পাওয়া যায়নি, অন্যভাবে লিখুন")
        val text = (0 until parts.length()).joinToString("") { parts.getJSONObject(it).optString("text") }
        val o = JSONObject(text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```"))
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
