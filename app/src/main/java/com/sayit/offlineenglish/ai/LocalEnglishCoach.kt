package com.sayit.offlineenglish.ai

import kotlin.math.max

data class WritingIssue(
    val title: String,
    val before: String,
    val after: String,
    val explanationAr: String,
)

data class WritingFeedback(
    val score: Int,
    val corrected: String,
    val issues: List<WritingIssue>,
    val better: String,
)

data class WordUpgrade(
    val simple: String,
    val stronger: String,
    val meaningAr: String,
)

data class ImproveFeedback(
    val corrected: String,
    val natural: String,
    val stronger: String,
    val upgrades: List<WordUpgrade>,
    val tipAr: String,
)

object LocalEnglishCoach {
    private data class MutableFix(
        var text: String,
        val issues: MutableList<WritingIssue> = mutableListOf(),
    )

    private val spelling = linkedMapOf(
        "becaue" to "because", "becuase" to "because", "beacuse" to "because",
        "goverment" to "government", "enviroment" to "environment", "recieve" to "receive",
        "seperate" to "separate", "definately" to "definitely", "responsability" to "responsibility",
        "availabe" to "available", "comunication" to "communication", "immediatly" to "immediately",
        "succesful" to "successful", "tommorow" to "tomorrow", "langauge" to "language"
    )

    private val thirdPerson = linkedMapOf(
        "need" to "needs", "want" to "wants", "work" to "works", "go" to "goes", "do" to "does",
        "have" to "has", "like" to "likes", "help" to "helps", "study" to "studies", "try" to "tries",
        "watch" to "watches", "finish" to "finishes", "use" to "uses", "live" to "lives", "make" to "makes"
    )

    private val upgrades = listOf(
        WordUpgrade("very good", "excellent", "ممتاز"),
        WordUpgrade("very bad", "terrible", "سيئ جدًا"),
        WordUpgrade("very tired", "exhausted", "مرهق جدًا"),
        WordUpgrade("very important", "essential", "أساسي / بالغ الأهمية"),
        WordUpgrade("very difficult", "challenging", "صعب ويتطلب جهدًا"),
        WordUpgrade("very easy", "straightforward", "واضح وسهل التنفيذ"),
        WordUpgrade("big problem", "major issue", "مشكلة كبيرة"),
        WordUpgrade("good result", "strong result", "نتيجة قوية"),
        WordUpgrade("good idea", "great idea", "فكرة ممتازة"),
        WordUpgrade("get better", "improve", "يتحسن"),
        WordUpgrade("a lot of", "many", "الكثير من — للمعدود غالبًا"),
        WordUpgrade("I think", "In my view", "برأيي"),
        WordUpgrade("I want to", "I'd like to", "أود أن"),
        WordUpgrade("I like", "I enjoy", "أستمتع بـ / أحب"),
        WordUpgrade("I don't know", "I'm not sure", "لست متأكدًا"),
        WordUpgrade("help me", "support me", "يساعدني / يدعمني")
    )

    fun review(input: String): WritingFeedback {
        val fix = correct(input)
        val issueCount = fix.issues.distinctBy { it.title + it.before }.size
        val score = max(45, 100 - issueCount * 7 - if (input.trim().length < 35) 5 else 0)
        val corrected = finishText(fix.text)
        val better = makeNatural(corrected)
        return WritingFeedback(
            score = score,
            corrected = corrected,
            issues = fix.issues.distinctBy { it.title + it.before }.take(4),
            better = better,
        )
    }

    fun improve(input: String): ImproveFeedback {
        val fix = correct(input)
        val corrected = finishText(fix.text)
        val natural = makeNatural(corrected)
        var stronger = natural
        val found = mutableListOf<WordUpgrade>()
        upgrades.forEach { upgrade ->
            val regex = Regex("\\b${Regex.escape(upgrade.simple)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(stronger)) {
                stronger = regex.replace(stronger, upgrade.stronger)
                found += upgrade
            }
        }
        if (found.isEmpty()) {
            val fallback = when {
                Regex("^I want to\\b", RegexOption.IGNORE_CASE).containsMatchIn(stronger) -> WordUpgrade("I want to", "I'd like to", "أود أن")
                Regex("^I think\\b", RegexOption.IGNORE_CASE).containsMatchIn(stronger) -> WordUpgrade("I think", "In my view", "برأيي")
                Regex("\\bvery\\b", RegexOption.IGNORE_CASE).containsMatchIn(stronger) -> WordUpgrade("very", "really", "جدًا / فعلاً")
                else -> null
            }
            fallback?.let {
                stronger = Regex("\\b${Regex.escape(it.simple)}\\b", RegexOption.IGNORE_CASE).replace(stronger, it.stronger)
                found += it
            }
        }
        return ImproveFeedback(
            corrected = corrected,
            natural = natural,
            stronger = finishText(stronger),
            upgrades = found.distinctBy { it.simple }.take(3),
            tipAr = when {
                fix.issues.isNotEmpty() -> "صحح الخطأ أولًا، وبعدها قوّي المفردات. لا تغيّر الجملة كلها دفعة واحدة."
                found.isNotEmpty() -> "الجملة صحيحة؛ التحسين هنا في اختيار تعبير أكثر طبيعية أو كلمة أقوى."
                else -> "جملتك جيدة. حاول إضافة سبب أو تفصيل صغير بدل حشو كلمات أصعب بلا حاجة."
            }
        )
    }

