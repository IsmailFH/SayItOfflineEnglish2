package com.sayit.offlineenglish.learning

object Curriculum {
    val skills: List<SkillDefinition> = listOf(
        SkillDefinition(
            id = "ps.form", unitId = "present_simple", level = "A1",
            titleEn = "Present Simple: form", titleAr = "تكوين المضارع البسيط",
            goalAr = "تكوّن جملة صحيحة عن عادة أو حقيقة.",
            explanationAr = "نستخدم المضارع البسيط للعادات والحقائق والأشياء المتكررة. مع I/you/we/they نستخدم الفعل كما هو، ومع he/she/it غالبًا نضيف s أو es.",
            formula = "I/You/We/They + verb  |  He/She/It + verb-s",
            examples = listOf("I work every day.", "She works at a hospital.", "They live in Gaza."),
            commonMistakeAr = "لا تنسَ s مع he / she / it: She works، وليس She work."
        ),
        SkillDefinition(
            id = "ps.third_person", unitId = "present_simple", level = "A1",
            titleEn = "Third person -s", titleAr = "قاعدة he / she / it",
            goalAr = "تستخدم s/es/ies بشكل صحيح مع الشخص الثالث.",
            explanationAr = "مع he/she/it نضيف غالبًا -s. الأفعال المنتهية بـ ch/sh/x/s/o تأخذ -es، والمنتهية بحرف ساكن + y تتحول y إلى ies.",
            formula = "work→works · watch→watches · study→studies · go→goes",
            examples = listOf("He studies English.", "She watches TV.", "It goes fast."),
            commonMistakeAr = "بعد does/doesn't نرجع للفعل الأساسي: Does he work? وليس Does he works?"
        ),
        SkillDefinition(
            id = "ps.neg_questions", unitId = "present_simple", level = "A1",
            titleEn = "Negatives & questions", titleAr = "النفي والأسئلة",
            goalAr = "تستخدم do/does و don't/doesn't بشكل صحيح.",
            explanationAr = "للنفي نستخدم don't أو doesn't ثم الفعل الأساسي. وللسؤال نبدأ بـ Do أو Does.",
            formula = "don't/doesn't + base verb  |  Do/Does + subject + base verb?",
            examples = listOf("I don't work on Friday.", "She doesn't drive.", "Does he speak English?"),
            commonMistakeAr = "لا تستخدم -s بعد does أو doesn't."
        ),
        SkillDefinition(
            id = "ps.usage", unitId = "present_simple", level = "A1",
            titleEn = "Present Simple: usage", titleAr = "متى نستخدم المضارع البسيط؟",
            goalAr = "تميّز العادة والحقيقة من الحدث الجاري الآن.",
            explanationAr = "كلمات مثل every day, usually, often, always غالبًا تشير إلى عادة، ولذلك تناسب المضارع البسيط.",
            formula = "habit / fact / routine → Present Simple",
            examples = listOf("I usually wake up at seven.", "Water boils at 100°C.", "He often walks to work."),
            commonMistakeAr = "وجود now أو right now غالبًا يعني أن المضارع المستمر أنسب."
        ),

        SkillDefinition(
            id = "pc.form", unitId = "present_continuous", level = "A1",
            titleEn = "Present Continuous: form", titleAr = "تكوين المضارع المستمر",
            goalAr = "تكوّن am/is/are + verb-ing بشكل صحيح.",
            explanationAr = "نستخدم المضارع المستمر عادةً لشيء يحدث الآن أو حول الوقت الحالي. لازم يكون عندنا فعل be مناسب ثم الفعل مع ing.",
            formula = "Subject + am/is/are + verb-ing",
            examples = listOf("I am studying now.", "She is cooking.", "They are working today."),
            commonMistakeAr = "لا تحذف am/is/are. نقول She is working، وليس She working."
        ),
        SkillDefinition(
            id = "pc.negative", unitId = "present_continuous", level = "A1",
            titleEn = "Present Continuous: negative", titleAr = "النفي في المضارع المستمر",
            goalAr = "تكوّن النفي بإضافة not بعد am/is/are.",
            explanationAr = "نضع not بعد am/is/are. في الكلام الطبيعي نستخدم isn't و aren't كثيرًا.",
            formula = "Subject + am/is/are + not + verb-ing",
            examples = listOf("I am not sleeping.", "He isn't working today.", "They aren't waiting."),
            commonMistakeAr = "لا تستخدم don't مع المضارع المستمر: He isn't working، وليس He doesn't working."
        ),
        SkillDefinition(
            id = "pc.questions", unitId = "present_continuous", level = "A1",
            titleEn = "Present Continuous: questions", titleAr = "الأسئلة في المضارع المستمر",
            goalAr = "تضع am/is/are قبل الفاعل عند تكوين السؤال.",
            explanationAr = "في السؤال ننقل am/is/are إلى بداية الجملة، أو بعد أداة السؤال مثل what/why/where.",
            formula = "Am/Is/Are + subject + verb-ing?",
            examples = listOf("Are you working?", "Is she studying?", "What are they doing?"),
            commonMistakeAr = "الترتيب الصحيح: What are you doing? وليس What you are doing?"
        ),
        SkillDefinition(
            id = "pc.ing_spelling", unitId = "present_continuous", level = "A1",
            titleEn = "-ing spelling", titleAr = "كتابة الفعل مع ing",
            goalAr = "تكتب الشكل -ing للأفعال الشائعة بدقة.",
            explanationAr = "غالبًا نضيف ing مباشرة. إذا انتهى الفعل بـ e نحذفها غالبًا: make→making. بعض الأفعال القصيرة تضاعف الحرف الأخير: run→running.",
            formula = "work→working · make→making · run→running · lie→lying",
            examples = listOf("write → writing", "sit → sitting", "study → studying"),
            commonMistakeAr = "لا تكتب makeing؛ الصحيح making."
        ),
        SkillDefinition(
            id = "pc.usage", unitId = "present_continuous", level = "A1",
            titleEn = "Present Continuous: usage", titleAr = "متى نستخدم المضارع المستمر؟",
            goalAr = "تختار المضارع المستمر عندما يكون الحدث جاريًا الآن أو مؤقتًا.",
            explanationAr = "كلمات مثل now, right now, at the moment, today قد تشير إلى حدث جارٍ أو وضع مؤقت.",
            formula = "happening now / temporary situation → Present Continuous",
            examples = listOf("I'm talking to you now.", "She's staying with her sister this week.", "We're working from home today."),
            commonMistakeAr = "العادات المتكررة عادةً تأخذ المضارع البسيط، لا المستمر."
        ),
        SkillDefinition(
            id = "pc.vs_simple", unitId = "present_continuous", level = "A2",
            titleEn = "Simple vs Continuous", titleAr = "المضارع البسيط أم المستمر؟",
            goalAr = "تميّز بين العادة وما يحدث الآن.",
            explanationAr = "المضارع البسيط للعادات والحقائق، والمستمر لما يحدث الآن أو لفترة مؤقتة. نفس الفعل قد يأتي بالزمنين حسب المعنى.",
            formula = "usually/every day → Simple  |  now/today → Continuous",
            examples = listOf("I work at TechNest. / I'm working from home today.", "She usually drives. / She's taking a taxi now."),
            commonMistakeAr = "لا تختار الزمن من شكل الفعل فقط؛ اقرأ معنى الجملة وكلمات الوقت."
        ),

        SkillDefinition(
            id = "art.a_an", unitId = "articles", level = "A1",
            titleEn = "a / an", titleAr = "a و an",
            goalAr = "تختار a أو an قبل اسم مفرد غير محدد.",
            explanationAr = "نستخدم a قبل صوت ساكن و an قبل صوت متحرك. القرار يعتمد على الصوت لا الحرف فقط.",
            formula = "a + consonant sound  |  an + vowel sound",
            examples = listOf("a job", "an engineer", "a university", "an hour"),
            commonMistakeAr = "نقول an engineer، وليس a engineer."
        ),
        SkillDefinition(
            id = "art.the", unitId = "articles", level = "A1",
            titleEn = "the", titleAr = "استخدام the",
            goalAr = "تستخدم the عندما يكون الشيء محددًا أو معروفًا للطرفين.",
            explanationAr = "نستخدم the عندما نتحدث عن شيء محدد أو ذُكر سابقًا أو معروف من السياق.",
            formula = "specific / known noun → the",
            examples = listOf("Close the door.", "I saw a dog. The dog was friendly."),
            commonMistakeAr = "لا تستخدم the مع كل اسم؛ اسأل هل الشيء محدد فعلًا؟"
        ),
        SkillDefinition(
            id = "art.zero", unitId = "articles", level = "A2",
            titleEn = "Zero article", titleAr = "متى لا نستخدم أداة؟",
            goalAr = "تترك الاسم بدون a/an/the عندما يكون الكلام عامًا في مواضع شائعة.",
            explanationAr = "كثير من الأسماء غير المعدودة والجمع العام لا تحتاج أداة عندما نتحدث بشكل عام.",
            formula = "general plural / uncountable → often no article",
            examples = listOf("I like coffee.", "Children need sleep.", "Technology changes quickly."),
            commonMistakeAr = "لا تقل I like the coffee إذا كنت تقصد القهوة عمومًا."
        ),

        SkillDefinition(
            id = "past.regular", unitId = "past_simple", level = "A2",
            titleEn = "Past Simple: regular verbs", titleAr = "الماضي البسيط — الأفعال المنتظمة",
            goalAr = "تستخدم -ed للأفعال المنتظمة في الماضي.",
            explanationAr = "لأحداث انتهت في الماضي نستخدم الماضي البسيط. الأفعال المنتظمة غالبًا تأخذ -ed.",
            formula = "regular verb + ed",
            examples = listOf("I worked yesterday.", "She called me last night.", "We finished at six."),
            commonMistakeAr = "لا تستخدم did مع الفعل الماضي في الجملة المثبتة."
        ),
        SkillDefinition(
            id = "past.irregular", unitId = "past_simple", level = "A2",
            titleEn = "Irregular verbs", titleAr = "الأفعال غير المنتظمة",
            goalAr = "تستدعي أشكال الماضي لأكثر الأفعال شيوعًا.",
            explanationAr = "بعض الأفعال لا تتبع -ed ويجب حفظ شكل الماضي منها، مثل go→went و see→saw و have→had.",
            formula = "go→went · see→saw · take→took · make→made",
            examples = listOf("I went home early.", "She saw her friend.", "They had lunch."),
            commonMistakeAr = "لا تقل goed؛ الصحيح went."
        ),
        SkillDefinition(
            id = "past.neg_questions", unitId = "past_simple", level = "A2",
            titleEn = "Past negatives & questions", titleAr = "نفي وأسئلة الماضي",
            goalAr = "تستخدم did/didn't مع الفعل الأساسي.",
            explanationAr = "بعد did أو didn't نستخدم الفعل في صورته الأساسية، لأن did يحمل معنى الماضي.",
            formula = "didn't + base verb  |  Did + subject + base verb?",
            examples = listOf("I didn't go.", "Did she call you?", "They didn't finish."),
            commonMistakeAr = "لا تقل Did you went? الصحيح Did you go?"
        ),
        SkillDefinition(
            id = "past.usage", unitId = "past_simple", level = "A2",
            titleEn = "Past Simple: usage", titleAr = "متى نستخدم الماضي البسيط؟",
            goalAr = "تتعرف على الأحداث المنتهية في وقت ماضٍ محدد.",
            explanationAr = "yesterday, last week, in 2024, two days ago إشارات شائعة لحدث انتهى في الماضي.",
            formula = "finished past event → Past Simple",
            examples = listOf("I met him yesterday.", "We moved here in 2024."),
            commonMistakeAr = "إذا كان الوقت الماضي منتهيًا ومحددًا، الماضي البسيط غالبًا هو الاختيار الطبيعي."
        ),

        SkillDefinition(
            id = "prep.time", unitId = "prepositions", level = "A2",
            titleEn = "Prepositions of time", titleAr = "حروف الجر مع الوقت",
            goalAr = "تستخدم at/on/in مع الوقت والتاريخ.",
            explanationAr = "at للوقت المحدد، on للأيام والتواريخ، in للشهور والسنوات والفترات الأوسع.",
            formula = "at 7:00 · on Monday · in August · in 2026",
            examples = listOf("at night", "on Friday", "in the morning"),
            commonMistakeAr = "نقول on Monday وليس in Monday."
        ),
        SkillDefinition(
            id = "prep.place", unitId = "prepositions", level = "A2",
            titleEn = "Prepositions of place", titleAr = "حروف الجر مع المكان",
            goalAr = "تستخدم at/in/on في أكثر تراكيب المكان شيوعًا.",
            explanationAr = "in داخل مكان أو مدينة/دولة، on على سطح، at لنقطة أو مكان باعتباره موقعًا.",
            formula = "in Gaza · on the table · at work · at home",
            examples = listOf("I'm at home.", "The phone is on the table.", "She lives in Gaza."),
            commonMistakeAr = "نقول at home بدون the."
        ),
        SkillDefinition(
            id = "prep.phrases", unitId = "prepositions", level = "A2",
            titleEn = "Common phrases", titleAr = "تراكيب شائعة بحروف الجر",
            goalAr = "تتعلم التراكيب كوحدة واحدة بدل ترجمتها حرفيًا.",
            explanationAr = "بعض الأفعال والتعبيرات تأتي مع حرف جر ثابت، والأفضل حفظ التركيب كاملًا.",
            formula = "listen to · wait for · interested in · good at",
            examples = listOf("Listen to me.", "I'm waiting for Ahmed.", "She's good at English."),
            commonMistakeAr = "تعلم العبارة كاملة؛ لا تختَر حرف الجر بترجمة عربية حرفية."
        ),
    )

