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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayit.offlineenglish.ai.ImproveFeedback
import com.sayit.offlineenglish.ai.WritingFeedback
import com.sayit.offlineenglish.audio.WavRecorder
import com.sayit.offlineenglish.data.GrammarEngine
import com.sayit.offlineenglish.data.ProgressState
import com.sayit.offlineenglish.data.SeedData
import com.sayit.offlineenglish.data.VocabularyItem
import com.sayit.offlineenglish.util.similarityPercent
import com.sayit.offlineenglish.util.spellingDiff
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: EnglishCoachViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SayItTheme { SayItApp(vm) } }
    }
}

enum class Screen { HOME, PRACTICE, PROGRESS, GRAMMAR, VOCAB, SPELLING, DICTATION, WRITING, IMPROVE, SPEAKING }

@Composable
private fun SayItTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Color(0xFF111827),
        onPrimary = Color.White,
        secondary = Color(0xFF4B5563),
        surface = Color.White,
        background = Color(0xFFF7F7F8),
        outline = Color(0xFFE5E7EB),
    )
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

@Composable
private fun SayItApp(vm: EnglishCoachViewModel) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val progress by vm.progress.collectAsStateWithLifecycle()
    val status by vm.modelStatus.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val modelDownloadProgress by vm.modelDownloadProgress.collectAsStateWithLifecycle()
    val modelDownloadLabel by vm.modelDownloadLabel.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (screen in listOf(Screen.HOME, Screen.PRACTICE, Screen.PROGRESS)) {
                NavigationBar(containerColor = Color.White) {
                    listOf(
                        Screen.HOME to "الرئيسية",
                        Screen.PRACTICE to "تدرّب",
                        Screen.PROGRESS to "تقدمي"
                    ).forEach { (s, label) ->
                        NavigationBarItem(
                            selected = screen == s,
                            onClick = { screen = s },
                            icon = { Text(if (s == Screen.HOME) "⌂" else if (s == Screen.PRACTICE) "✦" else "↗", fontSize = 20.sp) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    level = vm.currentLevel(),
                    xp = progress.xp,
                    streak = progress.streak,
                    speechReady = status.speechReady,
                    busy = busy,
                    downloadProgress = modelDownloadProgress,
                    downloadLabel = modelDownloadLabel,
                    onDownloadModels = vm::downloadModels,
                ) { screen = it }
                Screen.PRACTICE -> PracticeScreen { screen = it }
                Screen.PROGRESS -> ProgressScreen(vm)
                Screen.GRAMMAR -> GrammarScreen(vm) { screen = Screen.PRACTICE }
                Screen.VOCAB -> VocabularyScreen(vm) { screen = Screen.PRACTICE }
                Screen.SPELLING -> SpellingScreen(vm) { screen = Screen.PRACTICE }
                Screen.DICTATION -> DictationScreen(vm) { screen = Screen.PRACTICE }
                Screen.WRITING -> WritingScreen(vm) { screen = Screen.PRACTICE }
                Screen.IMPROVE -> ImproveScreen(vm) { screen = Screen.PRACTICE }
                Screen.SPEAKING -> SpeakingScreen(vm) { screen = Screen.PRACTICE }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    level: String,
    xp: Int,
    streak: Int,
    speechReady: Boolean,
    busy: Boolean,
    downloadProgress: Int,
    downloadLabel: String,
    onDownloadModels: () -> Unit,
    go: (Screen) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("SayIt English", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("تدريب تفاعلي سريع — يعمل محليًا", color = Color.Gray)
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("مستواك الحالي", color = Color(0xFFD1D5DB))
                    Text(level, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🔥 $streak يوم", color = Color.White)
                        Text("$xp XP", color = Color.White)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = if (speechReady) Color(0xFFEFFAF3) else Color(0xFFFFF7E8))) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (speechReady) "✓ كل المزايا جاهزة" else "🎙️ الصوت يحتاج تجهيز مرة واحدة", fontWeight = FontWeight.Bold)
                    Text(
                        if (speechReady)
                            "القواعد والكلمات والكتابة والصوت تعمل على الجهاز بدون اتصال."
                        else
                            "كل شيء يعمل الآن ما عدا تحويل الكلام إلى نص. نزّل Whisper Tiny مرة واحدة فقط (حوالي 75 MB)."
                    )
                    if (!speechReady) {
                        Spacer(Modifier.height(10.dp))
                        if (busy && downloadLabel.isNotBlank()) {
                            LinearProgressIndicator(
                                progress = { downloadProgress.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("$downloadLabel — $downloadProgress%", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                        Button(onClick = onDownloadModels, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(if (busy) "جاري التنزيل..." else "تنزيل موديل الصوت")
                        }
                    }
                }
            }
        }
        item { Text("ابدأ تدريبًا سريعًا", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item { FeatureCard("🧠", "Grammar", "جلسة 10 أسئلة تتغير حسب أخطائك") { go(Screen.GRAMMAR) } }
        item { FeatureCard("📚", "Vocabulary", "اختيار + سياق + استدعاء من الذاكرة") { go(Screen.VOCAB) } }
        item { FeatureCard("✍️", "Writing Coach", "تصحيح فوري ومختصر بدون هبد") { go(Screen.WRITING) } }
        item { FeatureCard("✨", "Improve", "صحّح الجملة ثم قوّي الكلمات") { go(Screen.IMPROVE) } }
    }
}

@Composable
private fun PracticeScreen(go: (Screen) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("اختار مهارة", fontSize = 28.sp, fontWeight = FontWeight.Black) }
        val modes = listOf(
            Triple(Screen.GRAMMAR, "🧠 قواعد", "أسئلة متولدة ومتغيرة + تركيز على نقاط ضعفك"),
            Triple(Screen.VOCAB, "📚 كلمات", "104 كلمة بتدريبات مختلفة بدل البطاقات الثابتة"),
            Triple(Screen.SPELLING, "🔤 إملاء", "اسمع كلمة عشوائية واكتبها"),
            Triple(Screen.DICTATION, "🎧 Dictation", "40 جملة متغيرة للسمع والكتابة"),
            Triple(Screen.WRITING, "✍️ كتابة", "تصحيح واضح + أهم أخطائك + نسخة طبيعية"),
            Triple(Screen.IMPROVE, "✨ حسّن جملتي", "تصحيح ثم تعبير طبيعي ثم كلمات أقوى"),
            Triple(Screen.SPEAKING, "🗣️ تحدث", "نطق الجملة أو كلام حر أوفلاين")
        )
        items(modes) { (s, title, desc) -> FeatureCard(title.substringBefore(" "), title.substringAfter(" "), desc) { go(s) } }
    }
}

@Composable
private fun FeatureCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
            Text("›", fontSize = 28.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun PageHeader(title: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = back) { Text("‹ رجوع") }
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GrammarScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    var question by remember { mutableStateOf(GrammarEngine.nextQuestion(progress.weaknessCounts)) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var number by remember { mutableIntStateOf(1) }
    var sessionCorrect by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Grammar", back)
        if (finished) {
            SessionFinishedCard(
                title = "خلصت جلسة القواعد",
                score = sessionCorrect,
                total = 10,
                onRestart = {
                    number = 1
                    sessionCorrect = 0
                    selected = null
                    finished = false
                    question = GrammarEngine.nextQuestion(progress.weaknessCounts)
                }
            )
            return@Column
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("سؤال $number من 10", color = Color.Gray)
            Text(question.skill, color = Color.Gray)
        }
        LinearProgressIndicator(progress = { number / 10f }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp)) {
                Text(question.prompt, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                question.options.forEachIndexed { i, option ->
                    val isCorrect = i == question.correctIndex
                    val bg = when {
                        selected == null -> Color(0xFFF3F4F6)
                        i == selected && isCorrect -> Color(0xFFE7F8ED)
                        i == selected -> Color(0xFFFFECEC)
                        isCorrect -> Color(0xFFE7F8ED)
                        else -> Color(0xFFF3F4F6)
                    }
                    Button(
                        onClick = {
                            if (selected == null) {
                                selected = i
                                val correct = i == question.correctIndex
                                if (correct) sessionCorrect++
                                vm.answerGrammar(correct, question.skill)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Color(0xFF111827)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) { Text(option, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }
                }
                if (selected != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(if (selected == question.correctIndex) "✓ ممتاز" else "الصحيح: ${question.options[question.correctIndex]}", fontWeight = FontWeight.Bold)
                    Text(question.explanationAr, color = Color.DarkGray)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (number >= 10) {
                                finished = true
                            } else {
                                number++
                                question = GrammarEngine.nextQuestion(progress.weaknessCounts, question.prompt)
                                selected = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (number == 10) "شوف نتيجة الجلسة" else "السؤال التالي") }
                }
            }
        }
    }
}

@Composable
private fun SessionFinishedCard(title: String, score: Int, total: Int, onRestart: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✓", fontSize = 44.sp)
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text("$score / $total", fontSize = 36.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 10.dp))
            Text(
                when {
                    score >= total * .9 -> "ممتاز. الجلسة الجاية رح تظل متنوعة وتعيد نقاط الضعف عند الحاجة."
                    score >= total * .7 -> "جيد جدًا. الأخطاء اللي ظهرت رح تاخذ وزن أكبر في الجلسات القادمة."
                    else -> "تمام. التطبيق سجّل نقاط الضعف ورح يرجّعها لك بصيغ مختلفة بدل نفس السؤال."
                },
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("جلسة جديدة") }
        }
    }
}

