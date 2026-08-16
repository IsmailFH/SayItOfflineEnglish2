package com.sayit.offlineenglish

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayit.offlineenglish.ai.ImproveFeedback
import com.sayit.offlineenglish.ai.WritingFeedback
import com.sayit.offlineenglish.audio.WavRecorder
import com.sayit.offlineenglish.learning.Curriculum
import com.sayit.offlineenglish.learning.ExerciseFactory
import com.sayit.offlineenglish.learning.LearnerState
import com.sayit.offlineenglish.learning.SkillDefinition
import com.sayit.offlineenglish.learning.SkillProgress
import com.sayit.offlineenglish.learning.UnitDefinition
import com.sayit.offlineenglish.util.similarityPercent
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: EnglishCoachViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SayItTheme { SayItApp(vm) } }
    }
}

enum class Screen {
    TODAY, LEARN, REVIEW, PROGRESS,
    UNIT, LESSON, PRACTICE,
    LAB, WRITING, IMPROVE, SPEAKING,
}

private val Navy = Color(0xFF101828)
private val Green = Color(0xFF17B26A)
private val SoftGreen = Color(0xFFECFDF3)
private val SoftBlue = Color(0xFFEFF8FF)
private val SoftOrange = Color(0xFFFFFAEB)
private val PageBg = Color(0xFFF7F8FA)
private val Muted = Color(0xFF667085)

@Composable
private fun SayItTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            onPrimary = Color.White,
            secondary = Green,
            background = PageBg,
            surface = Color.White,
            outline = Color(0xFFE4E7EC),
        ),
        content = content,
    )
}

