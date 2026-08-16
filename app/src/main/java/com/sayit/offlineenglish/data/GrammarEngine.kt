package com.sayit.offlineenglish.data

import kotlin.random.Random

object GrammarEngine {
    private val names = listOf("Sara", "Omar", "Lina", "Adam", "Maya", "Yousef", "Nour", "Khaled")
    private data class PresentVerb(val base: String, val third: String, val prompt: (String) -> String)
    private data class PastVerb(val base: String, val past: String, val pp: String, val prompt: String)

    private val dailyVerbs = listOf(
        PresentVerb("go", "goes") { name -> "$name ___ to work every day." },
        PresentVerb("work", "works") { name -> "$name ___ from home every day." },
        PresentVerb("study", "studies") { name -> "$name ___ English every day." },
        PresentVerb("watch", "watches") { name -> "$name ___ the news every evening." },
        PresentVerb("finish", "finishes") { name -> "$name ___ work at five." },
        PresentVerb("try", "tries") { name -> "$name ___ to practice every day." }
    )
    private val irregularPast = listOf(
        PastVerb("go", "went", "gone", "Yesterday, I ___ to the office."),
        PastVerb("see", "saw", "seen", "Yesterday, I ___ my friend."),
        PastVerb("meet", "met", "met", "Yesterday, I ___ a new client."),
        PastVerb("drink", "drank", "drunk", "Yesterday, I ___ some coffee."),
        PastVerb("write", "wrote", "written", "Yesterday, I ___ an email."),
        PastVerb("take", "took", "taken", "Yesterday, I ___ the bus."),
        PastVerb("buy", "bought", "bought", "Yesterday, I ___ a new notebook."),
        PastVerb("make", "made", "made", "Yesterday, I ___ a decision."),
        PastVerb("come", "came", "come", "Yesterday, I ___ home early.")
    )
    private val articlePrompts = listOf(
        "I spoke to ___ engineer." to true,
        "She is ___ artist." to true,
        "They rented ___ office." to true,
        "That is ___ useful idea." to false,
        "I received ___ email." to true,
        "I have ___ appointment." to true,
        "Take ___ umbrella." to true,
        "It took ___ hour." to true,
        "He bought ___ laptop." to false,
        "We need ___ new plan." to false
    )
    private val adjectives = listOf(
        Triple("fast", "faster", "fastest"), Triple("small", "smaller", "smallest"),
        Triple("cheap", "cheaper", "cheapest"), Triple("easy", "easier", "easiest"),
        Triple("busy", "busier", "busiest"), Triple("safe", "safer", "safest")
    )
    private val skills = listOf(
        "Present Simple", "Past Simple", "Articles", "Prepositions", "Agreement", "Infinitives",
        "Conditionals", "Present Perfect", "Comparatives", "Passive Voice", "Past Continuous",
        "Modal Verbs", "Present Continuous", "Countable Nouns", "Question Forms"
    )

    fun nextQuestion(weaknesses: Map<String, Int> = emptyMap(), previousPrompt: String? = null): GrammarQuestion {
        val weighted = buildList {
            skills.forEach { skill ->
                add(skill)
                repeat((weaknesses[skill] ?: 0).coerceAtMost(4)) { add(skill) }
            }
        }
        repeat(8) {
            val skill = weighted.random()
            val q = make(skill)
            if (q.prompt != previousPrompt) return q
        }
        return make(skills.random())
    }

    private fun make(skill: String): GrammarQuestion = when (skill) {
        "Present Simple" -> presentSimple()
        "Past Simple" -> pastSimple()
        "Articles" -> articles()
        "Prepositions" -> prepositions()
        "Agreement" -> agreement()
        "Infinitives" -> infinitives()
        "Conditionals" -> conditionals()
        "Present Perfect" -> presentPerfect()
        "Comparatives" -> comparatives()
        "Passive Voice" -> passive()
        "Past Continuous" -> pastContinuous()
        "Modal Verbs" -> modals()
        "Present Continuous" -> presentContinuous()
        "Countable Nouns" -> countable()
        else -> questionForms()
    }

    private fun question(prompt: String, correct: String, wrong: List<String>, explanation: String, skill: String): GrammarQuestion {
        val options = (wrong + correct).distinct().shuffled().take(4)
        return GrammarQuestion(
            id = Random.nextInt(1, Int.MAX_VALUE),
            prompt = prompt,
            options = options,
            correctIndex = options.indexOf(correct),
            explanationAr = explanation,
            skill = skill,
        )
    }

    private fun presentSimple(): GrammarQuestion {
        val name = names.random()
        val verb = dailyVerbs.random()
        val base = verb.base
        val third = verb.third
        val ing = ingForm(base)
        return question(
            verb.prompt(name), third,
            listOf(base, ing, "is $ing"),
            "مع he / she أو اسم مفرد في المضارع البسيط نستخدم صيغة الفعل مع s/es.", "Present Simple"
        )
    }

    private fun pastSimple(): GrammarQuestion {
        val verb = irregularPast.random()
        val base = verb.base
        val past = verb.past
        val pp = verb.pp
        val ppDistractor = if (pp == past) "have $pp" else pp
        return question(
            verb.prompt, past,
            listOf(base, ppDistractor, "was ${ingForm(base)}"),
            "وجود yesterday يدل غالبًا على الماضي البسيط، لذلك نستخدم صيغة الماضي للفعل.", "Past Simple"
        )
    }

    private fun articles(): GrammarQuestion {
        val (prompt, vowelSound) = articlePrompts.random()
        val correct = if (vowelSound) "an" else "a"
        return question(
            prompt, correct,
            listOf(if (correct == "a") "an" else "a", "the", "— (no article)"),
            "نستخدم a/an مع اسم مفرد غير محدد؛ an قبل صوت متحرك.", "Articles"
        )
    }