private fun nextVocabulary(progress: ProgressState, excludeId: Int? = null): VocabularyItem {
    val fresh = SeedData.vocabulary.filter { it.id !in progress.learnedWordIds && it.id != excludeId }
    val pool = if (fresh.isNotEmpty() && kotlin.random.Random.nextInt(100) < 70) fresh else SeedData.vocabulary.filter { it.id != excludeId }
    return (if (pool.isNotEmpty()) pool else SeedData.vocabulary).random()
}

@Composable
private fun VocabularyScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    var item by remember { mutableStateOf(nextVocabulary(progress)) }
    var round by remember { mutableIntStateOf(1) }
    var sessionCorrect by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var typed by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }
    val mode = (round - 1) % 3

    val options = remember(item.id, round) {
        when (mode) {
            0 -> (SeedData.vocabulary.filter { it.id != item.id }.shuffled().take(3).map { it.meaningAr } + item.meaningAr).distinct().shuffled()
            1 -> (SeedData.vocabulary.filter { it.id != item.id }.shuffled().take(3).map { it.word } + item.word).distinct().shuffled()
            else -> emptyList()
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Vocabulary", back)
        if (finished) {
            SessionFinishedCard("خلصت جلسة الكلمات", sessionCorrect, 10) {
                round = 1
                sessionCorrect = 0
                result = null
                selected = null
                typed = ""
                finished = false
                item = nextVocabulary(progress, item.id)
            }
            return@Column
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("كلمة $round من 10", color = Color.Gray)
            Text("${SeedData.vocabulary.size} كلمة في البنك", color = Color.Gray)
        }
        LinearProgressIndicator(progress = { round / 10f }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text(
                    when (mode) {
                        0 -> "اختار المعنى الصحيح"
                        1 -> "اختار الكلمة التي تكمل الجملة"
                        else -> "اكتب الكلمة من الذاكرة"
                    },
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))

                if (mode == 0) {
                    Text(item.word, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(16.dp))
                    options.forEach { option ->
                        ChoiceButton(option, selected, result, option == item.meaningAr) {
                            if (result == null) {
                                selected = option
                                val correct = option == item.meaningAr
                                result = correct
                                if (correct) sessionCorrect++
                                vm.answerVocabulary(correct, item.id)
                            }
                        }
                    }
                } else if (mode == 1) {
                    val blanked = remember(item.id) { Regex("\\b${Regex.escape(item.word)}\\b", RegexOption.IGNORE_CASE).replace(item.example, "___") }
                    Text(blanked, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    options.forEach { option ->
                        ChoiceButton(option, selected, result, option == item.word) {
                            if (result == null) {
                                selected = option
                                val correct = option == item.word
                                result = correct
                                if (correct) sessionCorrect++
                                vm.answerVocabulary(correct, item.id)
                            }
                        }
                    }
                } else {
                    Text(item.meaningAr, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("أول حرف: ${item.word.first().uppercaseChar()}  •  ${item.word.length} أحرف", color = Color.Gray)
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it; if (result != null) result = null },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        label = { Text("اكتب الكلمة بالإنجليزية") },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val correct = typed.trim().equals(item.word, ignoreCase = true)
                            result = correct
                            if (correct) sessionCorrect++
                            vm.answerVocabulary(correct, item.id)
                        },
                        enabled = typed.isNotBlank() && result == null,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) { Text("تحقق") }
                }

                result?.let { correct ->
                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(14.dp))
                    Text(if (correct) "✓ ممتاز" else "✕ الصحيح: ${item.word} — ${item.meaningAr}", fontWeight = FontWeight.Bold)
                    Text(item.example, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                    if (item.alternatives.isNotEmpty()) {
                        Text("بدائل: ${item.alternatives.joinToString(" • ")}", color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
                    }
                    Button(
                        onClick = {
                            if (round >= 10) {
                                finished = true
                            } else {
                                round++
                                item = nextVocabulary(progress, item.id)
                                result = null
                                selected = null
                                typed = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    ) { Text(if (round == 10) "شوف نتيجة الجلسة" else "تحدي جديد") }
                }
            }
        }
    }
}