@Composable
private fun SayItApp(vm: EnglishCoachViewModel) {
    var screen by remember { mutableStateOf(Screen.TODAY) }
    var selectedUnitId by remember { mutableStateOf("present_continuous") }
    var selectedSkillId by remember { mutableStateOf("pc.form") }
    val learner by vm.learner.collectAsStateWithLifecycle()

    fun openUnit(unitId: String) {
        selectedUnitId = unitId
        vm.setActiveUnit(unitId)
        screen = Screen.UNIT
    }

    fun openLesson(skillId: String) {
        selectedSkillId = skillId
        selectedUnitId = Curriculum.skill(skillId).unitId
        vm.setActiveUnit(selectedUnitId)
        screen = Screen.LESSON
    }

    fun openPractice(skillId: String) {
        selectedSkillId = skillId
        selectedUnitId = Curriculum.skill(skillId).unitId
        vm.setActiveUnit(selectedUnitId)
        screen = Screen.PRACTICE
    }

    val topLevel = screen in listOf(Screen.TODAY, Screen.LEARN, Screen.REVIEW, Screen.PROGRESS)

    Scaffold(
        containerColor = PageBg,
        bottomBar = {
            if (topLevel) {
                NavigationBar(containerColor = Color.White) {
                    listOf(
                        Screen.TODAY to ("⌂" to "اليوم"),
                        Screen.LEARN to ("▤" to "تعلّم"),
                        Screen.REVIEW to ("↻" to "مراجعة"),
                        Screen.PROGRESS to ("↗" to "تقدمي"),
                    ).forEach { (target, pair) ->
                        NavigationBarItem(
                            selected = screen == target,
                            onClick = { screen = target },
                            icon = { Text(pair.first, fontSize = 20.sp) },
                            label = { Text(pair.second) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                Screen.TODAY -> TodayScreen(
                    vm = vm,
                    learner = learner,
                    onSkill = ::openPractice,
                    onUnit = ::openUnit,
                    onLab = { screen = Screen.LAB },
                )
                Screen.LEARN -> LearnScreen(vm, learner, ::openUnit)
                Screen.REVIEW -> ReviewScreen(vm, learner, ::openPractice)
                Screen.PROGRESS -> ProgressScreen(vm, learner, ::openLesson)
                Screen.UNIT -> UnitScreen(
                    vm = vm,
                    learner = learner,
                    unit = Curriculum.unit(selectedUnitId),
                    back = { screen = Screen.LEARN },
                    onLearn = ::openLesson,
                    onPractice = ::openPractice,
                )
                Screen.LESSON -> LessonScreen(
                    skill = Curriculum.skill(selectedSkillId),
                    progress = learner.skillProgress[selectedSkillId] ?: SkillProgress(),
                    back = { screen = Screen.UNIT },
                    practice = { openPractice(selectedSkillId) },
                )
                Screen.PRACTICE -> SkillPracticeScreen(
                    vm = vm,
                    learner = learner,
                    skill = Curriculum.skill(selectedSkillId),
                    back = { screen = Screen.UNIT },
                    reviewLesson = { screen = Screen.LESSON },
                )
                Screen.LAB -> LabScreen(
                    back = { screen = Screen.TODAY },
                    writing = { screen = Screen.WRITING },
                    improve = { screen = Screen.IMPROVE },
                    speaking = { screen = Screen.SPEAKING },
                )
                Screen.WRITING -> WritingScreen(vm, learner, back = { screen = Screen.LAB })
                Screen.IMPROVE -> ImproveScreen(vm, back = { screen = Screen.LAB })
                Screen.SPEAKING -> SpeakingScreen(vm, learner, back = { screen = Screen.LAB })
            }
        }
    }
}

@Composable
private fun TodayScreen(
    vm: EnglishCoachViewModel,
    learner: LearnerState,
    onSkill: (String) -> Unit,
    onUnit: (String) -> Unit,
    onLab: () -> Unit,
) {
    val status by vm.modelStatus.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val downloadProgress by vm.modelDownloadProgress.collectAsStateWithLifecycle()
    val downloadLabel by vm.modelDownloadLabel.collectAsStateWithLifecycle()
    val level = vm.estimateLevel()
    val plan = vm.dailyPlan()
    val activeUnit = Curriculum.unit(learner.activeUnitId)
    val (unitMastery, unitConfidence) = vm.unitStats(activeUnit.id)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("SayIt", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Navy)
            Text("مدرّبك الإنجليزي — ذكي وأوفلاين", color = Muted)
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Navy),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("تقدير المستوى", color = Color(0xFFCED4DC), fontSize = 13.sp)
                    Text(level.label, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
                    Text(level.detailAr, color = Color(0xFFE4E7EC), fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("مبني على ${level.evidenceAttempts} محاولة فعلية", color = Color(0xFFAAB3C0), fontSize = 12.sp)
                }
            }
        }

        item {
            SectionTitle("خطة اليوم", "مصممة من أدائك ومواعيد المراجعة")
        }
        items(plan) { item ->
            PlanCard(
                title = item.skill.titleAr,
                subtitle = "${item.skill.titleEn} · ${item.reasonAr}",
                minutes = item.minutes,
                progress = learner.skillProgress[item.skill.id],
                onClick = { onSkill(item.skill.id) },
            )
        }

        item {
            Card(
                onClick = { onUnit(activeUnit.id) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SoftBlue),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("الوحدة الحالية", color = Muted, fontSize = 12.sp)
                    Text(activeUnit.titleAr, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy)
                    Text(activeUnit.descriptionAr, color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    EvidenceProgress("الإتقان", unitMastery, unitConfidence)
                }
            }
        }

        item {
            SectionTitle("مختبر المهارات", "كتابة، تحسين جمل، وتحدث")
            Card(
                onClick = onLab,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("طبّق الإنجليزي بحرية", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("اكتب، حسّن جملتك، أو تدرب على الكلام", color = Muted, fontSize = 13.sp)
                    }
                    Text("›", fontSize = 28.sp, color = Muted)
                }
            }
        }

        if (!status.speechReady) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SoftOrange), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🎙️ جهّز التدريب الصوتي", fontWeight = FontWeight.Bold)
                        Text("نزّل Whisper Tiny مرة واحدة، وبعدها الكلام يعمل أوفلاين.", color = Muted, fontSize = 13.sp)
                        if (busy && downloadLabel.isNotBlank()) {
                            LinearProgressIndicator(
                                progress = { downloadProgress.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            )
                            Text("$downloadLabel — $downloadProgress%", color = Muted, fontSize = 12.sp)
                        }
                        Button(
                            onClick = vm::downloadModels,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        ) { Text(if (busy) "جاري التنزيل…" else "تنزيل موديل الصوت") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    minutes: Int,
    progress: SkillProgress?,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(SoftGreen, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("${minutes}m", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Navy)
                Text(subtitle, color = Muted, fontSize = 12.sp)
                if (progress != null && progress.attempts > 0) {
                    Text("إتقان ${progress.mastery()}% · ثقة ${progress.confidence()}%", color = Green, fontSize = 11.sp)
                }
            }
            Text("›", color = Muted, fontSize = 26.sp)
        }
    }
}

