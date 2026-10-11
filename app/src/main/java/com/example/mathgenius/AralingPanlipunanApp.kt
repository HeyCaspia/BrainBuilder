package com.example.mathgenius

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
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
import kotlinx.coroutines.delay
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

data class APLevel(
    val id: String,
    val name: String,
    val topics: List<APTopic>
)

data class APTopic(
    val id: String,
    val name: String,
    val questions: List<APQuestion>
)

data class APQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)

sealed class APScreen {
    object LevelSelection : APScreen()
    data class TopicSelection(val level: APLevel) : APScreen()
    data class QuizScreen(val topic: APTopic) : APScreen()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AralingPanlipunanApp(userName: String, onExit: () -> Unit, onSaveScore: (ScoreRecord) -> Unit) {
    var currentScreen by remember { mutableStateOf<APScreen>(APScreen.LevelSelection) }
    
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ARALING PANLIPUNAN", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentScreen) {
                            is APScreen.LevelSelection -> onExit()
                            is APScreen.TopicSelection -> currentScreen = APScreen.LevelSelection
                            is APScreen.QuizScreen -> {
                                val topic = (currentScreen as APScreen.QuizScreen).topic
                                val level = apLevels.find { it.topics.any { t -> t.id == topic.id } } ?: apLevels.first()
                                currentScreen = APScreen.TopicSelection(level)
                            }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE91E63),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() with fadeOut() },
                label = "APTransition"
            ) { screen ->
                when (screen) {
                    is APScreen.LevelSelection -> {
                        APLevelSelectionScreen(userName) { currentScreen = APScreen.TopicSelection(it) }
                    }
                    is APScreen.TopicSelection -> {
                        BackHandler { currentScreen = APScreen.LevelSelection }
                        APTopicSelectionScreen(screen.level) { currentScreen = APScreen.QuizScreen(it) }
                    }
                    is APScreen.QuizScreen -> {
                        val backLevel = apLevels.find { it.topics.any { t -> t.id == screen.topic.id } } ?: apLevels.first()
                        BackHandler { currentScreen = APScreen.TopicSelection(backLevel) }
                        APQuizApp(
                            userName = userName,
                            topic = screen.topic,
                            onSpeak = { speak(it) },
                            onBack = { currentScreen = APScreen.TopicSelection(backLevel) },
                            onSaveScore = onSaveScore
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun APLevelSelectionScreen(name: String, onLevelSelected: (APLevel) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Maligayang Pagdating, $name", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Pumili ng antas:", modifier = Modifier.padding(bottom = 32.dp))
        apLevels.forEach { level ->
            Button(
                onClick = { onLevelSelected(level) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text(level.name, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun APTopicSelectionScreen(level: APLevel, onTopicSelected: (APTopic) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(level.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Pumili ng paksa:", modifier = Modifier.padding(bottom = 32.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(level.topics) { topic ->
                Button(
                    onClick = { onTopicSelected(topic) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCE4EC), contentColor = Color(0xFFC2185B))
                ) {
                    Text(topic.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun APQuizApp(
    userName: String,
    topic: APTopic,
    onSpeak: (String) -> Unit = {},
    onBack: () -> Unit,
    onSaveScore: (ScoreRecord) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var timeSpentSeconds by remember { mutableStateOf(0) }
    val wrongAnswersList = remember { mutableStateListOf<WrongAnswer>() }

    val questions = remember(topic) { topic.questions }

    LaunchedEffect(showResult) {
        if (showResult) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            onSaveScore(ScoreRecord(userName, "ARALING PANLIPUNAN", topic.name, score, questions.size, sdf.format(Date())))
        }
    }

    LaunchedEffect(showResult) {
        if (!showResult) {
            while (true) {
                delay(1000)
                timeSpentSeconds++
            }
        }
    }

    if (showResult) {
        ResultsScreen(
            name = userName,
            score = score,
            total = questions.size,
            modeName = topic.name,
            mistakes = wrongAnswersList,
            timeSpent = timeSpentSeconds,
            onRestart = onBack
        )
    } else {
        val q = questions[currentIndex]
        Column {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFCE4EC)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mag-aaral: $userName", fontWeight = FontWeight.Bold)
                        Text("Paksa: ${topic.name}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onSpeak(q.question) }) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Pakinggan", tint = Color(0xFFC2185B))
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Tanong ${currentIndex + 1} sa ${questions.size}")

                val displayFontSize = if (q.question.length > 30) 18.sp else 22.sp
                Text(
                    text = q.question,
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = displayFontSize * 1.2f,
                    modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
                )

                q.options.forEach { option ->
                    val isSelected = selectedOption == option
                    val color = if (isSelected) Color(0xFFE91E63) else Color.White

                    OutlinedButton(
                        onClick = {
                            if (selectedOption == null) {
                                selectedOption = option
                                if (option == q.correctAnswer) {
                                    score++
                                } else {
                                    wrongAnswersList.add(WrongAnswer(q.question, option, q.correctAnswer))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = color,
                            contentColor = if (color == Color.White) Color.Black else Color.White
                        ),
                        enabled = selectedOption == null
                    ) {
                        Text(option, fontSize = 15.sp, textAlign = TextAlign.Center)
                    }
                }

                if (selectedOption != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (currentIndex < questions.size - 1) {
                                currentIndex++
                                selectedOption = null
                            } else {
                                showResult = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) {
                        Text(if (currentIndex < questions.size - 1) "SUSUNOD" else "TAPUSIN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(0.5f)) { Text("BUMALIK") }
            }
        }
    }
}

val apLevels = listOf(
    APLevel(
        id = "grade4_ap",
        name = "Grade 4 🏛️",
        topics = listOf(
            APTopic(
                id = "summative_test_g4_ap",
                name = "First Summative Test 📝",
                questions = listOf(
                    APQuestion("1. Ano ang pagkabansa?", listOf("Pagkakaisa ng mga taong may teritoryo, pamahalaan, at pagkakakilanlan", "Pagkakaroon lamang ng maraming gusali", "Pagdami lamang ng mamamayan", "Pagkakaroon ng iisang hanapbuhay"), "Pagkakaisa ng mga taong may teritoryo, pamahalaan, at pagkakakilanlan"),
                    APQuestion("2. Alin ang mahalagang elemento ng pagkabansa?", listOf("laruan", "teritoryo", "paligsahan", "pamilihan"), "teritoryo"),
                    APQuestion("3. Ano ang tumutukoy sa mga taong bumubuo sa bansa?", listOf("klima", "produkto", "mamamayan", "sasakyan"), "mamamayan"),
                    APQuestion("4. Aling elemento ang nagpapatupad ng batas?", listOf("wika lamang", "anyong lupa", "populasyon lamang", "pamahalaan"), "pamahalaan"),
                    APQuestion("5. Bakit mahalaga ang soberanya?", listOf("Pinahihina nito ang pamahalaan.", "Inaalis nito ang teritoryo.", "Nagbibigay ito ng kapangyarihang mamahala nang malaya.", "Pinipigil nito ang mamamayan."), "Nagbibigay ito ng kapangyarihang mamahala nang malaya."),
                    APQuestion("6. Ano ang nagtatakda sa sakop ng isang bansa?", listOf("tradisyon", "teritoryo", "hanapbuhay", "pista"), "teritoryo"),
                    APQuestion("7. Alin ang nagpapakita ng pagmamalasakit sa bansa?", listOf("Pagsunod sa makatarungang batas", "Pagsira sa pampublikong gamit", "Pagkalat ng maling balita", "Pag-iwas sa tungkulin"), "Pagsunod sa makatarungang batas"),
                    APQuestion("8. Ano ang tatlong pangunahing sangay ng pamahalaan?", listOf("Paaralan, ospital, at pamilihan", "Barangay, pamilya, at simbahan", "Luzon, Visayas, at Mindanao", "Ehekutibo, Lehislatibo, at Hudikatura"), "Ehekutibo, Lehislatibo, at Hudikatura"),
                    APQuestion("9. Aling sangay ang nagpapatupad ng batas?", listOf("Ehekutibo", "Lehislatibo", "Hudikatura", "Lokal na negosyo"), "Ehekutibo"),
                    APQuestion("10. Aling sangay ang gumagawa ng batas?", listOf("Ehekutibo", "Hudikatura", "Lehislatibo", "Paaralan"), "Lehislatibo"),
                    APQuestion("11. Aling sangay ang nagpapasya sa mga usaping legal?", listOf("Ehekutibo", "Lehislatibo", "Media", "Hudikatura"), "Hudikatura"),
                    APQuestion("12. Sino ang pinuno ng sangay ehekutibo ng bansa?", listOf("Punong Mahistrado", "Pangulo", "Senate President", "Punong Barangay"), "Pangulo"),
                    APQuestion("13. Ano ang pangunahing tungkulin ng Kongreso?", listOf("Gumawa ng mga batas", "Magpasya sa kaso", "Mamuno sa hukuman", "Magpatupad ng ordinansa sa klase"), "Gumawa ng mga batas"),
                    APQuestion("14. Bakit may magkakahiwalay na sangay?", listOf("Upang iisa ang magpasya", "Upang walang managot", "Upang magkaroon ng checks and balances", "Upang magkapareho ang tungkulin"), "Upang magkaroon ng checks and balances"),
                    APQuestion("15. Alin ang halimbawa ng checks and balances?", listOf("Gumagawa ng batas ang isang klase.", "Sinusuri ng Korte Suprema kung naaayon sa Konstitusyon ang batas.", "Nagpapasya ang isang tao para sa lahat.", "Walang sumusuri sa desisyon."), "Sinusuri ng Korte Suprema kung naaayon sa Konstitusyon ang batas."),
                    APQuestion("16. Kung may panukalang batas, aling sangay ang unang pangunahing kikilos dito?", listOf("Hudikatura", "Ehekutibo lamang", "Paaralan", "Lehislatibo"), "Lehislatibo"),
                    APQuestion("17. Bakit mahalaga ang mamamayan sa pagkabansa?", listOf("Sila ang bumubuo at nakikilahok sa bansa.", "Sila ang hangganan ng teritoryo.", "Sila ang anyong tubig.", "Sila ang gusali ng pamahalaan."), "Sila ang bumubuo at nakikilahok sa bansa."),
                    APQuestion("18. Ano ang maaaring mangyari kung walang malinaw na estruktura ng pamahalaan?", listOf("Lilinaw ang lahat ng gawain.", "Mawawala ang pangangailangan sa batas.", "Aayos ang lahat nang kusa.", "Maaaring magulo ang tungkulin at pananagutan."), "Maaaring magulo ang tungkulin at pananagutan."),
                    APQuestion("19. Aling pahayag ang tama?", listOf("Teritoryo lamang ang kailangan ng bansa.", "Iisa lamang ang sangay ng pamahalaan.", "Magkakaugnay ang mga elemento ng pagkabansa at pamahalaan.", "Hindi mahalaga ang mamamayan."), "Magkakaugnay ang mga elemento ng pagkabansa at pamahalaan."),
                    APQuestion("20. Paano makikiisa ang mag-aaral sa pamahalaan?", listOf("Manira ng pasilidad.", "Maging responsable, sumunod sa batas, at makilahok sa makabuluhang gawain.", "Magbahagi ng maling impormasyon.", "Iwasan ang lahat ng gawaing sibiko."), "Maging responsable, sumunod sa batas, at makilahok sa makabuluhang gawain.")
                )
            )
        )
    )
)