    val units: List<UnitDefinition> = listOf(
        UnitDefinition(
            id = "present_simple", level = "A1", titleEn = "Present Simple", titleAr = "المضارع البسيط",
            descriptionAr = "العادات والحقائق والتكرار، مع النفي والأسئلة.", estimatedMinutes = 55,
            skillIds = listOf("ps.form", "ps.third_person", "ps.neg_questions", "ps.usage")
        ),
        UnitDefinition(
            id = "present_continuous", level = "A1–A2", titleEn = "Present Continuous", titleAr = "المضارع المستمر",
            descriptionAr = "من التكوين الأساسي إلى الاستخدام والفرق مع المضارع البسيط.", estimatedMinutes = 75,
            skillIds = listOf("pc.form", "pc.negative", "pc.questions", "pc.ing_spelling", "pc.usage", "pc.vs_simple")
        ),
        UnitDefinition(
            id = "articles", level = "A1–A2", titleEn = "Articles", titleAr = "a / an / the",
            descriptionAr = "اختيار أداة التعريف أو التنكير، ومتى نترك الاسم بدون أداة.", estimatedMinutes = 40,
            skillIds = listOf("art.a_an", "art.the", "art.zero")
        ),
        UnitDefinition(
            id = "past_simple", level = "A2", titleEn = "Past Simple", titleAr = "الماضي البسيط",
            descriptionAr = "الأفعال المنتظمة وغير المنتظمة، النفي والأسئلة والاستخدام.", estimatedMinutes = 60,
            skillIds = listOf("past.regular", "past.irregular", "past.neg_questions", "past.usage")
        ),
        UnitDefinition(
            id = "prepositions", level = "A2", titleEn = "Prepositions", titleAr = "حروف الجر الأساسية",
            descriptionAr = "الوقت والمكان والتراكيب الشائعة التي تسبب أخطاء متكررة.", estimatedMinutes = 45,
            skillIds = listOf("prep.time", "prep.place", "prep.phrases")
        ),
    )

