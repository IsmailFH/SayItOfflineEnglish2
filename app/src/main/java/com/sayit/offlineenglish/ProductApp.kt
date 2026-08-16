package com.sayit.offlineenglish

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayit.offlineenglish.ai.ImproveFeedback
import com.sayit.offlineenglish.ai.WritingFeedback
import com.sayit.offlineenglish.audio.WavRecorder
import com.sayit.offlineenglish.data.SeedData
import com.sayit.offlineenglish.data.VocabularyItem
import com.sayit.offlineenglish.learning.Curriculum
import com.sayit.offlineenglish.learning.ExerciseFactory
import com.sayit.offlineenglish.learning.LearningExercise
import com.sayit.offlineenglish.learning.SkillDefinition
import com.sayit.offlineenglish.learning.SkillProgress
import com.sayit.offlineenglish.util.similarityPercent
import com.sayit.offlineenglish.util.spellingDiff
import java.io.File
import java.util.Locale
import kotlin.random.Random

private val Ink = Color(0xFF111827)
private val Paper = Color(0xFFF5F7FB)
private val SurfaceSoft = Color(0xFFF0F2F7)
private val Indigo = Color(0xFF4F5BD5)
private val Emerald = Color(0xFF0F9D7A)
private val Amber = Color(0xFFF4A340)
private val Rose = Color(0xFFE85D75)
private val Muted = Color(0xFF6B7280)

enum class ProductTab { TODAY, ACADEMY, LAB, COACH, PASSPORT }
enum class DetailKind { UNIT, LESSON, DRILL, VOCAB, DICTATION, SPEAKING }
data class ProductDetail(val kind: DetailKind, val id: String = "", val mode: String = "")
enum class CoachMode { FIX, UPGRADE, WRITE }