@Composable
private fun ChoiceButton(label: String, selected: String?, result: Boolean?, correct: Boolean, onClick: () -> Unit) {
    val bg = when {
        result == null -> Color(0xFFF3F4F6)
        label == selected && correct -> Color(0xFFE7F8ED)
        label == selected -> Color(0xFFFFECEC)
        correct -> Color(0xFFE7F8ED)
        else -> Color(0xFFF3F4F6)
    }
    Button(
        onClick = onClick,
        enabled = result == null,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Color(0xFF111827), disabledContainerColor = bg, disabledContentColor = Color(0xFF111827)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }
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
private fun SpellingScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val tts = rememberTts()
    var item by remember { mutableStateOf(SeedData.vocabulary.random()) }
    var answer by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf<Boolean?>(null) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Spelling", back)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("اسمع الكلمة واكتبها", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("كل مرة بنختار من بنك ${SeedData.vocabulary.size} كلمة", color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { tts?.speak(item.word, TextToSpeech.QUEUE_FLUSH, null, "spell") }, modifier = Modifier.fillMaxWidth()) { Text("🔊 اسمع الكلمة") }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(answer, { answer = it; checked = null }, modifier = Modifier.fillMaxWidth(), label = { Text("اكتب هنا") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    val correct = answer.trim().equals(item.word, true)
                    checked = correct
                    vm.recordSpelling(correct)
                }, enabled = answer.isNotBlank() && checked == null, modifier = Modifier.fillMaxWidth()) { Text("تحقق") }
                checked?.let { correct ->
                    Spacer(Modifier.height(14.dp))
                    Text(if (correct) "✓ صحيح" else "✕ الصحيح: ${item.word}", fontWeight = FontWeight.Bold)
                    Text("${item.meaningAr} — ${item.example}")
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = {
                        val oldId = item.id
                        item = SeedData.vocabulary.filter { it.id != oldId }.random()
                        answer = ""
                        checked = null
                    }, modifier = Modifier.fillMaxWidth()) { Text("كلمة جديدة") }
                }
            }
        }
    }
}