@Composable
private fun LearnScreen(vm: EnglishCoachViewModel, learner: LearnerState, openUnit: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("المنهج", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Navy)
            Text("تعلم بشكل مرتب، أو ادخل أي وحدة وتدرب مباشرة.", color = Muted)
        }
        items(Curriculum.units) { unit ->
            val (mastery, confidence) = vm.unitStats(unit.id)
            val assessed = vm.assessedSkills(unit.id)
            Card(
                onClick = { openUnit(unit.id) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(unit.level, color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(unit.titleAr, fontSize = 21.sp, fontWeight = FontWeight.Black, color = Navy)
                            Text(unit.titleEn, color = Muted, fontSize = 13.sp)
                        }
                        Text("${unit.estimatedMinutes} د", color = Muted, fontSize = 12.sp)
                    }
                    Text(unit.descriptionAr, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(12.dp))
                    if (assessed == 0) {
                        Text("لم تبدأ بعد · ${unit.skillIds.size} مهارات", color = Muted, fontSize = 12.sp)
                    } else {
                        EvidenceProgress("$assessed/${unit.skillIds.size} مهارات مقاسة", mastery, confidence)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitScreen(
    vm: EnglishCoachViewModel,
    learner: LearnerState,
    unit: UnitDefinition,
    back: () -> Unit,
    onLearn: (String) -> Unit,
    onPractice: (String) -> Unit,
) {
    val skills = Curriculum.skillsForUnit(unit.id)
    val (mastery, confidence) = vm.unitStats(unit.id)
    val nextSkill = skills.firstOrNull { (learner.skillProgress[it.id]?.attempts ?: 0) == 0 }
        ?: skills.minByOrNull { learner.skillProgress[it.id]?.mastery() ?: 0 }
        ?: skills.first()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PageHeader(unit.titleAr, back) }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("${unit.level} · ${unit.titleEn}", color = Color(0xFFD0D5DD))
                    Text(unit.descriptionAr, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    EvidenceProgress("تقدم الوحدة", mastery, confidence, dark = true)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onLearn(nextSkill.id) }, modifier = Modifier.weight(1f)) { Text("تعلّم") }
                OutlinedButton(onClick = { onPractice(nextSkill.id) }, modifier = Modifier.weight(1f)) { Text("تدرب مباشرة") }
            }
        }
        item { SectionTitle("مهارات الوحدة", "كل نسبة مبنية على إجاباتك الفعلية") }
        items(skills) { skill ->
            val p = learner.skillProgress[skill.id] ?: SkillProgress()
            Card(
                onClick = { onLearn(skill.id) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(skill.titleAr, fontWeight = FontWeight.Bold, color = Navy)
                            Text(skill.titleEn, color = Muted, fontSize = 12.sp)
                        }
                        StatusPill(p.statusAr(), p)
                    }
                    if (p.attempts > 0) {
                        Spacer(Modifier.height(10.dp))
                        EvidenceProgress("${p.attempts} محاولة", p.mastery(), p.confidence())
                    } else {
                        Text("لم يتم تقييمها بعد", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonScreen(skill: SkillDefinition, progress: SkillProgress, back: () -> Unit, practice: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageHeader(skill.titleAr, back) }
        item {
            Text(skill.titleEn, color = Green, fontWeight = FontWeight.Bold)
            Text(skill.goalAr, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
        }
        if (progress.attempts > 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("وضعك الحالي", fontWeight = FontWeight.Bold)
                        EvidenceProgress("${progress.statusAr()} · ${progress.attempts} محاولة", progress.mastery(), progress.confidence())
                    }
                }
            }
        }
        item { LessonBlock("الفكرة", skill.explanationAr) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("التركيب", color = Color(0xFFD0D5DD), fontSize = 12.sp)
                    Text(skill.formula, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("أمثلة", fontWeight = FontWeight.Black, color = Navy)
                    skill.examples.forEach { example ->
                        Text("• $example", fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftOrange), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("خطأ شائع", fontWeight = FontWeight.Bold)
                    Text(skill.commonMistakeAr, color = Muted)
                }
            }
        }
        item {
            Button(onClick = practice, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text("ابدأ 10 تمارين على هذه المهارة")
            }
        }
    }
}