@Composable
fun SayItProductApp(vm: EnglishCoachViewModel) {
    var tab by remember { mutableStateOf(ProductTab.TODAY) }
    var detail by remember { mutableStateOf<ProductDetail?>(null) }
    var coachMode by remember { mutableStateOf(CoachMode.FIX) }

    val scheme = lightColorScheme(
        primary = Ink,
        onPrimary = Color.White,
        secondary = Indigo,
        tertiary = Emerald,
        background = Paper,
        surface = Color.White,
        surfaceVariant = SurfaceSoft,
        outline = Color(0xFFDDE1EA),
    )

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Paper,
                bottomBar = {
                    if (detail == null) {
                        NavigationBar(containerColor = Color.White, tonalElevation = 3.dp) {
                            val tabs = listOf(
                                ProductTab.TODAY to ("اليوم" to "◎"),
                                ProductTab.ACADEMY to ("تعلّم" to "◫"),
                                ProductTab.LAB to ("المختبر" to "✦"),
                                ProductTab.COACH to ("المدرب" to "✎"),
                                ProductTab.PASSPORT to ("تقدمي" to "◉"),
                            )
                            tabs.forEach { (item, meta) ->
                                NavigationBarItem(
                                    selected = tab == item,
                                    onClick = { tab = item },
                                    icon = { Text(meta.second, fontSize = 19.sp, fontWeight = FontWeight.Bold) },
                                    label = { Text(meta.first, fontSize = 11.sp) },
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    detail?.let { d ->
                        when (d.kind) {
                            DetailKind.UNIT -> UnitDetailScreen(
                                vm = vm,
                                unitId = d.id,
                                back = { detail = null },
                                learnSkill = { detail = ProductDetail(DetailKind.LESSON, it) },
                                practiceSkill = { detail = ProductDetail(DetailKind.DRILL, it, "SKILL") },
                                checkpoint = { detail = ProductDetail(DetailKind.DRILL, d.id, "CHECKPOINT") },
                            )
                            DetailKind.LESSON -> LessonScreen(
                                vm = vm,
                                skillId = d.id,
                                back = { detail = ProductDetail(DetailKind.UNIT, Curriculum.skill(d.id).unitId) },
                                goPractice = { detail = ProductDetail(DetailKind.DRILL, d.id, "SKILL") },
                            )
                            DetailKind.DRILL -> DrillScreen(vm, d.id, d.mode) { detail = null }
                            DetailKind.VOCAB -> VocabularyLabScreen(vm) { detail = null }
                            DetailKind.DICTATION -> DictationLabScreen(vm) { detail = null }
                            DetailKind.SPEAKING -> SpeakingLabScreen(vm) { detail = null }
                        }
                    } ?: when (tab) {
                        ProductTab.TODAY -> TodayScreen(
                            vm = vm,
                            onLearn = { detail = ProductDetail(DetailKind.LESSON, it) },
                            onPractice = { detail = ProductDetail(DetailKind.DRILL, it, "SKILL") },
                            onCoach = {
                                coachMode = CoachMode.WRITE
                                tab = ProductTab.COACH
                            },
                            openUnit = { detail = ProductDetail(DetailKind.UNIT, it) },
                        )
                        ProductTab.ACADEMY -> AcademyScreen(vm) { detail = ProductDetail(DetailKind.UNIT, it) }
                        ProductTab.LAB -> LabScreen(
                            vm = vm,
                            smart = { detail = ProductDetail(DetailKind.DRILL, "", "SMART") },
                            repair = { detail = ProductDetail(DetailKind.DRILL, "", "WEAK") },
                            unitDrill = { detail = ProductDetail(DetailKind.DRILL, vm.learner.value.activeUnitId, "UNIT") },
                            checkpoint = { detail = ProductDetail(DetailKind.DRILL, vm.learner.value.activeUnitId, "CHECKPOINT") },
                            vocab = { detail = ProductDetail(DetailKind.VOCAB) },
                            dictation = { detail = ProductDetail(DetailKind.DICTATION) },
                            speaking = { detail = ProductDetail(DetailKind.SPEAKING) },
                        )
                        ProductTab.COACH -> CoachScreen(
                            vm = vm,
                            mode = coachMode,
                            changeMode = { coachMode = it },
                            practiceActiveUnit = {
                                detail = ProductDetail(DetailKind.DRILL, vm.learner.value.activeUnitId, "UNIT")
                            },
                        )
                        ProductTab.PASSPORT -> PassportScreen(vm) {
                            detail = ProductDetail(DetailKind.DRILL, it, "SKILL")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(
    vm: EnglishCoachViewModel,
    onLearn: (String) -> Unit,
    onPractice: (String) -> Unit,
    onCoach: () -> Unit,
    openUnit: (String) -> Unit,
) {
    val learner by vm.learner.collectAsStateWithLifecycle()
    val level = vm.estimateLevel()
    val plan = vm.dailyPlan()
    val focus = plan.firstOrNull()
    val activeUnit = Curriculum.unit(learner.activeUnitId)

    AppList {
        item {
            Text("SayIt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Indigo)
            Text("إنجليزيتك اليوم", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Ink)
            Text("مش كورس طويل. كل يوم حلقة قصيرة تبني مهارة حقيقية.", color = Muted, fontSize = 14.sp)
        }
        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("تقدير المستوى", color = Color(0xFFB9C0CD), fontSize = 12.sp)
                            Text(level.label, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .1f)) {
                            Text("${level.evidenceAttempts} دليل", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(level.detailAr, color = Color(0xFFD9DEE7), fontSize = 13.sp)
                }
            }
        }
        item { SectionTitle("English Loop", "افهم → جرّب → استخدم") }
        if (focus != null) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("تركيز اليوم", color = Indigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(focus.skill.titleAr, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
                        Text(focus.reasonAr, color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        LoopStep("1", "Academy", "افهم الفكرة والقالب والأخطاء الشائعة", Indigo) { onLearn(focus.skill.id) }
                        LoopStep("2", "Lab", "جرّب المهارة بأسئلة متغيرة وقياس حقيقي", Emerald) { onPractice(focus.skill.id) }
                        LoopStep("3", "Real use", "استخدمها بجملة من حياتك وخلي Coach يصقلها", Amber) { onCoach() }
                    }
                }
            }
        } else {
            item { EmptyState("ما عندك خطة اليوم بعد", "ابدأ وحدة من Academy حتى أبني لك Loop مناسبة.") }
        }
        item { SectionTitle("خطتك الذكية", "سبب كل مهمة ظاهر؛ مفيش Random") }
        items(plan) { item ->
            val p = vm.skillProgress(item.skill.id)
            SlimPlanCard(item.skill, item.reasonAr, item.minutes, p) { onPractice(item.skill.id) }
        }
        item { SectionTitle("الوحدة الحالية", "المكان اللي منه بيتبني تقدمك") }
        item {
            val stats = vm.unitStats(activeUnit.id)
            ActionCard(
                eyebrow = activeUnit.level,
                title = activeUnit.titleAr,
                subtitle = if (stats.second == 0) "لسه ما جمعنا Evidence كفاية" else "إتقان ${stats.first}% • ثقة ${stats.second}%",
                accent = Indigo,
                action = "فتح الوحدة",
            ) { openUnit(activeUnit.id) }
        }
    }
}

@Composable
private fun AcademyScreen(vm: EnglishCoachViewModel, openUnit: (String) -> Unit) {
    val learner by vm.learner.collectAsStateWithLifecycle()
    AppList {
        item {
            Eyebrow("ACADEMY", Indigo)
            Text("تعلّم بشكل منظم", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("هنا فقط المنهج والشرح. التدريب الحر والتصحيح إلهم أماكنهم الخاصة.", color = Muted)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF1FF)), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("◫", fontSize = 28.sp, color = Indigo)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("مسار متدرج، مش قائمة قواعد", fontWeight = FontWeight.Bold)
                        Text("كل Unit فيها Skills صغيرة؛ تتعلم كل واحدة وبعدين تختبرها في Lab.", color = Muted, fontSize = 13.sp)
                    }
                }
            }
        }
        val grouped = Curriculum.units.groupBy { if (it.level.contains("A2") && !it.level.contains("A1")) "A2" else "A1 → A2" }
        grouped.forEach { (level, units) ->
            item { SectionTitle(level, "") }
            items(units) { unit ->
                val stats = vm.unitStats(unit.id)
                val assessed = vm.assessedSkills(unit.id)
                val active = learner.activeUnitId == unit.id
                Card(
                    onClick = { openUnit(unit.id) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (active) Badge { Text("وحدتك الحالية", fontSize = 10.sp) }
                                    Text(unit.level, color = Indigo, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                                }
                                Text(unit.titleAr, fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
                                Text(unit.descriptionAr, color = Muted, fontSize = 13.sp)
                            }
                            Text("›", fontSize = 30.sp, color = Muted)
                        }
                        Spacer(Modifier.height(14.dp))
                        if (assessed == 0) {
                            Text("${unit.skillIds.size} مهارات • ${unit.estimatedMinutes} دقيقة تقريبًا", color = Muted, fontSize = 12.sp)
                        } else {
                            LinearProgressIndicator(progress = { stats.first / 100f }, modifier = Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$assessed/${unit.skillIds.size} مهارات مقاسة", fontSize = 11.sp, color = Muted)
                                Text("إتقان ${stats.first}% • ثقة ${stats.second}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitDetailScreen(
    vm: EnglishCoachViewModel,
    unitId: String,
    back: () -> Unit,
    learnSkill: (String) -> Unit,
    practiceSkill: (String) -> Unit,
    checkpoint: () -> Unit,
) {
    val learner by vm.learner.collectAsStateWithLifecycle()
    val unit = Curriculum.unit(unitId)
    val skills = Curriculum.skillsForUnit(unitId)
    val stats = vm.unitStats(unitId)
    AppList {
        item { BackHeader("Academy", back) }
        item {
            Eyebrow(unit.level, Indigo)
            Text(unit.titleAr, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(unit.descriptionAr, color = Muted)
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
                Column(Modifier.padding(18.dp)) {
                    Text(if (stats.second == 0) "لسه بنبني صورتك في الوحدة" else "صورتك في الوحدة", color = Color(0xFFBCC4D1), fontSize = 12.sp)
                    Text(if (stats.second == 0) "ابدأ أول Skill" else "إتقان ${stats.first}%  ·  ثقة ${stats.second}%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    if (learner.activeUnitId != unitId) {
                        OutlinedButton(onClick = { vm.setActiveUnit(unitId) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                            Text("اجعلها وحدتي الحالية")
                        }
                    } else Text("✓ هذه وحدتك الحالية", color = Color(0xFF9EE6D3), fontWeight = FontWeight.Bold)
                }
            }
        }
        item { SectionTitle("Skills", "كل مهارة تقاس لوحدها") }
        items(skills) { skill ->
            val p = vm.skillProgress(skill.id)
            SkillCard(skill, p, { learnSkill(skill.id) }, { practiceSkill(skill.id) })
        }
        item {
            Spacer(Modifier.height(4.dp))
            Button(onClick = checkpoint, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Indigo)) {
                Text("Checkpoint — اختبر الوحدة بدون تلميحات", fontWeight = FontWeight.Bold)
            }
            Text("الـCheckpoint يعرض النتيجة بالنهاية فقط، لذلك Evidence تبعه أقوى من التدريب العادي.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
private fun LessonScreen(vm: EnglishCoachViewModel, skillId: String, back: () -> Unit, goPractice: () -> Unit) {
    val skill = Curriculum.skill(skillId)
    var checking by remember(skillId) { mutableStateOf(false) }
    var question by remember(skillId) { mutableStateOf(ExerciseFactory.next(skillId)) }
    var selected by remember(skillId) { mutableStateOf<Int?>(null) }
    var number by remember(skillId) { mutableIntStateOf(1) }
    var correct by remember(skillId) { mutableIntStateOf(0) }

    AppList {
        item { BackHeader("${Curriculum.unit(skill.unitId).titleAr}", back) }
        item {
            Eyebrow("LEARN", Indigo)
            Text(skill.titleAr, fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text(skill.goalAr, color = Muted)
        }
        if (!checking) {
            item { LearningBlock("الفكرة", skill.explanationAr, Indigo) }
            item { LearningBlock("القالب", skill.formula, Emerald, english = true) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("أمثلة", fontWeight = FontWeight.Black, fontSize = 17.sp)
                        skill.examples.forEach { example ->
                            Surface(color = SurfaceSoft, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(example, modifier = Modifier.padding(12.dp), fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Start)
                                }
                            }
                        }
                    }
                }
            }
            item { LearningBlock("الفخ الشائع", skill.commonMistakeAr, Rose) }
            item {
                Button(onClick = { checking = true }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Indigo)) {
                    Text("فهمت — اعمل Quick Check", fontWeight = FontWeight.Bold)
                }
                Text("3 أسئلة للتأكد إن الفكرة وصلت، مش جلسة تدريب كاملة.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            item {
                Text("Quick Check  $number / 3", color = Indigo, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { number / 3f }, modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp))
            }
            item {
                ExerciseCard(question, selected, showAnswer = selected != null) { index ->
                    if (selected == null) {
                        selected = index
                        val ok = index == question.correctIndex
                        if (ok) correct++
                        vm.recordLearningAttempt(question, ok)
                    }
                }
            }
            if (selected != null) {
                item {
                    if (number < 3) {
                        Button(onClick = {
                            number++
                            question = ExerciseFactory.next(skillId, question.prompt)
                            selected = null
                        }, modifier = Modifier.fillMaxWidth()) { Text("التالي") }
                    } else {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF8F3)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(18.dp)) {
                                Text("Quick Check: $correct / 3", fontSize = 22.sp, fontWeight = FontWeight.Black)
                                Text(if (correct >= 2) "الفكرة وصلت. الآن خذها للمختبر." else "ارجع فوق للفكرة والفخ الشائع، وبعدها جرّب مرة ثانية.", color = Muted)
                                Button(onClick = goPractice, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Emerald)) {
                                    Text("افتح Lab لهذه المهارة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabScreen(
    vm: EnglishCoachViewModel,
    smart: () -> Unit,
    repair: () -> Unit,
    unitDrill: () -> Unit,
    checkpoint: () -> Unit,
    vocab: () -> Unit,
    dictation: () -> Unit,
    speaking: () -> Unit,
) {
    val learner by vm.learner.collectAsStateWithLifecycle()
    val status by vm.modelStatus.collectAsStateWithLifecycle()
    val activeUnit = Curriculum.unit(learner.activeUnitId)
    val weakCount = vm.topWeakSkills().size

    AppList {
        item {
            Eyebrow("LAB", Emerald)
            Text("جرّب الإنجليزي", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("ما في درس إجباري هنا. اختبر، خرب الجملة، صححها، وخلّي التطبيق يتعلم من أدائك.", color = Muted)
        }
        item {
            ActionCard("ADAPTIVE", "Smart Mix", "يمزج مراجعاتك المستحقة ونقاط ضعفك والمهارة التالية.", Emerald, "ابدأ 8 تحديات", smart)
        }
        item {
            ActionCard("REPAIR", "Weakness Repair", if (weakCount == 0) "لسه ما في ضعف مؤكد؛ رح يستخدم وحدتك الحالية." else "$weakCount نقاط ضعف مؤكدة من Evidence حقيقية.", Rose, "صلّح الضعف", repair)
        }
        item {
            ActionCard(activeUnit.level, "Unit Drill — ${activeUnit.titleAr}", "تدريب متنوع داخل الوحدة الحالية مع Feedback بعد كل سؤال.", Indigo, "تدرّب", unitDrill)
        }
        item {
            ActionCard("NO HINTS", "Checkpoint", "اختبار قصير. ما بنقولك الصح والغلط إلا في النهاية.", Amber, "اختبر نفسي", checkpoint)
        }
        item { SectionTitle("Studios", "مهارات منفصلة، بنفس روح المختبر") }
        item { StudioRow("Aa", "Vocabulary Forge", "معنى + سياق + استدعاء من الذاكرة", Indigo, vocab) }
        item { StudioRow("◖", "Dictation Studio", "اسمع واكتب، وشوف الفرق كلمة بكلمة", Emerald, dictation) }
        item { StudioRow("◉", "Speaking Studio", if (status.speechReady) "جاهز أوفلاين" else "يحتاج تنزيل موديل الصوت مرة واحدة", Rose, speaking) }
    }
}

@Composable
private fun DrillScreen(vm: EnglishCoachViewModel, id: String, mode: String, back: () -> Unit) {
    val learner by vm.learner.collectAsStateWithLifecycle()
    val skills = remember(id, mode, learner) {
        when (mode) {
            "SKILL" -> listOf(Curriculum.skill(id))
            "UNIT", "CHECKPOINT" -> Curriculum.skillsForUnit(id)
            "WEAK" -> vm.topWeakSkills(6).ifEmpty { Curriculum.skillsForUnit(learner.activeUnitId) }
            else -> vm.dailyPlan().map { it.skill }.ifEmpty { Curriculum.skillsForUnit(learner.activeUnitId) }
        }
    }
    val isCheckpoint = mode == "CHECKPOINT"
    val total = if (isCheckpoint) 12 else 8
    var number by remember(id, mode) { mutableIntStateOf(1) }
    var score by remember(id, mode) { mutableIntStateOf(0) }
    var selected by remember(id, mode) { mutableStateOf<Int?>(null) }
    var finished by remember(id, mode) { mutableStateOf(false) }
    var misses by remember(id, mode) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var skill by remember(id, mode) { mutableStateOf(skills.first()) }
    var question by remember(id, mode) { mutableStateOf(ExerciseFactory.next(skill.id)) }

    fun nextQuestion() {
        skill = skills[(number) % skills.size]
        question = ExerciseFactory.next(skill.id, question.prompt)
        selected = null
    }

    AppList {
        item { BackHeader(if (isCheckpoint) "Checkpoint" else "Lab", back) }
        if (finished) {
            item {
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = if (score >= total * .7) Color(0xFFEAF8F3) else Color(0xFFFFF3EE))) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isCheckpoint) "Checkpoint complete" else "Lab session complete", color = Muted)
                        Text("$score / $total", fontSize = 42.sp, fontWeight = FontWeight.Black)
                        Text(when {
                            score >= total * .85 -> "قوي. رح تتباعد مراجعات المهارات اللي أثبتها اليوم."
                            score >= total * .65 -> "جيد، لكن لسه في أجزاء تحتاج تثبيت."
                            else -> "طلع عندنا Evidence واضح على اللي لازم نصلحه بعدين."
                        }, textAlign = TextAlign.Center, color = Muted)
                    }
                }
            }
            if (misses.isNotEmpty()) {
                item { SectionTitle("أين تعثرت؟", "مش بس نتيجة نهائية") }
                items(misses.entries.sortedByDescending { it.value }) { entry ->
                    val s = Curriculum.skill(entry.key)
                    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.titleAr, fontWeight = FontWeight.Bold)
                            Text("${entry.value} خطأ", color = Rose, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            return@AppList
        }
        item {
            Eyebrow(if (isCheckpoint) "NO HINTS" else "ACTIVE PRACTICE", if (isCheckpoint) Amber else Emerald)
            Text(if (isCheckpoint) "اختبار الوحدة" else skill.titleAr, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(if (isCheckpoint) "مش رح نكشف الإجابة قبل نهاية الاختبار." else "Feedback مباشر، والسؤال التالي بيتغير.", color = Muted)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$number / $total", color = Muted)
                if (!isCheckpoint) Text(skill.titleEn, color = Indigo, fontSize = 12.sp)
            }
            LinearProgressIndicator(progress = { number / total.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))
        }
        item {
            ExerciseCard(question, selected, showAnswer = selected != null && !isCheckpoint) { index ->
                if (selected == null) {
                    selected = index
                    val ok = index == question.correctIndex
                    if (ok) score++ else misses = misses + (question.skillId to ((misses[question.skillId] ?: 0) + 1))
                    vm.recordLearningAttempt(question, ok)
                }
            }
        }
        if (selected != null) {
            item {
                if (isCheckpoint) {
                    Surface(color = SurfaceSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("تم تسجيل الإجابة. النتيجة تظهر بالنهاية.", modifier = Modifier.padding(14.dp), color = Muted)
                    }
                }
                Button(onClick = {
                    if (number >= total) {
                        finished = true
                        vm.finishStudySession()
                    } else {
                        number++
                        nextQuestion()
                    }
                }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isCheckpoint) Amber else Emerald)) {
                    Text(if (number == total) "إنهاء" else "التالي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CoachScreen(vm: EnglishCoachViewModel, mode: CoachMode, changeMode: (CoachMode) -> Unit, practiceActiveUnit: () -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val writing by vm.writingFeedback.collectAsStateWithLifecycle()
    val improve by vm.improveFeedback.collectAsStateWithLifecycle()
    val learner by vm.learner.collectAsStateWithLifecycle()
    val activeUnit = Curriculum.unit(learner.activeUnitId)
    var text by remember(mode) { mutableStateOf("") }

    AppList {
        item {
            Eyebrow("COACH", Amber)
            Text("هات إنجليزيتك الحقيقية", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("هنا ما في امتحان. اكتب اللي بدك تحكيه فعلًا وأنا أصلحه وأقويه.", color = Muted)
        }
        item {
            Surface(color = Color(0xFFFFF5E7), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Text("قاعدة مهمة", fontWeight = FontWeight.Black, color = Ink)
                    Text("Coach لا يرفع أو ينزل مستواك تلقائيًا. مستوى Skill Passport يعتمد على تمارين واختبارات قابلة للقياس فقط.", color = Muted, fontSize = 13.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoachMode.entries.forEach { item ->
                    val label = when (item) { CoachMode.FIX -> "صحح"; CoachMode.UPGRADE -> "قوّي"; CoachMode.WRITE -> "اكتب" }
                    FilterChip(selected = mode == item, onClick = { changeMode(item) }, label = { Text(label) }, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    if (mode == CoachMode.WRITE) {
                        Text("مهمة كتابة من حياتك", color = Amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("اكتب 3–5 جمل عن يومك. لو قدرت، استخدم شيء من ${activeUnit.titleAr}.", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                    } else {
                        Text(if (mode == CoachMode.FIX) "اكتب جملة أو فقرة وأنا أحدد الخطأ وأشرح السبب." else "اكتب جملة صحيحة أو بسيطة، ونصعدها درجة بدون ما نخرب معناها.", color = Muted, fontSize = 13.sp)
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        minLines = if (mode == CoachMode.WRITE) 5 else 3,
                        label = { Text("اكتب بالإنجليزية") },
                    )
                    Button(onClick = {
                        if (mode == CoachMode.UPGRADE) {
                            vm.clearImproveFeedback(); vm.improveSentence(text)
                        } else {
                            vm.clearWritingFeedback(); vm.reviewWriting(text)
                        }
                    }, enabled = text.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Amber)) {
                        Text(if (busy) "جاري التحليل..." else when (mode) { CoachMode.FIX -> "صححلي"; CoachMode.UPGRADE -> "قوّي الجملة"; CoachMode.WRITE -> "راجع كتابتي" }, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (mode == CoachMode.UPGRADE) {
            improve?.let { item { ImproveResult(it) } }
        } else {
            writing?.let { feedback ->
                item { WritingResult(feedback) }
                item {
                    OutlinedButton(onClick = practiceActiveUnit, modifier = Modifier.fillMaxWidth()) {
                        Text("بدك تثبتها؟ افتح Lab لوحدتك الحالية")
                    }
                }
            }
        }
    }
}

@Composable
private fun PassportScreen(vm: EnglishCoachViewModel, practice: (String) -> Unit) {
    val learner by vm.learner.collectAsStateWithLifecycle()
    val legacy by vm.progress.collectAsStateWithLifecycle()
    val level = vm.estimateLevel()
    val assessed = Curriculum.skills.mapNotNull { s -> vm.skillProgress(s.id).takeIf { it.attempts > 0 }?.let { s to it } }
    val strong = assessed.filter { it.second.confidence() >= 32 }.sortedByDescending { it.second.mastery() }.take(4)
    val weak = vm.topWeakSkills(5)
    val errors = vm.topErrors(6)
    val totalAttempts = learner.skillProgress.values.sumOf { it.attempts }

    AppList {
        item {
            Eyebrow("SKILL PASSPORT", Indigo)
            Text("صورتك الحقيقية", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("مش XP ولا رقم تجميلي. كل سطر تحت مبني على Evidence خزّنها التطبيق من أدائك.", color = Muted)
        }
        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Estimated CEFR", color = Color(0xFFBFC6D3), fontSize = 12.sp)
                    Text(level.label, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    Text(level.detailAr, color = Color(0xFFD8DEE8), fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkMetric("Evidence", "$totalAttempts", Modifier.weight(1f))
                        DarkMetric("Skills", "${assessed.size}/${Curriculum.skills.size}", Modifier.weight(1f))
                        DarkMetric("Sessions", "${learner.totalStudySessions}", Modifier.weight(1f))
                    }
                }
            }
        }
        if (weak.isNotEmpty()) {
            item { SectionTitle("نقاط تحتاج إصلاح", "ثقة كافية + إتقان أقل من المطلوب") }
            items(weak) { skill ->
                val p = vm.skillProgress(skill.id)
                PassportSkillRow(skill, p, Rose) { practice(skill.id) }
            }
        }
        if (strong.isNotEmpty()) {
            item { SectionTitle("أقوى مهاراتك", "مش بنسميها قوية قبل ما يكون عندنا Evidence كفاية") }
            items(strong) { (skill, p) -> PassportSkillRow(skill, p, Emerald) { practice(skill.id) } }
        }
        item { SectionTitle("Mistake DNA", "الأخطاء اللي بتتكرر عبر الجلسات") }
        if (errors.isEmpty()) item { EmptyState("لسه ما عندنا نمط أخطاء", "بعد عدة جلسات رح يظهر هون مش بس شو غلطت، بل شو بيتكرر.") }
        else items(errors) { (tag, count) ->
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Curriculum.errorLabel(tag), fontWeight = FontWeight.Bold)
                    Text("×$count", color = Rose, fontWeight = FontWeight.Black)
                }
            }
        }
        item { SectionTitle("Practice signals", "مؤشرات منفصلة؛ مش داخلة في CEFR حاليًا") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SignalRow("Vocabulary", legacy.vocabularyCorrect, legacy.vocabularyTotal)
                    SignalRow("Spelling", legacy.spellingCorrect, legacy.spellingTotal)
                    SignalRow("Dictation", legacy.dictationCorrect, legacy.dictationTotal)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Speaking attempts", color = Muted)
                        Text("${legacy.speakingAttempts}", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabularyLabScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    var item by remember { mutableStateOf(SeedData.vocabulary.random()) }
    var round by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    val mode = (round - 1) % 3
    val options = remember(item.id, round) {
        when (mode) {
            0 -> (SeedData.vocabulary.filter { it.id != item.id }.shuffled().take(3).map { it.meaningAr } + item.meaningAr).distinct().shuffled()
            1 -> (SeedData.vocabulary.filter { it.id != item.id }.shuffled().take(3).map { it.word } + item.word).distinct().shuffled()
            else -> emptyList()
        }
    }
    AppList {
        item { BackHeader("Lab", back) }
        item { Eyebrow("VOCABULARY FORGE", Indigo); Text("استخدم الكلمة، مش بس احفظها", fontSize = 27.sp, fontWeight = FontWeight.Black) }
        if (round > 7) {
            item { ResultCard("جلسة الكلمات", score, 7, "التحديات تنقلت بين التعرف والسياق والاستدعاء.") }
            return@AppList
        }
        item { Text("$round / 7", color = Muted); LinearProgressIndicator(progress = { round / 7f }, modifier = Modifier.fillMaxWidth()) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    when (mode) {
                        0 -> {
                            Text("اختار المعنى", color = Muted); Text(item.word, fontSize = 34.sp, fontWeight = FontWeight.Black)
                            options.forEach { opt -> ChoiceOption(opt, selected, result, opt == item.meaningAr) {
                                if (result == null) { selected = opt; val ok = opt == item.meaningAr; result = ok; if (ok) score++; vm.answerVocabulary(ok, item.id) }
                            } }
                        }
                        1 -> {
                            Text("اختار الكلمة المناسبة", color = Muted)
                            val blanked = Regex("\\b${Regex.escape(item.word)}\\b", RegexOption.IGNORE_CASE).replace(item.example, "___")
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(blanked, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start) }
                            options.forEach { opt -> ChoiceOption(opt, selected, result, opt == item.word) {
                                if (result == null) { selected = opt; val ok = opt == item.word; result = ok; if (ok) score++; vm.answerVocabulary(ok, item.id) }
                            } }
                        }
                        else -> {
                            Text("استدعاء من الذاكرة", color = Muted); Text(item.meaningAr, fontSize = 26.sp, fontWeight = FontWeight.Black)
                            OutlinedTextField(answer, { answer = it; result = null }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("الكلمة بالإنجليزية") }, singleLine = true)
                            Button(onClick = { val ok = answer.trim().equals(item.word, true); result = ok; if (ok) score++; vm.answerVocabulary(ok, item.id) }, enabled = answer.isNotBlank() && result == null, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("تحقق") }
                        }
                    }
                    result?.let { ok ->
                        HorizontalDivider(Modifier.padding(vertical = 13.dp))
                        Text(if (ok) "✓ ممتاز" else "الصحيح: ${item.word} — ${item.meaningAr}", fontWeight = FontWeight.Black)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(item.example, modifier = Modifier.padding(top = 7.dp), textAlign = TextAlign.Start) }
                        Button(onClick = { round++; item = nextWord(item); result = null; selected = null; answer = "" }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Indigo)) { Text("التالي") }
                    }
                }
            }
        }
    }
}

private fun nextWord(old: VocabularyItem): VocabularyItem = SeedData.vocabulary.filter { it.id != old.id }.random()

@Composable
private fun DictationLabScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val tts = rememberProductTts()
    var item by remember { mutableStateOf(SeedData.dictation.random()) }
    var answer by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Int?>(null) }
    AppList {
        item { BackHeader("Lab", back) }
        item { Eyebrow("DICTATION STUDIO", Emerald); Text("اسمع التفاصيل", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("الهدف مش تسمع الفكرة العامة؛ الهدف تلتقط الكلمات والتركيب.", color = Muted) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("${item.level} • جملة جديدة كل مرة", color = Muted)
                    Button(onClick = { tts?.speak(item.text, TextToSpeech.QUEUE_FLUSH, null, "dictation-v2") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Emerald)) { Text("▶ اسمع") }
                    OutlinedTextField(answer, { answer = it; result = null }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), minLines = 3, label = { Text("اكتب اللي سمعته") })
                    Button(onClick = { val s = similarityPercent(item.text, answer); result = s; vm.recordDictation(s) }, enabled = answer.isNotBlank() && result == null, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("قارن") }
                    result?.let { s ->
                        Text("$s%", fontSize = 34.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 14.dp))
                        if (s < 100) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(item.text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
                                Text(spellingDiff(item.text, answer), color = Muted, textAlign = TextAlign.Start)
                            }
                        }
                        OutlinedButton(onClick = { val old = item.id; item = SeedData.dictation.filter { it.id != old }.random(); answer = ""; result = null }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("جملة ثانية") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeakingLabScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val context = LocalContext.current
    val learner by vm.learner.collectAsStateWithLifecycle()
    val status by vm.modelStatus.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val transcript by vm.transcript.collectAsStateWithLifecycle()
    val downloadProgress by vm.modelDownloadProgress.collectAsStateWithLifecycle()
    val downloadLabel by vm.modelDownloadLabel.collectAsStateWithLifecycle()
    val skill = remember(learner.activeUnitId) { Curriculum.skillsForUnit(learner.activeUnitId).random() }
    var target by remember(skill.id) { mutableStateOf(skill.examples.random()) }
    val recorder = remember { WavRecorder() }
    val audioFile = remember { File(context.cacheDir, "sayit-v2-speaking.wav") }
    var recording by remember { mutableStateOf(false) }
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permission = granted }

    AppList {
        item { BackHeader("Lab", back) }
        item { Eyebrow("SPEAKING STUDIO", Rose); Text("قلها بصوتك", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("نقيس وضوح الكلمات ومطابقة الجملة. مش بندّعي إنه تحليل phoneme كامل.", color = Muted) }
        if (!status.speechReady) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F4)), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(17.dp)) {
                        Text("جهّز الصوت مرة واحدة", fontWeight = FontWeight.Black)
                        Text("نزّل Whisper Tiny وبعدها الاستماع والتحويل يشتغلوا أوفلاين.", color = Muted, fontSize = 13.sp)
                        if (busy && downloadLabel.isNotBlank()) { LinearProgressIndicator(progress = { downloadProgress / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)); Text("$downloadLabel — $downloadProgress%", color = Muted, fontSize = 11.sp) }
                        Button(onClick = vm::downloadModels, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("تنزيل موديل الصوت") }
                    }
                }
            }
            return@AppList
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Target • ${skill.titleAr}", color = Rose, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(target, fontSize = 23.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) }
                    Button(onClick = {
                        if (!permission) launcher.launch(Manifest.permission.RECORD_AUDIO)
                        else if (!recording) { vm.clearTranscript(); recorder.start(audioFile); recording = true }
                        else { recorder.stop(); recording = false; vm.transcribeAndScore(audioFile, target, skill.id) }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = if (recording) Rose else Ink)) {
                        Text(if (recording) "■ وقف وحلل" else "● ابدأ التسجيل", fontWeight = FontWeight.Bold)
                    }
                    if (transcript.isNotBlank()) {
                        val score = similarityPercent(target, transcript)
                        HorizontalDivider(Modifier.padding(vertical = 14.dp))
                        Text("وضوح/مطابقة: $score%", fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text("سمعنا:", color = Muted, fontSize = 12.sp)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(transcript, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                        OutlinedButton(onClick = { vm.clearTranscript(); target = skill.examples.random() }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("جملة ثانية") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(question: LearningExercise, selected: Int?, showAnswer: Boolean, choose: (Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(20.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(question.prompt, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            }
            Spacer(Modifier.height(14.dp))
            question.options.forEachIndexed { index, option ->
                val correct = index == question.correctIndex
                val bg = when {
                    !showAnswer -> SurfaceSoft
                    correct -> Color(0xFFE5F7F1)
                    selected == index -> Color(0xFFFFE9ED)
                    else -> SurfaceSoft
                }
                Button(
                    onClick = { choose(index) },
                    enabled = selected == null,
                    colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Ink, disabledContainerColor = bg, disabledContentColor = Ink),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(option, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) } }
            }
            if (showAnswer && selected != null) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(if (selected == question.correctIndex) "✓ صحيح" else "الصحيح: ${question.options[question.correctIndex]}", fontWeight = FontWeight.Black, color = if (selected == question.correctIndex) Emerald else Rose)
                Text(question.explanationAr, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
private fun WritingResult(feedback: WritingFeedback) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("مراجعة Coach", color = Amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${feedback.score}/100", fontSize = 30.sp, fontWeight = FontWeight.Black)
            ResultEnglishBlock("التصحيح", feedback.corrected)
            feedback.issues.forEach { issue ->
                Surface(color = Color(0xFFFFF4F0), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(issue.title, fontWeight = FontWeight.Bold)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text("${issue.before} → ${issue.after}", color = Rose, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                        Text(issue.explanationAr, color = Muted, fontSize = 12.sp)
                    }
                }
            }
            ResultEnglishBlock("نسخة طبيعية", feedback.better)
        }
    }
}

@Composable
private fun ImproveResult(feedback: ImproveFeedback) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("Level up", color = Amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            ResultEnglishBlock("صح", feedback.corrected)
            ResultEnglishBlock("طبيعي", feedback.natural)
            ResultEnglishBlock("أقوى", feedback.stronger)
            feedback.upgrades.forEach { up -> Text("${up.simple} → ${up.stronger}  •  ${up.meaningAr}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) }
            Text(feedback.tipAr, color = Muted, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun ResultEnglishBlock(label: String, text: String) {
    Text(label, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    Surface(color = SurfaceSoft, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(text, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Start, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun SkillCard(skill: SkillDefinition, p: SkillProgress, learn: () -> Unit, practice: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(skill.titleAr, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(skill.goalAr, color = Muted, fontSize = 12.sp)
                }
                Text(if (p.attempts == 0) "جديدة" else p.statusAr(), color = if (p.mastery() < 60 && p.attempts > 0) Rose else Indigo, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            if (p.attempts > 0) {
                LinearProgressIndicator(progress = { p.mastery() / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إتقان ${p.mastery()}%", color = Muted, fontSize = 11.sp)
                    Text("ثقة ${p.confidence()}% • ${p.attempts} محاولات", color = Muted, fontSize = 11.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = learn, modifier = Modifier.weight(1f)) { Text("تعلّم") }
                Button(onClick = practice, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Emerald)) { Text("تدرّب") }
            }
        }
    }
}

@Composable
private fun PassportSkillRow(skill: SkillDefinition, p: SkillProgress, accent: Color, practice: () -> Unit) {
    Card(onClick = practice, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(skill.titleAr, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${p.mastery()}%", fontWeight = FontWeight.Black, color = accent)
            }
            LinearProgressIndicator(progress = { p.mastery() / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = accent)
            Text("ثقة ${p.confidence()}% • ${p.attempts} Evidence", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun LearningBlock(title: String, body: String, accent: Color, english: Boolean = false) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.Black, fontSize = 13.sp)
            if (english) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(body, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 7.dp), textAlign = TextAlign.Start) }
            else Text(body, fontSize = 15.sp, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
private fun LoopStep(number: String, title: String, subtitle: String, accent: Color, click: () -> Unit) {
    Card(onClick = click, colors = CardDefaults.cardColors(containerColor = SurfaceSoft), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = accent, shape = CircleShape) { Text(number, color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 12.sp) }
            Text("›", color = accent, fontSize = 26.sp)
        }
    }
}

@Composable
private fun SlimPlanCard(skill: SkillDefinition, reason: String, minutes: Int, p: SkillProgress, click: () -> Unit) {
    Card(onClick = click, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFEFF1FF), shape = RoundedCornerShape(12.dp)) { Text("$minutes د", color = Indigo, fontWeight = FontWeight.Black, modifier = Modifier.padding(10.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(skill.titleAr, fontWeight = FontWeight.Bold); Text(reason, color = Muted, fontSize = 11.sp) }
            if (p.attempts > 0) Text("${p.mastery()}%", color = Indigo, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ActionCard(eyebrow: String, title: String, subtitle: String, accent: Color, action: String, click: () -> Unit) {
    Card(onClick = click, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Eyebrow(eyebrow, accent)
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
            Text(subtitle, color = Muted, fontSize = 13.sp)
            Text("$action  ←", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun StudioRow(symbol: String, title: String, subtitle: String, accent: Color, click: () -> Unit) {
    Card(onClick = click, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = accent.copy(alpha = .1f), shape = RoundedCornerShape(13.dp)) { Text(symbol, color = accent, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(12.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 12.sp) }
            Text("›", color = Muted, fontSize = 27.sp)
        }
    }
}

@Composable
private fun ChoiceOption(label: String, selected: String?, result: Boolean?, correct: Boolean, click: () -> Unit) {
    val bg = when { result == null -> SurfaceSoft; selected == label && correct -> Color(0xFFE5F7F1); selected == label -> Color(0xFFFFE9ED); correct -> Color(0xFFE5F7F1); else -> SurfaceSoft }
    Button(onClick = click, enabled = result == null, modifier = Modifier.fillMaxWidth().padding(top = 7.dp), colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Ink, disabledContainerColor = bg, disabledContentColor = Ink)) {
        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
    }
}

@Composable
private fun ResultCard(title: String, score: Int, total: Int, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Muted)
            Text("$score / $total", fontSize = 40.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SignalRow(label: String, correct: Int, total: Int) {
    val text = if (total == 0) "لا بيانات" else "${(correct * 100 / total.coerceAtLeast(1))}%  ($total محاولة)"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted); Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun DarkMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = Color.White.copy(alpha = .08f), shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Color.White, fontWeight = FontWeight.Black); Text(label, color = Color(0xFFBFC6D3), fontSize = 10.sp) }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 7.dp)) { Text(title, fontSize = 19.sp, fontWeight = FontWeight.Black); if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 12.sp) }
}

@Composable
private fun Eyebrow(text: String, color: Color) { Text(text.uppercase(), color = color, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp) }

@Composable
private fun BackHeader(label: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = back) { Text("‹ رجوع") }; Text(label, color = Muted, fontSize = 12.sp) }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(17.dp)) { Text(title, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 12.sp) } }
}

@Composable
private fun AppList(content: LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
}

@Composable
private fun rememberProductTts(): TextToSpeech? {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) { engine.language = Locale.US; tts = engine } }
        onDispose { engine.stop(); engine.shutdown(); tts = null }
    }
    return tts
}