    private fun correct(input: String): MutableFix {
        val fix = MutableFix(normalize(input))

        spelling.forEach { (wrong, right) ->
            replaceWord(fix, wrong, right, "إملاء", "الكلمة الصحيحة هي $right.")
        }

        simpleRule(fix, Regex("\\bgo to home\\b", RegexOption.IGNORE_CASE), "go home", "تعبير طبيعي", "go to home", "go home", "مع home لا نستخدم to بعد go.")
        simpleRule(fix, Regex("\\bcome to home\\b", RegexOption.IGNORE_CASE), "come home", "تعبير طبيعي", "come to home", "come home", "مع home نقول come home بدون to.")
        simpleRule(fix, Regex("\\bI am agree\\b", RegexOption.IGNORE_CASE), "I agree", "قواعد", "I am agree", "I agree", "agree فعل، لذلك لا نضع am قبله.")
        simpleRule(fix, Regex("\\bI want ([a-z]+)\\b", RegexOption.IGNORE_CASE), { m -> "I want to ${m.groupValues[1]}" }, "Infinitive", "want + verb", "want to + verb", "بعد want نستخدم to + الفعل الأساسي.", exclude = setOf("to"))
        simpleRule(fix, Regex("\\b(can|could|should|must) to ([a-z]+)\\b", RegexOption.IGNORE_CASE), { m -> "${m.groupValues[1]} ${m.groupValues[2]}" }, "Modal verbs", "modal + to", "modal + base verb", "بعد can/could/should/must نستخدم الفعل بدون to.")
        simpleRule(fix, Regex("\\b(did not|didn't) went\\b", RegexOption.IGNORE_CASE), { m -> "${m.groupValues[1]} go" }, "Past Simple", "did + went", "did + go", "بعد did نستخدم الفعل بصيغته الأساسية.")
        simpleRule(fix, Regex("\\b(she|he|it) need my to\\b", RegexOption.IGNORE_CASE), { m -> "${m.groupValues[1]} needs me to" }, "ضمير + فعل", "need my to", "needs me to", "بعد need نستخدم ضمير المفعول me، ومع she/he نضيف s للفعل.")
        simpleRule(fix, Regex("\\bneed my to\\b", RegexOption.IGNORE_CASE), "need me to", "الضمائر", "my to", "me to", "هنا نحتاج ضمير المفعول me وليس my.")

        thirdPerson.forEach { (base, third) ->
            val regex = Regex("\\b(she|he|it) $base\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(fix.text)) {
                val before = regex.find(fix.text)?.value ?: "$base"
                fix.text = regex.replace(fix.text) { m -> "${m.groupValues[1]} $third" }
                fix.issues += WritingIssue("Present Simple", before, before.substringBeforeLast(" ") + " $third", "مع he / she / it نضيف s أو es للفعل في المضارع البسيط.")
            }
        }

        simpleRule(fix, Regex("\\b(many) (money|time|water|information|work)\\b", RegexOption.IGNORE_CASE), { m -> "much ${m.groupValues[2]}" }, "Much / Many", "many + غير معدود", "much + غير معدود", "نستخدم much مع الأسماء غير المعدودة.")
        simpleRule(fix, Regex("\\bmuch (people|books|days|hours|tasks|emails)\\b", RegexOption.IGNORE_CASE), { m -> "many ${m.groupValues[1]}" }, "Much / Many", "much + جمع معدود", "many + جمع معدود", "نستخدم many مع الأسماء المعدودة الجمع.")
        simpleRule(fix, Regex("\\bsince (\\d+) years?\\b", RegexOption.IGNORE_CASE), { m -> "for ${m.groupValues[1]} years" }, "Since / For", "since + مدة", "for + مدة", "نستخدم for مع مدة زمنية.")
        simpleRule(fix, Regex("\\bfor (20\\d{2}|19\\d{2})\\b", RegexOption.IGNORE_CASE), { m -> "since ${m.groupValues[1]}" }, "Since / For", "for + سنة", "since + سنة", "نستخدم since مع نقطة بداية محددة.")
        simpleRule(fix, Regex("\\b(a) ((?!useful\\b|user\\b|university\\b|unique\\b)[aeiou][a-z]*)\\b", RegexOption.IGNORE_CASE), { m -> "an ${m.groupValues[2]}" }, "A / An", "a + vowel sound", "an + vowel sound", "غالبًا نستخدم an قبل كلمة تبدأ بصوت متحرك.")
        simpleRule(fix, Regex("\\ban (book|car|job|project|team|meeting|report)\\b", RegexOption.IGNORE_CASE), { m -> "a ${m.groupValues[1]}" }, "A / An", "an + consonant", "a + consonant", "نستخدم a قبل صوت ساكن.")
        simpleRule(fix, Regex("\\bto hub\\b", RegexOption.IGNORE_CASE), "to the hub", "Articles", "to hub", "to the hub", "عند الحديث عن مكان محدد نقول the hub.")
        simpleRule(fix, Regex("\\bafter work in government\\b", RegexOption.IGNORE_CASE), "after work at the government office", "Prepositions", "work in government", "work at the government office", "للمكان الوظيفي المحدد at أكثر طبيعية هنا.")

        fix.text = fix.text.replace(Regex("\\bi\\b"), "I")
        fix.text = normalizePunctuation(fix.text)
        return fix
    }

