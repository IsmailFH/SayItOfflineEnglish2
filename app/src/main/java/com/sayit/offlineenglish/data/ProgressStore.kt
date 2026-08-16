package com.sayit.offlineenglish.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("sayit_progress", Context.MODE_PRIVATE)

    fun load(): ProgressState {
        val learned = prefs.getString("learned", "[]") ?: "[]"
        val weaknesses = prefs.getString("weaknesses", "{}") ?: "{}"
        val learnedSet = buildSet {
            runCatching {
                val arr = JSONArray(learned)
                for (i in 0 until arr.length()) add(arr.getInt(i))
            }
        }
        val weaknessMap = buildMap {
            runCatching {
                val obj = JSONObject(weaknesses)
                obj.keys().forEach { key -> put(key, obj.optInt(key, 0)) }
            }
        }
        return ProgressState(
            xp = prefs.getInt("xp", 0),
            streak = prefs.getInt("streak", 1),
            grammarCorrect = prefs.getInt("grammar_correct", 0),
            grammarTotal = prefs.getInt("grammar_total", 0),
            vocabularyCorrect = prefs.getInt("vocabulary_correct", 0),
            vocabularyTotal = prefs.getInt("vocabulary_total", 0),
            spellingCorrect = prefs.getInt("spelling_correct", 0),
            spellingTotal = prefs.getInt("spelling_total", 0),
            dictationCorrect = prefs.getInt("dictation_correct", 0),
            dictationTotal = prefs.getInt("dictation_total", 0),
            speakingAttempts = prefs.getInt("speaking_attempts", 0),
            writingAttempts = prefs.getInt("writing_attempts", 0),
            improveAttempts = prefs.getInt("improve_attempts", 0),
            learnedWordIds = learnedSet,
            weaknessCounts = weaknessMap,
        )
    }

    fun save(state: ProgressState) {
        val learned = JSONArray().apply { state.learnedWordIds.sorted().forEach { put(it) } }
        val weaknesses = JSONObject().apply { state.weaknessCounts.forEach { (k, v) -> put(k, v) } }
        prefs.edit()
            .putInt("xp", state.xp)
            .putInt("streak", state.streak)
            .putInt("grammar_correct", state.grammarCorrect)
            .putInt("grammar_total", state.grammarTotal)
            .putInt("vocabulary_correct", state.vocabularyCorrect)
            .putInt("vocabulary_total", state.vocabularyTotal)
            .putInt("spelling_correct", state.spellingCorrect)
            .putInt("spelling_total", state.spellingTotal)
            .putInt("dictation_correct", state.dictationCorrect)
            .putInt("dictation_total", state.dictationTotal)
            .putInt("speaking_attempts", state.speakingAttempts)
            .putInt("writing_attempts", state.writingAttempts)
            .putInt("improve_attempts", state.improveAttempts)
            .putString("learned", learned.toString())
            .putString("weaknesses", weaknesses.toString())
            .apply()
    }
}
