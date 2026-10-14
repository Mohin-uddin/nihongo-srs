package com.nihongo.srs.data

import android.content.Context

/** Small key-value settings stored on the phone. */
class AppPrefs(context: Context) {

    private val sp = context.getSharedPreferences("nihongo_prefs", Context.MODE_PRIVATE)

    /** Version of the bundled word lists already copied into the database. */
    var contentVersion: Int
        get() = sp.getInt("content_version", 0)
        set(v) = sp.edit().putInt("content_version", v).apply()

    var reminderEnabled: Boolean
        get() = sp.getBoolean("reminder_enabled", true)
        set(v) = sp.edit().putBoolean("reminder_enabled", v).apply()

    var reminderHour: Int
        get() = sp.getInt("reminder_hour", 20)
        set(v) = sp.edit().putInt("reminder_hour", v).apply()

    var reminderMinute: Int
        get() = sp.getInt("reminder_minute", 0)
        set(v) = sp.edit().putInt("reminder_minute", v).apply()

    /** We ask for notification permission once, on first launch. */
    var notificationPermissionAsked: Boolean
        get() = sp.getBoolean("notif_asked", false)
        set(v) = sp.edit().putBoolean("notif_asked", v).apply()

    /** The user's own Gemini API key for "Ask in Banglish". Stored only on this phone. */
    var geminiKey: String
        get() = sp.getString("gemini_key", "") ?: ""
        set(v) = sp.edit().putString("gemini_key", v.trim()).apply()

    /** Last easy-news level chosen (EASY_N5 / EASY_N4). */
    var newsMode: String
        get() = sp.getString("news_mode", "EASY_N5") ?: "EASY_N5"
        set(v) = sp.edit().putString("news_mode", v).apply()
}
