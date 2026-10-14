package com.nihongo.srs.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** One person studying on this phone, with their points. */
data class Player(
    val id: Int,
    val name: String,
    val points: Int = 0,
    /** Points earned on [todayDate]. */
    val today: Int = 0,
    val todayDate: String = "",
    val gamesWon: Int = 0,
) {
    fun todayPoints(date: String = LocalDate.now().toString()) = if (todayDate == date) today else 0
}

/** How many points each activity gives. */
object Points {
    const val LEARN = 3
    const val REVIEW_CORRECT = 2
    const val REVIEW_TRIED = 1
    const val EXAM_CORRECT = 2
    const val KANA_CORRECT = 1
    const val NEWS_WORD = 2
    const val GAME_CORRECT = 10
    const val GAME_STREAK_BONUS = 5
    const val GAME_WIN = 20
}

/**
 * Players who share this phone (for example husband and wife) and their points.
 * Points go to the active player; the word list and review schedule are shared.
 * Kept in SharedPreferences as JSON: small, simple, offline.
 */
class PlayerStore(context: Context) {

    private val sp = context.getSharedPreferences("nihongo_players", Context.MODE_PRIVATE)

    private val _players = MutableStateFlow(load())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _activeId = MutableStateFlow(sp.getInt("active", _players.value.first().id))
    val activeId: StateFlow<Int> = _activeId.asStateFlow()

    val active: Player get() = _players.value.firstOrNull { it.id == _activeId.value } ?: _players.value.first()

    private fun load(): List<Player> {
        val raw = sp.getString("players", null) ?: return listOf(Player(1, "Me"))
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Player(
                    o.getInt("id"), o.getString("name"), o.optInt("points"), o.optInt("today"),
                    o.optString("todayDate"), o.optInt("gamesWon"),
                )
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: listOf(Player(1, "Me"))
    }

    @Synchronized
    private fun save(list: List<Player>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("name", it.name).put("points", it.points)
                    .put("today", it.today).put("todayDate", it.todayDate).put("gamesWon", it.gamesWon)
            )
        }
        sp.edit().putString("players", arr.toString()).apply()
        _players.value = list
    }

    @Synchronized
    private fun change(id: Int, f: (Player) -> Player) = save(_players.value.map { if (it.id == id) f(it) else it })

    /** Adds [n] points to [playerId] (the active player by default). */
    fun award(n: Int, playerId: Int = active.id) {
        if (n <= 0) return
        val date = LocalDate.now().toString()
        change(playerId) {
            it.copy(points = it.points + n, today = it.todayPoints(date) + n, todayDate = date)
        }
    }

    fun addWin(playerId: Int) = change(playerId) { it.copy(gamesWon = it.gamesWon + 1) }

    fun setActive(id: Int) {
        _activeId.value = id
        sp.edit().putInt("active", id).apply()
    }

    fun rename(id: Int, name: String) {
        val clean = name.trim().take(20)
        if (clean.isNotEmpty()) change(id) { it.copy(name = clean) }
    }

    /** Adds a player (at most [MAX_PLAYERS]) and returns it. */
    fun add(name: String): Player? {
        val list = _players.value
        if (list.size >= MAX_PLAYERS) return null
        val p = Player((list.maxOf { it.id }) + 1, name.trim().take(20).ifEmpty { "Player ${list.size + 1}" })
        save(list + p)
        return p
    }

    fun remove(id: Int) {
        val list = _players.value
        if (list.size <= 1) return
        save(list.filter { it.id != id })
        if (_activeId.value == id) setActive(_players.value.first().id)
    }

    companion object {
        const val MAX_PLAYERS = 4
    }
}