@Composable
private fun SkillPracticeScreen(
    vm: EnglishCoachViewModel,
    learner: LearnerState,
    skill: SkillDefinition,
    back: () -> Unit,
    reviewLesson: () -> Unit,
) {
    val startProgress = remember(skill.id) { vm.skillProgress(skill.id) }
    var question by remember(skill.id) { mutableStateOf(ExerciseFactory.next(skill.id)) }
    var selected by remember(skill.id) { mutableStateOf<Int?>(null) }
    var number by remember(skill.id) { mutableIntStateOf(1) }
    var sessionScore by remember(skill.id) { mutableIntStateOf(0) }
    var finished by remember(skill.id) { mutableStateOf(false) }
    val liveProgress = learner.skillProgress[skill.id] ?: SkillProgress()

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        PageHeader(skill.titleAr, back)

        if (finished) {
            PracticeFinished(
                skill = skill,
                score = sessionScore,
                start = startProgress,
                end = liveProgress,
                reviewLesson = reviewLesson,
                again = {
                    number = 1
                    sessionScore = 0
                    selected = null
                    finished = false
                    question = ExerciseFactory.next(skill.id)
                },
            )
            return@Column
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$number / 10", color = Muted)
            Text("${skill.level} · ${liveProgress.statusAr()}", color = Muted)
        }
        LinearProgressIndicator(
            progress = { (number - 1) / 10f },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            color = Green,
        )

        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(question.prompt, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                Spacer(Modifier.height(18.dp))
                question.options.forEachIndexed { index, option ->
                    val isCorrect = index == question.correctIndex
                    val bg = when {
                        selected == null -> Color(0xFFF2F4F7)
                        index == selected && isCorrect -> SoftGreen
                        index == selected -> Color(0xFFFFEDEE)
                        isCorrect -> SoftGreen
                        else -> Color(0xFFF2F4F7)
                    }
                    Button(
                        onClick = {
                            if (selected == null) {
                                selected = index
                                val correct = isCorrect
                                if (correct) sessionScore++
                                vm.recordLearningAttempt(question, correct)
                            }
                        },
                        enabled = selected == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = bg,
                            contentColor = Navy,
                            disabledContainerColor = bg,
                            disabledContentColor = Navy,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) { Text(option, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }
                }

                if (selected != null) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    val correct = selected == question.correctIndex
                    Text(if (correct) "✓ ممتاز" else "الصحيح: ${question.options[question.correctIndex]}", fontWeight = FontWeight.Black, color = if (correct) Green else Color(0xFFD92D20))
                    Text(question.explanationAr, color = Muted, modifier = Modifier.padding(top = 5.dp))
                    Button(
                        onClick = {
                            if (number >= 10) {
                                finished = true
                                vm.finishStudySession()
                            } else {
                                number++
                                question = ExerciseFactory.next(skill.id, question.prompt)
                                selected = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    ) { Text(if (number == 10) "شوف التقييم الحقيقي" else "السؤال التالي") }
                }
            }
        }
    }
}