@Composable
private fun DictationScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val tts = rememberTts()
    var item by remember { mutableStateOf(SeedData.dictation.random()) }
    var answer by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Dictation", back)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("اسمع الجملة ثم اكتب ما سمعت", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("المستوى ${item.level} • بنك ${SeedData.dictation.size} جملة", color = Color.Gray)
                Spacer(Modifier.height(14.dp))
                Button(onClick = { tts?.speak(item.text, TextToSpeech.QUEUE_FLUSH, null, "dictation") }, modifier = Modifier.fillMaxWidth()) { Text("▶ تشغيل الجملة") }
                OutlinedTextField(answer, { answer = it; result = null }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), minLines = 3, label = { Text("اكتب الجملة") })
                Button(onClick = {
                    val score = similarityPercent(item.text, answer)
                    result = score
                    vm.recordDictation(score)
                }, enabled = answer.isNotBlank() && result == null, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("تحقق") }
                result?.let { score ->
                    Spacer(Modifier.height(14.dp))
                    Text("النتيجة $score%", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    if (score < 100) {
                        Text("الصحيح:\n${item.text}", fontWeight = FontWeight.Bold)
                        Text(spellingDiff(item.text, answer), color = Color.DarkGray)
                    }
                    OutlinedButton(onClick = {
                        val oldId = item.id
                        item = SeedData.dictation.filter { it.id != oldId }.random()
                        answer = ""
                        result = null
                    }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("جملة جديدة") }
                }
            }
        }
    }
}

