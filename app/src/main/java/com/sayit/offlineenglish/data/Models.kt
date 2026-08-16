package com.sayit.offlineenglish.data

data class GrammarQuestion(
    val id: Int,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationAr: String,
    val skill: String,
)

data class VocabularyItem(
    val id: Int,
    val word: String,
    val meaningAr: String,
    val example: String,
    val level: String,
    val alternatives: List<String> = emptyList(),
)

data class DictationItem(
    val id: Int,
    val text: String,
    val level: String,
)

data class ProgressState(
    val xp: Int = 0,
    val streak: Int = 1,
    val grammarCorrect: Int = 0,
    val grammarTotal: Int = 0,
    val vocabularyCorrect: Int = 0,
    val vocabularyTotal: Int = 0,
    val spellingCorrect: Int = 0,
    val spellingTotal: Int = 0,
    val dictationCorrect: Int = 0,
    val dictationTotal: Int = 0,
    val speakingAttempts: Int = 0,
    val writingAttempts: Int = 0,
    val improveAttempts: Int = 0,
    val learnedWordIds: Set<Int> = emptySet(),
    val weaknessCounts: Map<String, Int> = emptyMap(),
)
