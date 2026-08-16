package com.sayit.offlineenglish.learning

import kotlin.random.Random

object ExerciseFactory {
    private data class Subject(val text: String, val be: String, val doAux: String)
    private data class Verb(val base: String, val third: String, val ing: String, val past: String, val complement: String)

    private val subjects = listOf(
        Subject("I", "am", "do"), Subject("You", "are", "do"), Subject("He", "is", "does"),
        Subject("She", "is", "does"), Subject("We", "are", "do"), Subject("They", "are", "do")
    )

    private val verbs = listOf(
        Verb("work", "works", "working", "worked", "at the office"),
        Verb("study", "studies", "studying", "studied", "English"),
        Verb("cook", "cooks", "cooking", "cooked", "dinner"),
        Verb("play", "plays", "playing", "played", "football"),
        Verb("watch", "watches", "watching", "watched", "TV"),
        Verb("write", "writes", "writing", "wrote", "an email"),
        Verb("read", "reads", "reading", "read", "a book"),
        Verb("make", "makes", "making", "made", "coffee"),
        Verb("run", "runs", "running", "ran", "in the park"),
        Verb("sit", "sits", "sitting", "sat", "near the window"),
        Verb("go", "goes", "going", "went", "to work"),
        Verb("take", "takes", "taking", "took", "the bus"),
        Verb("call", "calls", "calling", "called", "my friend"),
        Verb("finish", "finishes", "finishing", "finished", "the task"),
        Verb("help", "helps", "helping", "helped", "the team")
    )

    private val articleWords = listOf(
        "engineer" to "an", "apple" to "an", "office" to "an", "idea" to "an", "hour" to "an",
        "job" to "a", "computer" to "a", "university" to "a", "project" to "a", "teacher" to "a"
    )

    fun next(skillId: String, previousPrompt: String? = null): LearningExercise {
        repeat(8) {
            val q = generate(skillId)
            if (q.prompt != previousPrompt) return q
        }
        return generate(skillId)
    }

    private fun generate(skillId: String): LearningExercise = when (skillId) {
        "ps.form" -> presentSimpleForm()
        "ps.third_person" -> thirdPerson()
        "ps.neg_questions" -> presentSimpleNegativeQuestion()
        "ps.usage" -> presentSimpleUsage()
        "pc.form" -> presentContinuousForm()
        "pc.negative" -> presentContinuousNegative()
        "pc.questions" -> presentContinuousQuestion()
        "pc.ing_spelling" -> ingSpelling()
        "pc.usage" -> presentContinuousUsage()
        "pc.vs_simple" -> simpleVsContinuous()
        "art.a_an" -> articleAAn()
        "art.the" -> articleThe()
        "art.zero" -> articleZero()
        "past.regular" -> pastRegular()
        "past.irregular" -> pastIrregular()
        "past.neg_questions" -> pastNegativeQuestion()
        "past.usage" -> pastUsage()
        "prep.time" -> prepositionTime()
        "prep.place" -> prepositionPlace()
        "prep.phrases" -> prepositionPhrase()
        else -> presentContinuousForm()
    }

    private fun exercise(
        skill: String,
        prompt: String,
        correct: String,
        distractors: List<String>,
        explanation: String,
        errorTag: String,
        difficulty: Int = 1,
    ): LearningExercise {
        val options = (distractors + correct).distinct().shuffled().take(4)
        val safeOptions = if (correct in options) options else (options.take(3) + correct).shuffled()
        return LearningExercise(
            id = "$skill-${System.nanoTime()}-${Random.nextInt(10_000)}",
            skillId = skill,
            prompt = prompt,
            options = safeOptions,
            correctIndex = safeOptions.indexOf(correct),
            explanationAr = explanation,
            errorTag = errorTag,
            difficulty = difficulty,
        )
    }

