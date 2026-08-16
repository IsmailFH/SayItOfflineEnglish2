package com.sayit.offlineenglish

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sayit.offlineenglish.ai.ImproveFeedback
import com.sayit.offlineenglish.ai.ModelStatus
import com.sayit.offlineenglish.ai.OfflineAiService
import com.sayit.offlineenglish.ai.WritingFeedback
import com.sayit.offlineenglish.data.ProgressState
import com.sayit.offlineenglish.data.ProgressStore
import com.sayit.offlineenglish.learning.Curriculum
import com.sayit.offlineenglish.learning.DailyPlanItem
import com.sayit.offlineenglish.learning.LearnerState
import com.sayit.offlineenglish.learning.LearningExercise
import com.sayit.offlineenglish.learning.LearningStore
import com.sayit.offlineenglish.learning.LevelEstimate
import com.sayit.offlineenglish.learning.SkillDefinition
import com.sayit.offlineenglish.learning.SkillProgress
import com.sayit.offlineenglish.util.similarityPercent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class EnglishCoachViewModel(app: Application) : AndroidViewModel(app) {
    private val progressStore = ProgressStore(app)
    private val learningStore = LearningStore(app)
    private val ai = OfflineAiService(app)

    private val _progress = MutableStateFlow(progressStore.load())
    val progress: StateFlow<ProgressState> = _progress.asStateFlow()

    private val _learner = MutableStateFlow(learningStore.load())
    val learner: StateFlow<LearnerState> = _learner.asStateFlow()

    private val _modelStatus = MutableStateFlow(ai.status())
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _modelDownloadProgress = MutableStateFlow(0)
    val modelDownloadProgress: StateFlow<Int> = _modelDownloadProgress.asStateFlow()

    private val _modelDownloadLabel = MutableStateFlow("")
    val modelDownloadLabel: StateFlow<String> = _modelDownloadLabel.asStateFlow()

    private val _writingFeedback = MutableStateFlow<WritingFeedback?>(null)
    val writingFeedback: StateFlow<WritingFeedback?> = _writingFeedback.asStateFlow()

    private val _improveFeedback = MutableStateFlow<ImproveFeedback?>(null)
    val improveFeedback: StateFlow<ImproveFeedback?> = _improveFeedback.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    fun refreshModels() { _modelStatus.value = ai.status() }

    fun downloadModels() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _modelDownloadProgress.value = 0
            _modelDownloadLabel.value = "بدء التنزيل"
            try {
                ai.downloadMissingModels { label, percent ->
                    _modelDownloadLabel.value = label
                    _modelDownloadProgress.value = percent
                }
                refreshModels()
            } catch (_: Throwable) {
                _modelDownloadLabel.value = "فشل التنزيل — تحقق من الإنترنت وأعد المحاولة"
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearWritingFeedback() { _writingFeedback.value = null }
    fun clearImproveFeedback() { _improveFeedback.value = null }
    fun clearTranscript() { _transcript.value = "" }

    fun setActiveUnit(unitId: String) {
        if (Curriculum.units.none { it.id == unitId }) return
        updateLearner(_learner.value.copy(activeUnitId = unitId))
    }

    fun skillProgress(skillId: String): SkillProgress = _learner.value.skillProgress[skillId] ?: SkillProgress()

    fun recordLearningAttempt(exercise: LearningExercise, correct: Boolean) {
        recordSkillScore(
            skillId = exercise.skillId,
            score = if (correct) 100 else 0,
            errorTag = if (correct) null else exercise.errorTag,
        )
        val p = _progress.value
        updateLegacy(p.copy(xp = p.xp + if (correct) 10 else 3))
    }

    fun recordSkillScore(skillId: String, score: Int, errorTag: String? = null) {
        if (Curriculum.skills.none { it.id == skillId }) return
        val now = System.currentTimeMillis()
        val state = _learner.value
        val old = state.skillProgress[skillId] ?: SkillProgress()
        val normalized = score.coerceIn(0, 100)
        val recent = (old.recentScores + normalized).takeLast(12)
        val errors = old.errorCounts.toMutableMap()
        if (errorTag != null && normalized < 70) {
            errors[errorTag] = (errors[errorTag] ?: 0) + 1
        }
        val interim = old.copy(
            attempts = old.attempts + 1,
            scoreSum = old.scoreSum + normalized,
            recentScores = recent,
            errorCounts = errors,
            lastPracticedAt = now,
        )
        val mastery = interim.mastery()
        val delay = when {
            normalized < 55 -> 4L * 60L * 60L * 1000L
            mastery < 60 -> 24L * 60L * 60L * 1000L
            mastery < 75 -> 3L * 24L * 60L * 60L * 1000L
            mastery < 88 -> 7L * 24L * 60L * 60L * 1000L
            else -> 14L * 24L * 60L * 60L * 1000L
        }
        val updated = interim.copy(dueAt = now + delay)
        updateLearner(
            state.copy(
                skillProgress = state.skillProgress + (skillId to updated),
                lastStudyAt = now,
            )
        )
    }

    fun finishStudySession() {
        val state = _learner.value
        updateLearner(state.copy(totalStudySessions = state.totalStudySessions + 1, lastStudyAt = System.currentTimeMillis()))
    }

    fun unitStats(unitId: String): Pair<Int, Int> {
        val progress = Curriculum.skillsForUnit(unitId).mapNotNull { skill ->
            _learner.value.skillProgress[skill.id]?.takeIf { it.attempts > 0 }
        }
        if (progress.isEmpty()) return 0 to 0
        return progress.map { it.mastery() }.average().toInt() to progress.map { it.confidence() }.average().toInt()
    }

    fun assessedSkills(unitId: String): Int = Curriculum.skillsForUnit(unitId).count {
        (_learner.value.skillProgress[it.id]?.attempts ?: 0) > 0
    }

    fun dailyPlan(): List<DailyPlanItem> {
        val state = _learner.value
        val now = System.currentTimeMillis()
        val picked = mutableListOf<DailyPlanItem>()
        val used = mutableSetOf<String>()

        val due = Curriculum.skills
            .filter { state.skillProgress[it.id]?.isDue(now) == true }
            .sortedBy { state.skillProgress[it.id]?.mastery() ?: 100 }
        due.take(2).forEach {
            picked += DailyPlanItem(it, "حان موعد المراجعة", 4)
            used += it.id
        }

        val weak = Curriculum.skills
            .filter { skill ->
                val p = state.skillProgress[skill.id] ?: return@filter false
                p.attempts >= 3 && p.mastery() < 70 && skill.id !in used
            }
            .sortedBy { state.skillProgress[it.id]?.mastery() ?: 100 }
        weak.take(2).forEach {
            if (picked.size < 3) {
                picked += DailyPlanItem(it, "نقطة ضعف ظهرت من إجاباتك", 5)
                used += it.id
            }
        }

        val activeSkills = Curriculum.skillsForUnit(state.activeUnitId)
        activeSkills.firstOrNull { skill ->
            skill.id !in used && (state.skillProgress[skill.id]?.attempts ?: 0) == 0
        }?.let {
            if (picked.size < 3) {
                picked += DailyPlanItem(it, "الخطوة التالية في وحدتك", 6)
                used += it.id
            }
        }

        if (picked.size < 3) {
            activeSkills
                .filter { it.id !in used }
                .sortedBy { state.skillProgress[it.id]?.mastery() ?: -1 }
                .take(3 - picked.size)
                .forEach { picked += DailyPlanItem(it, "تثبيت مهارة الوحدة الحالية", 4) }
        }
        return picked.take(3)
    }

    fun topWeakSkills(limit: Int = 5): List<SkillDefinition> = Curriculum.skills
        .filter { skill ->
            val p = _learner.value.skillProgress[skill.id] ?: return@filter false
            p.attempts >= 3 && p.confidence() >= 24 && p.mastery() < 75
        }
        .sortedBy { _learner.value.skillProgress[it.id]?.mastery() ?: 100 }
        .take(limit)

    fun topErrors(limit: Int = 5): List<Pair<String, Int>> {
        val totals = mutableMapOf<String, Int>()
        _learner.value.skillProgress.values.forEach { p ->
            p.errorCounts.forEach { (tag, count) -> totals[tag] = (totals[tag] ?: 0) + count }
        }
        return totals.entries.sortedByDescending { it.value }.take(limit).map { it.key to it.value }
    }

    fun estimateLevel(): LevelEstimate {
        val state = _learner.value
        val totalAttempts = state.skillProgress.values.sumOf { it.attempts }
        if (totalAttempts < 18) {
            return LevelEstimate(
                label = "قيد التقييم",
                detailAr = "أحتاج أدلة أكثر من تمارين متنوعة قبل أن أحدد مستوى حقيقيًا. أكمل الخطة اليومية.",
                evidenceAttempts = totalAttempts,
            )
        }

        fun masteredRatio(level: String): Double {
            val skills = Curriculum.skillsForLevel(level)
            if (skills.isEmpty()) return 0.0
            val mastered = skills.count { skill ->
                val p = state.skillProgress[skill.id] ?: return@count false
                p.attempts >= 4 && p.confidence() >= 32 && p.mastery() >= 70
            }
            return mastered.toDouble() / skills.size
        }

        val a1 = masteredRatio("A1")
        val a2 = masteredRatio("A2")
        return when {
            a1 >= .70 && a2 >= .50 -> LevelEstimate("A2", "أداؤك يثبت أساس A1 جيدًا وبدأت تتقن أغلب مهارات A2 التي قسناها.", totalAttempts)
            a1 >= .70 -> LevelEstimate("A1+", "أساسيات A1 مستقرة في المهارات المقاسة، لكن ما زلنا نبني أدلة على A2.", totalAttempts)
            a1 >= .45 -> LevelEstimate("A1", "عندك أساس واضح، لكن توجد مهارات A1 تحتاج تثبيت قبل الحكم على A2.", totalAttempts)
            else -> LevelEstimate("أساسيات", "البيانات الحالية تشير أن الأولوية لتثبيت الأساسيات قبل الانتقال لمستوى أعلى.", totalAttempts)
        }
    }

    fun answerGrammar(correct: Boolean, skill: String) {
        val p = _progress.value
        val weakness = p.weaknessCounts.toMutableMap()
        if (!correct) weakness[skill] = (weakness[skill] ?: 0) + 1
        else if ((weakness[skill] ?: 0) > 0) weakness[skill] = (weakness[skill] ?: 0) - 1
        updateLegacy(p.copy(
            xp = p.xp + if (correct) 10 else 2,
            grammarCorrect = p.grammarCorrect + if (correct) 1 else 0,
            grammarTotal = p.grammarTotal + 1,
            weaknessCounts = weakness.filterValues { it > 0 }
        ))
    }

    fun answerVocabulary(correct: Boolean, id: Int) {
        val p = _progress.value
        updateLegacy(p.copy(
            xp = p.xp + if (correct) 8 else 2,
            vocabularyCorrect = p.vocabularyCorrect + if (correct) 1 else 0,
            vocabularyTotal = p.vocabularyTotal + 1,
            learnedWordIds = if (correct) p.learnedWordIds + id else p.learnedWordIds,
        ))
    }

    fun markWordLearned(id: Int) {
        val p = _progress.value
        updateLegacy(p.copy(xp = p.xp + 3, learnedWordIds = p.learnedWordIds + id))
    }

    fun recordSpelling(correct: Boolean) {
        val p = _progress.value
        updateLegacy(p.copy(
            xp = p.xp + if (correct) 7 else 2,
            spellingCorrect = p.spellingCorrect + if (correct) 1 else 0,
            spellingTotal = p.spellingTotal + 1,
        ))
    }

    fun recordDictation(score: Int) {
        val p = _progress.value
        updateLegacy(p.copy(
            xp = p.xp + (score / 10).coerceAtLeast(1),
            dictationCorrect = p.dictationCorrect + if (score >= 85) 1 else 0,
            dictationTotal = p.dictationTotal + 1,
        ))
    }

    fun improveSentence(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _busy.value = true
            _improveFeedback.value = ai.improveSentence(text)
            _busy.value = false
            val p = _progress.value
            updateLegacy(p.copy(xp = p.xp + 8, improveAttempts = p.improveAttempts + 1))
        }
    }

    fun reviewWriting(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _busy.value = true
            _writingFeedback.value = ai.reviewWriting(text)
            _busy.value = false
            val p = _progress.value
            updateLegacy(p.copy(xp = p.xp + 12, writingAttempts = p.writingAttempts + 1))
        }
    }

    fun transcribeAndScore(audioFile: File, target: String? = null, skillId: String? = null) {
        viewModelScope.launch {
            _busy.value = true
            val text = ai.transcribe(audioFile)
            _transcript.value = text
            if (text.isNotBlank()) {
                val p = _progress.value
                val score = target?.let { similarityPercent(it, text) }
                val scoreBonus = score?.div(10) ?: 5
                updateLegacy(p.copy(xp = p.xp + scoreBonus.coerceAtLeast(2), speakingAttempts = p.speakingAttempts + 1))
                if (skillId != null && score != null) {
                    recordSkillScore(skillId, score, if (score < 70) "speaking_recall" else null)
                }
            }
            _busy.value = false
        }
    }

    fun currentLevel(): String = estimateLevel().label

    fun topWeaknesses(): List<Pair<String, Int>> = _progress.value.weaknessCounts.entries
        .sortedByDescending { it.value }
        .take(4)
        .map { it.key to it.value }

    private fun updateLegacy(newState: ProgressState) {
        _progress.value = newState
        progressStore.save(newState)
    }

    private fun updateLearner(newState: LearnerState) {
        _learner.value = newState
        learningStore.save(newState)
    }

    override fun onCleared() {
        ai.release()
        super.onCleared()
    }
}