@Composable
private fun WritingScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val feedback by vm.writingFeedback.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf(SeedData.writingPrompts.random()) }
    LaunchedEffect(Unit) { vm.clearWritingFeedback() }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Writing Coach", back) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF))) {
                Column(Modifier.padding(16.dp)) {
                    Text("اكتب عن:", color = Color.Gray)
                    Text(prompt, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        val old = prompt
                        prompt = SeedData.writingPrompts.filter { it != old }.random()
                        text = ""
                        vm.clearWritingFeedback()
                    }) { Text("غيّر الموضوع ↻") }
                }
            }
        }
        item {
            OutlinedTextField(
                text,
                { text = it; if (feedback != null) vm.clearWritingFeedback() },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                placeholder = { Text("Write 3–5 sentences in English...") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        }
        item {
            Button(
                onClick = { vm.reviewWriting(text) },
                enabled = text.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "ثانية..." else "راجع كتابتي") }
        }
        feedback?.let { f -> item { WritingFeedbackCard(f) } }
    }
}

@Composable
private fun WritingFeedbackCard(feedback: WritingFeedback) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("تقييم تقريبي", color = Color.Gray)
                    Text("${feedback.score}/100", fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                Text(if (feedback.score >= 85) "🔥" else if (feedback.score >= 70) "👍" else "✍️", fontSize = 34.sp)
            }
        }
        FeedbackTextCard("✓ التصحيح", feedback.corrected)
        if (feedback.issues.isNotEmpty()) {
            Text("أهم شيء تصلحه", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            feedback.issues.forEach { issue ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E8))) {
                    Column(Modifier.padding(14.dp)) {
                        Text(issue.title, fontWeight = FontWeight.Bold)
                        Text("${issue.before}  →  ${issue.after}", fontWeight = FontWeight.Bold)
                        Text(issue.explanationAr, color = Color.DarkGray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFFAF3))) {
                Text("✓ ما لقيت خطأ واضح من القواعد التي يتابعها التطبيق حاليًا.", modifier = Modifier.padding(14.dp))
            }
        }
        if (feedback.better != feedback.corrected) FeedbackTextCard("🌿 نسخة أكثر طبيعية", feedback.better)
    }
}

@Composable
private fun ImproveScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val feedback by vm.improveFeedback.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.clearImproveFeedback() }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Improve My Sentence", back) }
        item { Text("اكتب جملة. التطبيق أولًا يصححها، ثم يعطيك صياغة طبيعية، ثم يقترح كلمات أقوى بدون تغيير المعنى.") }
        item {
            OutlinedTextField(
                text,
                { text = it; if (feedback != null) vm.clearImproveFeedback() },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("I am very tired because I have a big problem at work.") }
            )
        }
        item {
            Button(onClick = { vm.improveSentence(text) }, enabled = text.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "ثانية..." else "حسّن الجملة ✨")
            }
        }
        feedback?.let { f -> item { ImproveFeedbackCard(f) } }
    }
}

