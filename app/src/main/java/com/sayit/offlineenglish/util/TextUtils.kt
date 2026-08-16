package com.sayit.offlineenglish.util

import java.util.Locale

fun normalizeEnglish(text: String): String = text
    .lowercase(Locale.ENGLISH)
    .replace(Regex("[^a-z0-9' ]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

fun similarityPercent(expected: String, actual: String): Int {
    val a = normalizeEnglish(expected).split(" ").filter { it.isNotBlank() }
    val b = normalizeEnglish(actual).split(" ").filter { it.isNotBlank() }
    if (a.isEmpty() && b.isEmpty()) return 100
    val d = levenshtein(a, b)
    val denom = maxOf(a.size, b.size, 1)
    return ((1.0 - d.toDouble() / denom) * 100).toInt().coerceIn(0, 100)
}

private fun levenshtein(a: List<String>, b: List<String>): Int {
    val dp = IntArray(b.size + 1) { it }
    for (i in 1..a.size) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..b.size) {
            val temp = dp[j]
            dp[j] = minOf(
                dp[j] + 1,
                dp[j - 1] + 1,
                prev + if (a[i - 1] == b[j - 1]) 0 else 1
            )
            prev = temp
        }
    }
    return dp[b.size]
}

fun spellingDiff(expected: String, actual: String): String {
    val e = normalizeEnglish(expected)
    val a = normalizeEnglish(actual)
    if (e == a) return "مطابق تمامًا ✓"
    val ew = e.split(" ")
    val aw = a.split(" ")
    val issues = mutableListOf<String>()
    val n = maxOf(ew.size, aw.size)
    repeat(n) { i ->
        val x = ew.getOrNull(i)
        val y = aw.getOrNull(i)
        when {
            x == null && y != null -> issues += "كلمة زائدة: $y"
            x != null && y == null -> issues += "كلمة ناقصة: $x"
            x != y -> issues += "${y ?: "—"} → ${x ?: "—"}"
        }
    }
    return issues.take(5).joinToString("\n")
}