    fun skill(id: String): SkillDefinition = skills.first { it.id == id }
    fun unit(id: String): UnitDefinition = units.first { it.id == id }
    fun skillsForUnit(unitId: String): List<SkillDefinition> = unit(unitId).skillIds.map(::skill)
    fun skillsForLevel(level: String): List<SkillDefinition> = skills.filter { it.level.startsWith(level) }

    fun errorLabel(tag: String): String = when (tag) {
        "missing_be" -> "نسيان am/is/are"
        "wrong_be" -> "اختيار am/is/are بشكل خاطئ"
        "question_order" -> "ترتيب السؤال"
        "ing_spelling" -> "كتابة -ing"
        "tense_choice" -> "اختيار الزمن من السياق"
        "third_person_s" -> "نسيان s مع he/she/it"
        "do_does" -> "do/does و don't/doesn't"
        "article_choice" -> "a/an/the"
        "past_form" -> "شكل الفعل في الماضي"
        "did_base" -> "استخدام الفعل الأساسي بعد did"
        "prep_time" -> "حرف الجر مع الوقت"
        "prep_place" -> "حرف الجر مع المكان"
        "fixed_phrase" -> "تركيب ثابت بحرف جر"
        "production" -> "استخدام المهارة في الكتابة"
        "speaking_recall" -> "تطبيق المهارة شفهيًا"
        else -> tag.replace('_', ' ')
    }
}
