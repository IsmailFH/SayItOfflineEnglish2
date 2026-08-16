package com.sayit.offlineenglish.learning

import kotlin.math.roundToInt

data class SkillDefinition(
    val id: String,
    val unitId: String,
    val level: String,
    val titleEn: String,
    val titleAr: String,
    val goalAr: String,
    val explanationAr: String,
    val formula: String,
    val examples: List<String>,
    val commonMistakeAr: String,
)

data class UnitDefinition(
    val id: String,
    val level: String,
    val titleEn: String,
    val titleAr: String,
    val descriptionAr: String,
    val estimatedMinutes: Int,
    val skillIds: List<String>,
)

data class LearningExercise(
    val id: String,
    val skillId: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationAr: String,
    val errorTag: String,
    val difficulty: Int = 1,
)

data class SkillProgress(
    val attempts: Int = 0,
    val scoreSum: Int = 0,
    val recentScores: List<Int> = emptyList(),
    val errorCounts: Map<String, Int> = emptyMap(),
    val lastPracticedAt: Long = 0L,
    val dueAt: Long = 0L,
) {
    fun averageScore(): Int = if (attempts == 0) 0 else (scoreSum.toDouble() / attempts).roundToInt().coerceIn(0, 100)

    fun mastery(): Int {
        if (attempts == 0) return 0
        val recent = recentScores.takeLast(12)
        val weightedRecent = if (recent.isEmpty()) {
            averageScore().toDouble()
        } else {
            var weighted = 0.0
            var weights = 0.0
            recent.forEachIndexed { index, score ->
                val weight = (index + 1).toDouble()
                weighted += score * weight
                weights += weight
            }
            weighted / weights
        }
        return (weightedRecent * 0.72 + averageScore() * 0.28).roundToInt().coerceIn(0, 100)
    }

    fun confidence(): Int = (attempts * 8).coerceAtMost(100)

    fun isDue(now: Long = System.currentTimeMillis()): Boolean = attempts > 0 && (dueAt == 0L || dueAt <= now)

    fun statusAr(): String = when {
        attempts == 0 -> "غير مقيّمة"
        confidence() < 32 -> "بيانات قليلة"
        mastery() < 50 -> "ضعيفة"
        mastery() < 70 -> "قيد التطوير"
        mastery() < 88 -> "جيدة"
        confidence() >= 72 -> "متقنة"
        else -> "قوية"
    }
}

data class LearnerState(
    val activeUnitId: String = "present_continuous",
    val skillProgress: Map<String, SkillProgress> = emptyMap(),
    val totalStudySessions: Int = 0,
    val lastStudyAt: Long = 0L,
)

data class LevelEstimate(
    val label: String,
    val detailAr: String,
    val evidenceAttempts: Int,
)

data class DailyPlanItem(
    val skill: SkillDefinition,
    val reasonAr: String,
    val minutes: Int,
)