    private fun prepositions(): GrammarQuestion {
        return when (Random.nextInt(3)) {
            0 -> question("The meeting starts ___ 9:30.", "at", listOf("in", "on", "for"), "نستخدم at مع وقت محدد.", "Prepositions")
            1 -> question("I have lived here ___ 2022.", "since", listOf("for", "at", "during"), "نستخدم since مع نقطة بداية زمنية.", "Prepositions")
            else -> question("I have studied English ___ three years.", "for", listOf("since", "from", "at"), "نستخدم for مع مدة زمنية.", "Prepositions")
        }
    }

    private fun agreement(): GrammarQuestion {
        return if (Random.nextBoolean()) {
            question("There ___ three messages for you.", "are", listOf("is", "was", "be"), "لأن الاسم جمع نستخدم are.", "Agreement")
        } else {
            question("My friend and I ___ ready.", "are", listOf("is", "am", "be"), "الفاعل هنا جمع، لذلك نستخدم are.", "Agreement")
        }
    }

    private fun infinitives(): GrammarQuestion {
        val verb = listOf("improve", "learn", "travel", "apply", "practice").random()
        return question("I want ___ more this month.", "to $verb", listOf(verb, ingForm(verb), "to ${ingForm(verb)}"), "بعد want نستخدم to + base verb.", "Infinitives")
    }

    private fun conditionals(): GrammarQuestion {
        val action = listOf("stay home", "call you", "take a taxi", "finish it tomorrow").random()
        return question("If it rains, we ___ $action.", "will", listOf("would", "did", "are"), "في first conditional نستخدم If + present ثم will + base verb.", "Conditionals")
    }

    private fun presentPerfect(): GrammarQuestion {
        val pp = listOf("finished", "sent", "started", "checked", "completed", "written").random()
        return question("She ___ $pp the task yet.", "hasn't", listOf("didn't", "isn't", "doesn't"), "مع yet في هذا السياق نستخدم present perfect: hasn't + past participle.", "Present Perfect")
    }

    private fun comparatives(): GrammarQuestion {
        val (base, comparative, superlative) = adjectives.random()
        return question("This option is ___ than the other one.", comparative, listOf(base, superlative, "more $base"), "مع صفة قصيرة نستخدم comparative + than.", "Comparatives")
    }

    private fun passive(): GrammarQuestion {
        val pairs = listOf("write" to "written", "send" to "sent", "make" to "made", "complete" to "completed")
        val (_, pp) = pairs.random()
        return question("The report ___ by the team yesterday.", "was $pp", listOf("is $pp", "wrote", pp), "المبني للمجهول في الماضي: was/were + past participle.", "Passive Voice")
    }

    private fun pastContinuous(): GrammarQuestion {
        val verb = listOf("work", "study", "drive", "talk", "write").random()
        val ing = ingForm(verb)
        return question("I ___ when you called me.", "was $ing", listOf(simplePast(verb), "am $ing", "have ${pastParticiple(verb)}"), "فعل كان مستمرًا عند حدوث فعل آخر في الماضي: was/were + verb-ing.", "Past Continuous")
    }

    private fun modals(): GrammarQuestion {
        val verb = listOf("help", "send", "check", "explain", "wait").random()
        return question("Could you ___ this for me?", verb, listOf("to $verb", "${verb}ing", "${verb}ed"), "بعد modal verb مثل could نستخدم الفعل بصيغته الأساسية.", "Modal Verbs")
    }

    private fun presentContinuous(): GrammarQuestion {
        val verb = listOf("work", "study", "wait", "write", "drive").random()
        val ing = ingForm(verb)
        return question("Look! He ___ now.", "is $ing", listOf(thirdPersonForm(verb), "was $ing", "has ${pastParticiple(verb)}"), "مع now وحدث مستمر نستخدم am/is/are + verb-ing.", "Present Continuous")
    }

    private fun countable(): GrammarQuestion {
        return if (Random.nextBoolean()) {
            question("How ___ emails did you receive?", "many", listOf("much", "some", "any"), "نستخدم many مع الأسماء المعدودة الجمع.", "Countable Nouns")
        } else {
            question("How ___ time do we have?", "much", listOf("many", "few", "several"), "نستخدم much مع الأسماء غير المعدودة.", "Countable Nouns")
        }
    }

    private fun ingForm(verb: String): String = when {
        verb.endsWith("ie") -> verb.dropLast(2) + "ying"
        verb.endsWith("e") && !verb.endsWith("ee") -> verb.dropLast(1) + "ing"
        else -> verb + "ing"
    }

    private fun thirdPersonForm(verb: String): String = when {
        verb.endsWith("y") && verb.length > 1 && verb[verb.length - 2] !in "aeiou" -> verb.dropLast(1) + "ies"
        verb.endsWith("s") || verb.endsWith("sh") || verb.endsWith("ch") || verb.endsWith("x") || verb.endsWith("o") -> verb + "es"
        else -> verb + "s"
    }

    private fun simplePast(verb: String): String = when (verb) {
        "drive" -> "drove"
        "write" -> "wrote"
        else -> if (verb.endsWith("e")) verb + "d" else verb + "ed"
    }

    private fun pastParticiple(verb: String): String = when (verb) {
        "drive" -> "driven"
        "write" -> "written"
        else -> simplePast(verb)
    }

    private fun questionForms(): GrammarQuestion {
        val base = listOf("work", "live", "study", "travel").random()
        return question("Where ___ you $base?", "do", listOf("does", "are", "didn't"), "في سؤال المضارع البسيط مع you نستخدم do + subject + base verb.", "Question Forms")
    }
}
