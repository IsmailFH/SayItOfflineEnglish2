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
import com.sayit.offlineenglish.util.similarityPercent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class EnglishCoachViewModel(app: Application) : AndroidViewModel(app) {
    private val progressStore = ProgressStore(app)
    private val ai = OfflineAiService(app)

    private val _progress = MutableStateFlow(progressStore.load())
    val progress: StateFlow<ProgressState> = _progress.asStateFlow()

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

    fun answerGrammar(correct: Boolean, skill: String) {
        val p = _progress.value
        val weakness = p.weaknessCounts.toMutableMap()
        if (!correct) weakness[skill] = (weakness[skill] ?: 0) + 1
        else if ((weakness[skill] ?: 0) > 0) weakness[skill] = (weakness[skill] ?: 0) - 1
        update(p.copy(
            xp = p.xp + if (correct) 10 else 2,
            grammarCorrect = p.grammarCorrect + if (correct) 1 else 0,
            grammarTotal = p.grammarTotal + 1,
            weaknessCounts = weakness.filterValues { it > 0 }
        ))
    }

    fun answerVocabulary(correct: Boolean, id: Int) {
        val p = _progress.value
        update(p.copy(
            xp = p.xp + if (correct) 8 else 2,
            vocabularyCorrect = p.vocabularyCorrect + if (correct) 1 else 0,
            vocabularyTotal = p.vocabularyTotal + 1,
            learnedWordIds = if (correct) p.learnedWordIds + id else p.learnedWordIds,
        ))
    }

    fun markWordLearned(id: Int) {
        val p = _progress.value
        update(p.copy(xp = p.xp + 3, learnedWordIds = p.learnedWordIds + id))
    }

    fun recordSpelling(correct: Boolean) {
        val p = _progress.value
        update(p.copy(
            xp = p.xp + if (correct) 7 else 2,
            spellingCorrect = p.spellingCorrect + if (correct) 1 else 0,
            spellingTotal = p.spellingTotal + 1,
        ))
    }

    fun recordDictation(score: Int) {
        val p = _progress.value
        update(p.copy(
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
            update(p.copy(xp = p.xp + 8, improveAttempts = p.improveAttempts + 1))
        }
    }

    fun reviewWriting(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _busy.value = true
            _writingFeedback.value = ai.reviewWriting(text)
            _busy.value = false
            val p = _progress.value
            update(p.copy(xp = p.xp + 12, writingAttempts = p.writingAttempts + 1))
        }
    }

    fun transcribeAndScore(audioFile: File, target: String? = null) {
        viewModelScope.launch {
            _busy.value = true
            val text = ai.transcribe(audioFile)
            _transcript.value = text
            if (text.isNotBlank()) {
                val p = _progress.value
                val scoreBonus = target?.let { similarityPercent(it, text) / 10 } ?: 5
                update(p.copy(xp = p.xp + scoreBonus.coerceAtLeast(2), speakingAttempts = p.speakingAttempts + 1))
            }
            _busy.value = false
        }
    }

    fun currentLevel(): String {
        val p = _progress.value
        val accuracy = if (p.grammarTotal == 0) 0.0 else p.grammarCorrect.toDouble() / p.grammarTotal
        return when {
            p.xp >= 600 && accuracy >= .8 -> "B1+"
            p.xp >= 250 && accuracy >= .7 -> "B1"
            p.xp >= 80 -> "A2+"
            else -> "A2"
        }
    }

    fun topWeaknesses(): List<Pair<String, Int>> = _progress.value.weaknessCounts.entries
        .sortedByDescending { it.value }
        .take(4)
        .map { it.key to it.value }

    private fun update(newState: ProgressState) {
        _progress.value = newState
        progressStore.save(newState)
    }

    override fun onCleared() {
        ai.release()
        super.onCleared()
    }
}