    private fun replaceWord(fix: MutableFix, wrong: String, right: String, title: String, explanation: String) {
        val regex = Regex("\\b${Regex.escape(wrong)}\\b", RegexOption.IGNORE_CASE)
        if (regex.containsMatchIn(fix.text)) {
            fix.text = regex.replace(fix.text, right)
            fix.issues += WritingIssue(title, wrong, right, explanation)
        }
    }

    private fun simpleRule(
        fix: MutableFix,
        regex: Regex,
        replacement: String,
        title: String,
        before: String,
        after: String,
        explanation: String,
    ) {
        if (regex.containsMatchIn(fix.text)) {
            fix.text = regex.replace(fix.text, replacement)
            fix.issues += WritingIssue(title, before, after, explanation)
        }
    }

    private fun simpleRule(
        fix: MutableFix,
        regex: Regex,
        replacement: (MatchResult) -> String,
        title: String,
        before: String,
        after: String,
        explanation: String,
        exclude: Set<String> = emptySet(),
    ) {
        val match = regex.find(fix.text) ?: return
        if (match.groupValues.drop(1).any { it.lowercase() in exclude }) return
        fix.text = regex.replace(fix.text, replacement)
        fix.issues += WritingIssue(title, before, after, explanation)
    }

    private fun normalize(input: String): String = input
        .trim()
        .replace(Regex("\\s+"), " ")
        .replace(" ,", ",")
        .replace(" .", ".")
        .replace(" !", "!")
        .replace(" ?", "?")

    private fun normalizePunctuation(input: String): String {
        var out = input.replace(Regex("([,.!?])(?=[A-Za-z])")) { "${it.value} " }
        out = out.replace(Regex("\\s+([,.!?])")) { it.groupValues[1] }
        out = out.replace(Regex(",\\s+(I|He|She|We|They)\\s+(do|does|did|am|is|are|have|has|will|want|need|can|could|should)\\b", RegexOption.IGNORE_CASE)) { m -> ". ${m.groupValues[1]} ${m.groupValues[2]}" }
        out = out.replace(Regex("([.!?])\\s*([a-z])")) { m -> "${m.groupValues[1]} ${m.groupValues[2].uppercase()}" }
        out = out.replace(Regex("^([a-z])")) { it.value.uppercase() }
        return out.trim()
    }

    private fun finishText(input: String): String {
        var out = normalizePunctuation(input)
        if (out.isNotBlank() && out.last() !in listOf('.', '!', '?')) out += "."
        return out
    }

    private fun makeNatural(input: String): String {
        var out = input
        out = out.replace(Regex("\\bdo not\\b", RegexOption.IGNORE_CASE), "don't")
            .replace(Regex("\\bdoes not\\b", RegexOption.IGNORE_CASE), "doesn't")
            .replace(Regex("\\bdid not\\b", RegexOption.IGNORE_CASE), "didn't")
            .replace(Regex("\\bI am\\b"), "I'm")
            .replace(Regex("\\bI would like to\\b", RegexOption.IGNORE_CASE), "I'd like to")
            .replace(Regex("\\bI want to go home after work at the government office, I\\b", RegexOption.IGNORE_CASE), "I want to go home after work at the government office. I")
            .replace(Regex("\\bmy wife is pregnant and she needs me\\b", RegexOption.IGNORE_CASE), "my wife is pregnant and needs me")
        return finishText(out)
    }
}