    private fun presentSimpleForm(): LearningExercise {
        val s = subjects.random(); val v = verbs.random()
        val correct = if (s.doAux == "does") v.third else v.base
        return exercise(
            "ps.form", "${s.text} ___ ${v.complement} every day.", correct,
            listOf(v.base, v.third, v.ing, v.past),
            "every day تدل على عادة. مع ${s.text} نستخدم ${if (s.doAux == "does") "الفعل بصيغة الشخص الثالث" else "الفعل الأساسي"}.",
            if (s.doAux == "does") "third_person_s" else "tense_choice"
        )
    }

    private fun thirdPerson(): LearningExercise {
        val s = listOf("He", "She").random(); val v = verbs.random()
        return exercise(
            "ps.third_person", "$s ___ ${v.complement} every morning.", v.third,
            listOf(v.base, v.ing, v.past),
            "مع $s في المضارع البسيط نستخدم ${v.third}.", "third_person_s"
        )
    }

    private fun presentSimpleNegativeQuestion(): LearningExercise {
        val s = subjects.random(); val v = verbs.random()
        return if (Random.nextBoolean()) {
            val correct = if (s.doAux == "does") "doesn't" else "don't"
            exercise(
                "ps.neg_questions", "${s.text} ___ ${v.base} ${v.complement} on Fridays.", correct,
                listOf("don't", "doesn't", "isn't", "aren't"),
                "بعد $correct نستخدم الفعل الأساسي ${v.base}.", "do_does", 2
            )
        } else {
            val correct = if (s.doAux == "does") "Does" else "Do"
            exercise(
                "ps.neg_questions", "___ ${s.text.lowercase()} ${v.base} ${v.complement} every day?", correct,
                listOf("Do", "Does", "Is", "Are"),
                "$correct هو المساعد الصحيح للمضارع البسيط مع ${s.text}.", "do_does", 2
            )
        }
    }

    private fun presentSimpleUsage(): LearningExercise {
        val v = verbs.random(); val s = subjects.random()
        val simpleVerb = if (s.doAux == "does") v.third else v.base
        val correct = "${s.text} $simpleVerb ${v.complement} every day."
        return exercise(
            "ps.usage", "أي جملة تصف عادة متكررة؟", correct,
            listOf(
                "${s.text} ${s.be} ${v.ing} ${v.complement} right now.",
                "${s.text} ${v.past} ${v.complement} yesterday.",
                "${s.text} will ${v.base} ${v.complement} tomorrow."
            ),
            "every day علامة واضحة على عادة، لذلك نستخدم المضارع البسيط.", "tense_choice", 2
        )
    }

    private fun presentContinuousForm(): LearningExercise {
        val s = subjects.random(); val v = verbs.random()
        return exercise(
            "pc.form", "${s.text} ___ ${v.ing} ${v.complement} right now.", s.be,
            listOf("am", "is", "are", s.doAux),
            "right now تعني أن الحدث يحدث الآن. الصيغة: ${s.text} + ${s.be} + ${v.ing}.",
            "wrong_be"
        )
    }

    private fun presentContinuousNegative(): LearningExercise {
        val s = subjects.random(); val v = verbs.random()
        val correct = when (s.be) { "is" -> "isn't"; "are" -> "aren't"; else -> "am not" }
        return exercise(
            "pc.negative", "${s.text} ___ ${v.ing} ${v.complement} now.", correct,
            listOf("don't", "doesn't", "isn't", "aren't", "am not"),
            "في المضارع المستمر نضع not بعد am/is/are. الصحيح هنا: $correct.", "wrong_be", 2
        )
    }

    private fun presentContinuousQuestion(): LearningExercise {
        val s = subjects.random(); val v = verbs.random(); val correct = s.be.replaceFirstChar { it.uppercase() }
        return exercise(
            "pc.questions", "___ ${s.text.lowercase()} ${v.ing} ${v.complement} now?", correct,
            listOf("Am", "Is", "Are", "Do", "Does"),
            "في سؤال المضارع المستمر يأتي $correct قبل الفاعل.", "question_order", 2
        )
    }