@Composable
private fun ImproveFeedbackCard(feedback: ImproveFeedback) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FeedbackTextCard("1. صحيحة", feedback.corrected)
        if (feedback.natural != feedback.corrected) FeedbackTextCard("2. أكثر طبيعية", feedback.natural)
        if (feedback.stronger != feedback.natural) FeedbackTextCard("3. مفردات أقوى", feedback.stronger)
        if (feedback.upgrades.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF))) {
                Column(Modifier.padding(16.dp)) {
                    Text("بدائل تعلمها من الجملة", fontWeight = FontWeight.Bold)
                    feedback.upgrades.forEach { up ->
                        Text("${up.simple}  →  ${up.stronger}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Text(up.meaningAr, color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Text("💡 ${feedback.tipAr}", modifier = Modifier.padding(14.dp), color = Color.DarkGray)
        }
    }
}

@Composable
private fun FeedbackTextCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(body, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun SpeakingScreen(vm: EnglishCoachViewModel, back: () -> Unit) {
    val context = LocalContext.current
    val busy by vm.busy.collectAsStateWithLifecycle()
    val transcript by vm.transcript.collectAsStateWithLifecycle()
    val status by vm.modelStatus.collectAsStateWithLifecycle()
    val recorder = remember { WavRecorder() }
    val tts = rememberTts()
    var recording by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(true) }
    var freePrompt by remember { mutableStateOf(SeedData.speakingPrompts.random()) }
    var repeatSentence by remember { mutableStateOf(SeedData.repeatSentences.random()) }
    val target = if (repeatMode) repeatSentence else null
    val audioFile = remember { File(context.getExternalFilesDir("audio") ?: context.filesDir, "speech.wav") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recorder.start(audioFile)
            recording = true
        }
    }

    DisposableEffect(recorder) { onDispose { recorder.stop() } }
    LaunchedEffect(repeatMode) { vm.clearTranscript() }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PageHeader("Speaking", back)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (repeatMode) {
                Button(onClick = { repeatMode = true }, modifier = Modifier.weight(1f)) { Text("نطق الجملة") }
                OutlinedButton(onClick = { repeatMode = false }, modifier = Modifier.weight(1f)) { Text("كلام حر") }
            } else {
                OutlinedButton(onClick = { repeatMode = true }, modifier = Modifier.weight(1f)) { Text("نطق الجملة") }
                Button(onClick = { repeatMode = false }, modifier = Modifier.weight(1f)) { Text("كلام حر") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (repeatMode) {
                    Text("اسمع ثم كرر", color = Color.Gray)
                    Text(repeatSentence, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = { tts?.speak(repeatSentence, TextToSpeech.QUEUE_FLUSH, null, "repeat") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("🔊 اسمع الجملة") }
                } else {
                    Text("تحدث بالإنجليزية عن:", color = Color.Gray)
                    Text(freePrompt, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (!recording) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                recorder.start(audioFile)
                                recording = true
                            } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            recorder.stop()
                            recording = false
                            vm.transcribeAndScore(audioFile, target)
                        }
                    },
                    enabled = status.speechReady && !busy,
                    modifier = Modifier.size(150.dp),
                    shape = RoundedCornerShape(75.dp)
                ) { Text(if (recording) "■\nإيقاف" else "🎙️\nابدأ", textAlign = TextAlign.Center, fontSize = 18.sp) }

                if (!status.speechReady) Text("نزّل موديل الصوت من الرئيسية مرة واحدة.", color = Color(0xFFB45309), modifier = Modifier.padding(top = 12.dp))
                if (busy) CircularProgressIndicator(Modifier.padding(top = 14.dp))

                if (transcript.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    if (repeatMode) {
                        val score = similarityPercent(repeatSentence, transcript)
                        Text("وضوح الجملة: $score%", fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text("التطبيق سمع:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Text(transcript, fontSize = 18.sp, textAlign = TextAlign.Center)
                        if (score < 100) Text(spellingDiff(repeatSentence, transcript), color = Color.DarkGray, modifier = Modifier.padding(top = 6.dp))
                        OutlinedButton(onClick = {
                            val old = repeatSentence
                            repeatSentence = SeedData.repeatSentences.filter { it != old }.random()
                            vm.clearTranscript()
                        }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("جملة جديدة") }
                    } else {
                        Text("هذا ما فهمه التطبيق:", fontWeight = FontWeight.Bold)
                        Text(transcript, fontSize = 18.sp, textAlign = TextAlign.Center)
                        OutlinedButton(onClick = {
                            val old = freePrompt
                            freePrompt = SeedData.speakingPrompts.filter { it != old }.random()
                            vm.clearTranscript()
                        }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("موضوع جديد") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(vm: EnglishCoachViewModel) {
    val p by vm.progress.collectAsStateWithLifecycle()
    fun acc(correct: Int, total: Int) = if (total == 0) 0 else correct * 100 / total
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("تقدمي", fontSize = 28.sp, fontWeight = FontWeight.Black) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Level", vm.currentLevel(), true)
                    Stat("XP", p.xp.toString(), true)
                    Stat("Words", p.learnedWordIds.size.toString(), true)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallStat("Grammar", "${acc(p.grammarCorrect, p.grammarTotal)}%", Modifier.weight(1f))
                SmallStat("Vocabulary", "${acc(p.vocabularyCorrect, p.vocabularyTotal)}%", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallStat("Spelling", "${acc(p.spellingCorrect, p.spellingTotal)}%", Modifier.weight(1f))
                SmallStat("Dictation", "${acc(p.dictationCorrect, p.dictationTotal)}%", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallStat("Speaking", p.speakingAttempts.toString(), Modifier.weight(1f))
                SmallStat("Writing", p.writingAttempts.toString(), Modifier.weight(1f))
            }
        }
        item { Text("نقاط تحتاج تركيز", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        val weaknesses = vm.topWeaknesses()
        if (weaknesses.isEmpty()) item { Text("حل جلسة Grammar، وبعدها التطبيق يبدأ يحدد نقاط ضعفك.", color = Color.Gray) }
        items(weaknesses) { (skill, count) ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(skill, fontWeight = FontWeight.Bold)
                    Text("وزن مراجعة $count", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, dark: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (dark) Color.White else Color.Black)
        Text(label, fontSize = 12.sp, color = if (dark) Color.LightGray else Color.Gray)
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { Stat(label, value) }
    }
}