@Composable
private fun PracticeFinished(
    skill: SkillDefinition,
    score: Int,
    start: SkillProgress,
    end: SkillProgress,
    reviewLesson: () -> Unit,
    again: () -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("انتهت الجلسة", color = Color(0xFFD0D5DD))
                    Text("$score / 10", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Text(skill.titleAr, color = Color.White)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("ماذا تغير فعلًا؟", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    MetricRow("الإتقان", "${start.mastery()}%", "${end.mastery()}%")
                    MetricRow("الثقة بالتقييم", "${start.confidence()}%", "${end.confidence()}%")
                    Text("الحالة: ${end.statusAr()}", color = Muted, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item {
            val advice = when {
                end.mastery() < 55 -> "راجع الشرح ثم أعد جلسة قصيرة؛ الأساس ما زال غير ثابت."
                end.confidence() < 55 -> "نتيجتك جيدة، لكن نحتاج محاولات أكثر في يوم آخر لرفع الثقة بالتقييم."
                end.mastery() < 78 -> "أنت على الطريق الصحيح. راجع الأخطاء ثم أعد التدريب لاحقًا."
                else -> "المهارة قوية حاليًا. اتركها للمراجعة المجدولة وانتقل للمهارة التالية."
            }
            Card(colors = CardDefaults.cardColors(containerColor = SoftGreen), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("توصية SayIt", fontWeight = FontWeight.Bold)
                    Text(advice, color = Muted)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = reviewLesson, modifier = Modifier.weight(1f)) { Text("راجع الشرح") }
                Button(onClick = again, modifier = Modifier.weight(1f)) { Text("جلسة ثانية") }
            }
        }
    }
}

@Composable
private fun ReviewScreen(vm: EnglishCoachViewModel, learner: LearnerState, practice: (String) -> Unit) {
    val now = System.currentTimeMillis()
    val due = Curriculum.skills.filter { learner.skillProgress[it.id]?.isDue(now) == true }
    val weak = vm.topWeakSkills()
    val errors = vm.topErrors()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("المراجعة الذكية", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Navy)
            Text("مش أسئلة عشوائية؛ هذه الأشياء حان موعدها أو ظهرت كنقاط ضعف.", color = Muted)
        }
        item { SectionTitle("مستحق الآن", "${due.size} مهارة") }
        if (due.isEmpty()) {
            item { EmptyCard("✓ لا توجد مراجعات مستحقة الآن", "التطبيق سيعيد المهارات في الوقت المناسب بدل تكرارها بلا سبب.") }
        } else {
            items(due.take(8)) { skill ->
                val p = learner.skillProgress[skill.id] ?: SkillProgress()
                SkillReviewCard(skill, p, "حان موعد المراجعة") { practice(skill.id) }
            }
        }

        item { SectionTitle("نقاط الضعف", "مبنية على 3 محاولات أو أكثر") }
        if (weak.isEmpty()) {
            item { EmptyCard("لا توجد نقطة ضعف مؤكدة بعد", "إما أداؤك جيد أو ما زلنا نحتاج بيانات أكثر.") }
        } else {
            items(weak) { skill ->
                val p = learner.skillProgress[skill.id] ?: SkillProgress()
                SkillReviewCard(skill, p, "الإتقان ${p.mastery()}%") { practice(skill.id) }
            }
        }

        if (errors.isNotEmpty()) {
            item { SectionTitle("دفتر الأخطاء", "الأنماط التي تكررت معك") }
            items(errors) { (tag, count) ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Curriculum.errorLabel(tag), fontWeight = FontWeight.Bold)
                        Text("تكرر $count×", color = Color(0xFFD92D20))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(vm: EnglishCoachViewModel, learner: LearnerState, openLesson: (String) -> Unit) {
    val level = vm.estimateLevel()
    val weak = vm.topWeakSkills(4)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("تقدمي", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Navy)
            Text("لا توجد نسب موكاب: كل شيء هنا يأتي من سجل إجاباتك.", color = Muted)
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Estimated CEFR", color = Color(0xFFD0D5DD), fontSize = 12.sp)
                    Text(level.label, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(level.detailAr, color = Color(0xFFE4E7EC))
                    Text("Evidence: ${level.evidenceAttempts} attempts", color = Color(0xFFAAB3C0), fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }

        if (weak.isNotEmpty()) {
            item { SectionTitle("أولويات التحسين", "ابدأ من الأعلى") }
            items(weak) { skill ->
                val p = learner.skillProgress[skill.id] ?: SkillProgress()
                SkillReviewCard(skill, p, "${p.statusAr()} · ثقة ${p.confidence()}%") { openLesson(skill.id) }
            }
        }

        item { SectionTitle("خريطة المهارات", "Mastery + Confidence لكل وحدة") }
        items(Curriculum.units) { unit ->
            val (mastery, confidence) = vm.unitStats(unit.id)
            val assessed = vm.assessedSkills(unit.id)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp)) {
                    Text(unit.titleAr, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("${unit.level} · $assessed/${unit.skillIds.size} مهارات مقاسة", color = Muted, fontSize = 12.sp)
                    if (assessed > 0) EvidenceProgress("الوحدة", mastery, confidence)
                    Spacer(Modifier.height(10.dp))
                    Curriculum.skillsForUnit(unit.id).forEach { skill ->
                        val p = learner.skillProgress[skill.id] ?: SkillProgress()
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(skill.titleAr, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(if (p.attempts == 0) "غير مقيّمة" else "${p.mastery()}% · ثقة ${p.confidence()}%", color = Muted, fontSize = 11.sp)
                            }
                            TextButton(onClick = { openLesson(skill.id) }) { Text("فتح") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabScreen(back: () -> Unit, writing: () -> Unit, improve: () -> Unit, speaking: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("مختبر المهارات", back) }
        item { Text("هنا تطبّق الإنجليزي بحرية، بعيدًا عن أسئلة الاختيار.", color = Muted) }
        item { ToolCard("✍️", "Writing Coach", "مهمة كتابة مرتبطة بما تتعلمه + تصحيح مختصر") { writing() } }
        item { ToolCard("✨", "Improve my sentence", "ارفع جودة نفس الجملة بدل استبدالها بنص غريب") { improve() } }
        item { ToolCard("🗣️", "Speaking", "كرر جملة أو تحدث وسجّل دليلًا فعليًا على الأداء") { speaking() } }
    }
}

@Composable
private fun WritingScreen(vm: EnglishCoachViewModel, learner: LearnerState, back: () -> Unit) {
    val feedback by vm.writingFeedback.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val activeUnit = Curriculum.unit(learner.activeUnitId)
    val targetSkill = Curriculum.skillsForUnit(activeUnit.id).firstOrNull { it.id == "pc.form" }
        ?: Curriculum.skillsForUnit(activeUnit.id).first()
    val prompt = when (activeUnit.id) {
        "present_continuous" -> "اكتب 3 جمل بالإنجليزية تصف ما يحدث حولك الآن. حاول استخدام am/is/are + ing في كل جملة."
        "present_simple" -> "اكتب 3 جمل عن روتينك اليومي بالإنجليزية."
        "past_simple" -> "اكتب 3 جمل عن شيء فعلته أمس."
        "articles" -> "اكتب 3 جمل تصف أشياء حولك، وحاول استخدام a/an/the بشكل صحيح."
        else -> "اكتب 3–5 جمل بالإنجليزية عن يومك."
    }
    var text by remember { mutableStateOf("") }
    var lastRecordedEvidence by remember { mutableStateOf("") }
    val production = if (feedback != null) productionScore(activeUnit.id, text) else null

    LaunchedEffect(feedback, text, activeUnit.id) {
        val fb = feedback
        val score = production
        if (fb != null && score != null) {
            val key = "${activeUnit.id}|${text.trim()}|${fb.corrected}"
            if (key != lastRecordedEvidence) {
                vm.recordSkillScore(targetSkill.id, score, if (score < 70) "production" else null)
                lastRecordedEvidence = key
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PageHeader("Writing Coach", back) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("مهمة مرتبطة بوحدتك: ${activeUnit.titleAr}", fontWeight = FontWeight.Bold)
                    Text(prompt, color = Muted, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; vm.clearWritingFeedback() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                label = { Text("اكتب بالإنجليزية") },
            )
        }
        item {
            Button(
                onClick = { vm.reviewWriting(text) },
                enabled = text.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "جاري التصحيح…" else "راجع كتابتي") }
        }

        feedback?.let { fb ->
            production?.let { score ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = SoftGreen), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("استخدام مهارة الوحدة", fontWeight = FontWeight.Bold)
                            Text("$score%", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Green)
                            Text("هذا الدليل يُضاف لخريطة مهاراتك، وليس مجرد Score على الشاشة.", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
            item { WritingFeedbackCard(fb) }
        }
    }
}

private fun productionScore(unitId: String, text: String): Int? {
    if (text.isBlank()) return null
    val sentences = text.split(Regex("[.!?]+" )).map { it.trim() }.filter { it.isNotBlank() }
    if (sentences.isEmpty()) return null
    return when (unitId) {
        "present_continuous" -> {
            val correct = sentences.count { Regex("\\b(am|is|are)\\s+(not\\s+)?[A-Za-z]+ing\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) }
            ((correct.toDouble() / 3.0) * 100).toInt().coerceIn(0, 100)
        }
        "past_simple" -> {
            val markers = Regex("\\b(yesterday|last|ago|went|saw|had|made|took|worked|studied|called|finished)\\b", RegexOption.IGNORE_CASE)
            val correct = sentences.count { markers.containsMatchIn(it) }
            ((correct.toDouble() / 3.0) * 100).toInt().coerceIn(0, 100)
        }
        else -> null
    }
}

@Composable
private fun WritingFeedbackCard(fb: WritingFeedback) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("التصحيح", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(fb.corrected, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
            if (fb.issues.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                fb.issues.forEach { issue ->
                    Text(issue.title, fontWeight = FontWeight.Bold)
                    Text("${issue.before} → ${issue.after}", color = Color(0xFFD92D20))
                    Text(issue.explanationAr, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("صياغة طبيعية", fontWeight = FontWeight.Bold)
            Text(fb.better, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun ImproveScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val feedback by vm.improveFeedback.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Improve my sentence", back) }
        item { Text("اكتب جملة واحدة. سنحافظ على معناها ونريك كيف تصبح طبيعية وأقوى.", color = Muted) }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; vm.clearImproveFeedback() },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Your sentence") },
            )
        }
        item {
            Button(onClick = { vm.improveSentence(text) }, enabled = text.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "جاري التحليل…" else "حسّن الجملة")
            }
        }
        feedback?.let { fb -> item { ImproveFeedbackCard(fb) } }
    }
}

@Composable
private fun ImproveFeedbackCard(fb: ImproveFeedback) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UpgradeStep("1", "صحيحة", fb.corrected)
            UpgradeStep("2", "طبيعية", fb.natural)
            UpgradeStep("3", "مفردات أقوى", fb.stronger)
            if (fb.upgrades.isNotEmpty()) {
                HorizontalDivider()
                fb.upgrades.forEach { u ->
                    Text("${u.simple} → ${u.stronger}", fontWeight = FontWeight.Bold)
                    Text(u.meaningAr, color = Muted, fontSize = 12.sp)
                }
            }
            Text(fb.tipAr, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun UpgradeStep(number: String, label: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(30.dp).background(SoftGreen, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text(number, color = Green, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = Muted, fontSize = 12.sp)
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SpeakingScreen(vm: EnglishCoachViewModel, learner: LearnerState, back: () -> Unit) {
    val context = LocalContext.current
    val status by vm.modelStatus.collectAsStateWithLifecycle()
    val transcript by vm.transcript.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val activeUnit = Curriculum.unit(learner.activeUnitId)
    val targetSkill = Curriculum.skillsForUnit(activeUnit.id).first()
    val target = when (activeUnit.id) {
        "present_continuous" -> "I am practicing English right now."
        "present_simple" -> "I practice English every day."
        "past_simple" -> "I practiced English yesterday."
        else -> "I want to improve my English every day."
    }
    val tts = rememberTts()
    val recorder = remember { WavRecorder() }
    val audioFile = remember { File(context.cacheDir, "sayit-speaking.wav") }
    var recording by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            vm.clearTranscript()
            recorder.start(audioFile)
            recording = true
        }
    }

    fun startRecording() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            vm.clearTranscript()
            recorder.start(audioFile)
            recording = true
        } else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun stopRecording() {
        recorder.stop()
        recording = false
        vm.transcribeAndScore(audioFile, target, targetSkill.id)
    }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Speaking", back) }
        if (!status.speechReady) {
            item { EmptyCard("موديل الصوت غير موجود", "ارجع للرئيسية ونزّل Whisper Tiny مرة واحدة، وبعدها يعمل هذا القسم بدون إنترنت.") }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("كرر الجملة", color = Muted, fontSize = 12.sp)
                        Text(target, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy)
                        OutlinedButton(onClick = { tts?.speak(target, TextToSpeech.QUEUE_FLUSH, null, "target") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Text("🔊 اسمعها")
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { if (recording) stopRecording() else startRecording() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (recording) Color(0xFFD92D20) else Navy),
                ) { Text(if (recording) "■ أوقف التسجيل" else "● ابدأ التسجيل") }
            }
            if (busy) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            if (transcript.isNotBlank()) {
                item {
                    val score = similarityPercent(target, transcript)
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("ما سمعه التطبيق", color = Muted, fontSize = 12.sp)
                            Text(transcript, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("مطابقة الكلمات $score%", fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (score >= 75) Green else Color(0xFFD92D20))
                            Text("هذه ليست درجة نطق phoneme كاملة؛ هي دليل على وضوح الجملة والتقاط الكلمات.", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberTts(): TextToSpeech? {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine.language = Locale.US
                tts = engine
            }
        }
        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }
    return tts
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun PageHeader(title: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = back) { Text("‹ رجوع") }
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.Black, color = Navy)
    }
}

@Composable
private fun EvidenceProgress(label: String, mastery: Int, confidence: Int, dark: Boolean = false) {
    val track = if (dark) Color(0xFF344054) else Color(0xFFE4E7EC)
    val text = if (dark) Color.White else Navy
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = if (dark) Color(0xFFD0D5DD) else Muted, fontSize = 11.sp)
            Text("$mastery%", color = text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        LinearProgressIndicator(
            progress = { mastery.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            color = Green,
            trackColor = track,
        )
        Text("ثقة القياس $confidence%", color = if (dark) Color(0xFFAAB3C0) else Muted, fontSize = 10.sp)
    }
}

@Composable
private fun StatusPill(label: String, p: SkillProgress) {
    val bg = when {
        p.attempts == 0 -> Color(0xFFF2F4F7)
        p.mastery() >= 88 && p.confidence() >= 72 -> SoftGreen
        p.mastery() >= 70 -> SoftBlue
        else -> SoftOrange
    }
    Box(Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy)
    }
}

@Composable
private fun SkillReviewCard(skill: SkillDefinition, p: SkillProgress, reason: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(skill.titleAr, fontWeight = FontWeight.Bold, color = Navy)
                Text(reason, color = Muted, fontSize = 12.sp)
                Text("إتقان ${p.mastery()}% · ثقة ${p.confidence()}%", color = Green, fontSize = 11.sp)
            }
            Text("تدرّب ›", color = Navy, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LessonBlock(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Black, color = Navy, fontSize = 18.sp)
            Text(body, color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Navy)
            Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun ToolCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 27.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Navy)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            Text("›", color = Muted, fontSize = 26.sp)
        }
    }
}

@Composable
private fun MetricRow(label: String, before: String, after: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted)
        Text("$before  →  $after", fontWeight = FontWeight.Bold, color = Navy)
    }
}