    private fun ingSpelling(): LearningExercise {
        val v = verbs.filter { it.base in listOf("write", "make", "run", "sit", "study", "work") }.random()
        val distractors = when (v.base) {
            "write" -> listOf("writeing", "writting", "writen")
            "make" -> listOf("makeing", "makking", "maked")
            "run" -> listOf("runing", "runingg", "runned")
            "sit" -> listOf("siting", "sitted", "sittingg")
            else -> listOf("${v.base}ingg", "${v.base}ed", "${v.base}in")
        }
        return exercise(
            "pc.ing_spelling", "ما الشكل الصحيح مع -ing للفعل “${v.base}”؟", v.ing,
            distractors,
            "الشكل الصحيح هو ${v.base} → ${v.ing}.", "ing_spelling", 2
        )
    }

    private fun presentContinuousUsage(): LearningExercise {
        val s = subjects.random(); val v = verbs.random()
        val correct = "${s.text} ${s.be} ${v.ing} ${v.complement} now."
        val simpleVerb = if (s.doAux == "does") v.third else v.base
        return exercise(
            "pc.usage", "أي جملة تصف ما يحدث الآن؟", correct,
            listOf(
                "${s.text} $simpleVerb ${v.complement} every day.",
                "${s.text} ${v.past} ${v.complement} yesterday.",
                "${s.text} will ${v.base} ${v.complement} next week."
            ),
            "كلمة now تشير إلى حدث جارٍ، لذلك نستخدم ${s.be} + ${v.ing}.", "tense_choice", 2
        )
    }

    private fun simpleVsContinuous(): LearningExercise {
        val s = subjects.random(); val v = verbs.random(); val simpleVerb = if (s.doAux == "does") v.third else v.base
        val correct = "Usually ${s.text.lowercase()} $simpleVerb ${v.complement}, but today ${s.text.lowercase()} ${s.be} ${v.ing}."
        return exercise(
            "pc.vs_simple", "اختر الجملة التي تميّز بين العادة واليوم الحالي بشكل صحيح:", correct,
            listOf(
                "Usually ${s.text.lowercase()} ${s.be} ${v.ing}, but today ${s.text.lowercase()} $simpleVerb.",
                "Usually ${s.text.lowercase()} ${v.past}, but today ${s.text.lowercase()} $simpleVerb.",
                "Usually ${s.text.lowercase()} $simpleVerb, but today ${s.text.lowercase()} ${v.past}."
            ),
            "Usually → مضارع بسيط، و today هنا وضع مؤقت → مضارع مستمر.", "tense_choice", 3
        )
    }

    private fun articleAAn(): LearningExercise {
        val (word, correct) = articleWords.random()
        return exercise(
            "art.a_an", "I am ___ $word.", correct,
            listOf("a", "an", "the", "—"),
            "نختار $correct قبل $word حسب الصوت في بداية الكلمة.", "article_choice"
        )
    }

    private fun articleThe(): LearningExercise {
        val cases = listOf(
            Triple("Please close ___ door. We both know which door.", "the", "الشيء محدد ومعروف للطرفين."),
            Triple("I saw a dog. ___ dog was very friendly.", "The", "ذُكر dog سابقًا فأصبح محددًا."),
            Triple("Can you turn off ___ light in this room?", "the", "الضوء محدد بالسياق: الموجود في هذه الغرفة.")
        )
        val (prompt, correct, expl) = cases.random()
        return exercise("art.the", prompt, correct, listOf("a", "an", "the", "—"), expl, "article_choice", 2)
    }

    private fun articleZero(): LearningExercise {
        val cases = listOf(
            Triple("I like ___ coffee in general.", "—", "coffee هنا غير معدود والكلام عام."),
            Triple("___ children need enough sleep.", "—", "children هنا جمع عام وليس مجموعة محددة."),
            Triple("___ technology changes quickly.", "—", "technology هنا مفهوم عام.")
        )
        val (prompt, correct, expl) = cases.random()
        return exercise("art.zero", prompt, correct, listOf("a", "an", "the"), expl, "article_choice", 2)
    }

