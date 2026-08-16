package com.sayit.offlineenglish.learning

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LearningStore(context: Context) {
    private val prefs = context.getSharedPreferences("sayit_learning_v1", Context.MODE_PRIVATE)

    fun load(): LearnerState {
        val raw = prefs.getString("learner_state", null) ?: return LearnerState()
        return runCatching {
            val root = JSONObject(raw)
            val progressObj = root.optJSONObject("skills") ?: JSONObject()
            val progress = buildMap {
                progressObj.keys().forEach { skillId ->
                    val obj = progressObj.optJSONObject(skillId) ?: return@forEach
                    val recent = buildList {
                        val arr = obj.optJSONArray("recent") ?: JSONArray()
                        for (i in 0 until arr.length()) add(arr.optInt(i, 0).coerceIn(0, 100))
                    }
                    val errors = buildMap {
                        val err = obj.optJSONObject("errors") ?: JSONObject()
                        err.keys().forEach { key -> put(key, err.optInt(key, 0)) }
                    }
                    put(
                        skillId,
                        SkillProgress(
                            attempts = obj.optInt("attempts", 0),
                            scoreSum = obj.optInt("scoreSum", 0),
                            recentScores = recent,
                            errorCounts = errors,
                            lastPracticedAt = obj.optLong("lastPracticedAt", 0L),
                            dueAt = obj.optLong("dueAt", 0L),
                        )
                    )
                }
            }
            LearnerState(
                activeUnitId = root.optString("activeUnitId", "present_continuous"),
                skillProgress = progress,
                totalStudySessions = root.optInt("totalStudySessions", 0),
                lastStudyAt = root.optLong("lastStudyAt", 0L),
            )
        }.getOrElse { LearnerState() }
    }

    fun save(state: LearnerState) {
        val skills = JSONObject()
        state.skillProgress.forEach { (skillId, p) ->
            val errors = JSONObject().apply { p.errorCounts.forEach { (key, value) -> put(key, value) } }
            val recent = JSONArray().apply { p.recentScores.forEach { put(it) } }
            skills.put(
                skillId,
                JSONObject()
                    .put("attempts", p.attempts)
                    .put("scoreSum", p.scoreSum)
                    .put("recent", recent)
                    .put("errors", errors)
                    .put("lastPracticedAt", p.lastPracticedAt)
                    .put("dueAt", p.dueAt)
            )
        }
        val root = JSONObject()
            .put("activeUnitId", state.activeUnitId)
            .put("skills", skills)
            .put("totalStudySessions", state.totalStudySessions)
            .put("lastStudyAt", state.lastStudyAt)
        prefs.edit().putString("learner_state", root.toString()).apply()
    }
}