    private fun pastRegular(): LearningExercise {
        val v = verbs.filter { it.past.endsWith("ed") }.random()
        return exercise(
            "past.regular", "Yesterday I ___ ${v.complement}.", v.past,
            listOf(v.base, v.third, v.ing),
            "Yesterday يدل على وقت ماضٍ منتهٍ، والفعل المنتظم هنا يصبح ${v.past}.", "past_form"
        )
    }

    private fun pastIrregular(): LearningExercise {
        val v = verbs.filterNot { it.past.endsWith("ed") }.random()
        return exercise(
            "past.irregular", "Last week they ___ ${v.complement}.", v.past,
            listOf(v.base, v.third, v.ing, "${v.base}ed"),
            "${v.base} فعل غير منتظم؛ الماضي الصحيح هو ${v.past}.", "past_form", 2
        )
    }

    private fun pastNegativeQuestion(): LearningExercise {
        val v = verbs.random(); val s = subjects.random()
        return if (Random.nextBoolean()) {
            exercise(
                "past.neg_questions", "${s.text} ___ ${v.base} ${v.complement} yesterday.", "didn't",
                listOf("doesn't", "don't", "wasn't"),
                "بعد didn't نستخدم الفعل الأساسي ${v.base}.", "did_base", 2
            )
        } else {
            exercise(
                "past.neg_questions", "___ ${s.text.lowercase()} ${v.base} ${v.complement} yesterday?", "Did",
                listOf("Do", "Does", "Was"),
                "Did يحمل معنى الماضي، وبعده نستخدم ${v.base} وليس ${v.past}.", "did_base", 2
            )
        }
    }

    private fun pastUsage(): LearningExercise {
        val v = verbs.random()
        return exercise(
            "past.usage", "أي جملة تناسب كلمة yesterday؟", "I ${v.past} ${v.complement} yesterday.",
            listOf(
                "I ${v.base} ${v.complement} every day.",
                "I am ${v.ing} ${v.complement} now.",
                "I will ${v.base} ${v.complement} tomorrow."
            ),
            "yesterday وقت ماضٍ منتهٍ، لذلك نستخدم الماضي البسيط.", "tense_choice"
        )
    }

    private fun prepositionTime(): LearningExercise {
        val cases = listOf(
            Triple("The meeting starts ___ 9:00.", "at", "at مع وقت محدد."),
            Triple("We are closed ___ Friday.", "on", "on مع الأيام."),
            Triple("My birthday is ___ August.", "in", "in مع الشهور."),
            Triple("The project started ___ 2026.", "in", "in مع السنوات.")
        )
        val (prompt, correct, expl) = cases.random()
        return exercise("prep.time", prompt, correct, listOf("at", "on", "in"), expl, "prep_time")
    }

    private fun prepositionPlace(): LearningExercise {
        val cases = listOf(
            Triple("I'm ___ home now.", "at", "التعبير الطبيعي هو at home."),
            Triple("She lives ___ Gaza.", "in", "in مع المدن والمناطق."),
            Triple("Your phone is ___ the table.", "on", "on لشيء فوق سطح."),
            Triple("He's ___ work right now.", "at", "التعبير الطبيعي هو at work.")
        )
        val (prompt, correct, expl) = cases.random()
        return exercise("prep.place", prompt, correct, listOf("at", "on", "in"), expl, "prep_place")
    }

    private fun prepositionPhrase(): LearningExercise {
        val cases = listOf(
            Triple("Please listen ___ me.", "to", "listen to تركيب ثابت."),
            Triple("I'm waiting ___ my friend.", "for", "wait for تركيب ثابت."),
            Triple("She's very good ___ English.", "at", "good at تركيب شائع."),
            Triple("He is interested ___ AI.", "in", "interested in تركيب ثابت.")
        )
        val (prompt, correct, expl) = cases.random()
        return exercise("prep.phrases", prompt, correct, listOf("to", "for", "at", "in"), expl, "fixed_phrase", 2)
    }
}
