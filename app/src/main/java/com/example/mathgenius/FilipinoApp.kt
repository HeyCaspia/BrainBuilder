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

data class FilipinoLevel(
    val id: String,
    val name: String,
    val topics: List<FilipinoTopic>
)

data class FilipinoTopic(
    val id: String,
    val name: String,
    val questions: List<FilipinoQuestion>
)

data class FilipinoQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)

sealed class FilipinoScreen {
    object LevelSelection : FilipinoScreen()
    data class TopicSelection(val level: FilipinoLevel) : FilipinoScreen()
    data class QuizScreen(val topic: FilipinoTopic) : FilipinoScreen()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FilipinoApp(userName: String, onExit: () -> Unit, onSaveScore: (ScoreRecord) -> Unit) {
    var currentScreen by remember { mutableStateOf<FilipinoScreen>(FilipinoScreen.LevelSelection) }
    
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
                title = { Text("FILIPINO FOR ALL", fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentScreen) {
                            is FilipinoScreen.LevelSelection -> onExit()
                            is FilipinoScreen.TopicSelection -> currentScreen = FilipinoScreen.LevelSelection
                            is FilipinoScreen.QuizScreen -> {
                                val topic = (currentScreen as FilipinoScreen.QuizScreen).topic
                                val level = filipinoLevels.find { it.topics.any { t -> t.id == topic.id } } ?: filipinoLevels.first()
                                currentScreen = FilipinoScreen.TopicSelection(level)
                            }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFF5722),
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
                label = "FilipinoTransition"
            ) { screen ->
                when (screen) {
                    is FilipinoScreen.LevelSelection -> {
                        FilipinoLevelSelectionScreen(userName) { currentScreen = FilipinoScreen.TopicSelection(it) }
                    }
                    is FilipinoScreen.TopicSelection -> {
                        BackHandler { currentScreen = FilipinoScreen.LevelSelection }
                        FilipinoTopicSelectionScreen(screen.level) { currentScreen = FilipinoScreen.QuizScreen(it) }
                    }
                    is FilipinoScreen.QuizScreen -> {
                        val backLevel = filipinoLevels.find { it.topics.any { t -> t.id == screen.topic.id } } ?: filipinoLevels.first()
                        BackHandler { currentScreen = FilipinoScreen.TopicSelection(backLevel) }
                        FilipinoQuizApp(
                            userName = userName,
                            topic = screen.topic,
                            onSpeak = { speak(it) },
                            onBack = { currentScreen = FilipinoScreen.TopicSelection(backLevel) },
                            onSaveScore = onSaveScore
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilipinoLevelSelectionScreen(name: String, onLevelSelected: (FilipinoLevel) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Maligayang Pagdating, $name", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Pumili ng antas:", modifier = Modifier.padding(bottom = 32.dp))
        filipinoLevels.forEach { level ->
            Button(
                onClick = { onLevelSelected(level) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
            ) {
                Text(level.name, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun FilipinoTopicSelectionScreen(level: FilipinoLevel, onTopicSelected: (FilipinoTopic) -> Unit) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBE9E7), contentColor = Color(0xFFD84315))
                ) {
                    Text(topic.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilipinoQuizApp(
    userName: String,
    topic: FilipinoTopic,
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
            onSaveScore(ScoreRecord(userName, "FILIPINO", topic.name, score, questions.size, sdf.format(Date())))
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
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFBE9E7)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mag-aaral: $userName", fontWeight = FontWeight.Bold)
                        Text("Paksa: ${topic.name}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onSpeak(q.question) }) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Pakinggan", tint = Color(0xFFD84315))
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
                    val color = if (isSelected) Color(0xFFFF5722) else Color.White

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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
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

val filipinoLevels = listOf(
    FilipinoLevel(
        id = "grade6_filipino",
        name = "Grade 6 🇵🇭",
        topics = listOf(
            FilipinoTopic(
                id = "summative_test_g6_filipino",
                name = "First Summative Test 📝",
                questions = listOf(
                    FilipinoQuestion("1. Ano ang denotasyon?", listOf("Tuwiran o literal na kahulugan ng salita", "Pahiwatig na kahulugan", "Damdamin ng tauhan", "Ayos ng pangyayari"), "Tuwiran o literal na kahulugan ng salita"),
                    FilipinoQuestion("2. Ano ang konotasyon ng “may pusong bato”?", listOf("May pusong yari sa bato", "Walang awa o matigas ang damdamin", "Mahusay tumakbo", "Mahilig sa bundok"), "Walang awa o matigas ang damdamin"),
                    FilipinoQuestion("3. Aling panandang konteksto ang nagbibigay ng pormal na depinisyon?", listOf("Masaya ang paglalakbay.", "Parang ibon ang bata.", "Ang migrasyon ay paglipat ng tao mula sa isang lugar patungo sa iba.", "Umalis sila kahapon."), "Ang migrasyon ay paglipat ng tao mula sa isang lugar patungo sa iba."),
                    FilipinoQuestion("4. Ano ang salawikain?", listOf("Listahan ng pangalan", "Ulat ng panahon", "Maikling panuto", "Kasabihang naglalaman ng aral at karunungan"), "Kasabihang naglalaman ng aral at karunungan"),
                    FilipinoQuestion("5. Ano ang ibig sabihin ng \"Kung may tiyaga, may nilaga\"?", listOf("Dapat laging magluto", "Madali ang lahat", "May bunga ang pagsisikap", "Hindi kailangan ang sipag"), "May bunga ang pagsisikap"),
                    FilipinoQuestion("6. Alin ang nagpapakita ng angkop na diksyon sa pormal na liham?", listOf("Uy, gawin n'yo ito!", "Iminumungkahi po naming magsagawa ng programang pagbasa.", "Astig ang plano namin.", "Bahala na kayo riyan."), "Iminumungkahi po naming magsagawa ng programang pagbasa."),
                    FilipinoQuestion("7. Aling pangungusap ang walang paksa?", listOf("Umuulan nang malakas.", "Ang mga bata ay nagbabasa.", "Masipag si Lino.", "Naglinis ang klase."), "Umuulan nang malakas."),
                    FilipinoQuestion("8. Sa pangungusap na “Ipinagluto ni Ana ng lugaw ang lola,” ano ang kaganapang tagatanggap?", listOf("ni Ana", "ng lugaw", "ipinagluto", "ang lola"), "ang lola"),
                    FilipinoQuestion("9. Ano ang mahalagang pangyayari sa naratibo?", listOf("Pangyayaring may malaking ambag sa pag-unlad ng banghay", "Anumang salitang mahaba", "Pamagat lamang", "Bilang ng talata"), "Pangyayaring may malaking ambag sa pag-unlad ng banghay"),
                    FilipinoQuestion("10. Paano nahihinuha ang damdamin ng tauhan?", listOf("Sa kulay ng pahina lamang", "Sa haba ng pangalan", "Sa kaniyang kilos, pahayag, at reaksiyon sa pangyayari", "Sa bilang ng tauhan"), "Sa kaniyang kilos, pahayag, at reaksiyon sa pangyayari"),
                    FilipinoQuestion("11. Ano ang sanhi sa pahayag na \"Dahil malakas ang ulan, bumaha sa kalsada\"?", listOf("bumaha sa kalsada", "ang kalsada", "dahil", "malakas ang ulan"), "malakas ang ulan"),
                    FilipinoQuestion("12. Ano ang mahusay na buod?", listOf("Kopya ng buong teksto", "Maikli ngunit taglay ang mahahalagang pangyayari at ideya", "Listahan ng lahat ng salita", "Sariling opinyon lamang"), "Maikli ngunit taglay ang mahahalagang pangyayari at ideya"),
                    FilipinoQuestion("13. Ano ang pangunahing layon ng rebyu?", listOf("Suriin at tasahin ang isang akda o produkto", "Maglista ng tauhan lamang", "Magsalaysay nang walang pagsusuri", "Magbigay ng panuto sa laro"), "Suriin at tasahin ang isang akda o produkto"),
                    FilipinoQuestion("14. Aling detalye ang sumusuporta sa pangunahing ideya tungkol sa pangangalaga ng dagat?", listOf("Maraming kulay ang bangka.", "Mahaba ang pangalan ng dagat.", "Ang pagbabawas ng plastik ay nakatutulong sa yamang-dagat.", "Maaga nagising ang mangingisda."), "Ang pagbabawas ng plastik ay nakatutulong sa yamang-dagat."),
                    FilipinoQuestion("15. Aling pahayag ang malinaw na opinyon?", listOf("Nagsimula ang programa noong Lunes.", "Sa palagay ko, epektibo ang programang ito.", "Dalawampung bata ang dumalo.", "Tatlong aklat ang ginamit."), "Sa palagay ko, epektibo ang programang ito."),
                    FilipinoQuestion("16. Ano ang layon ng tekstong nanghihikayat?", listOf("Magbigay lamang ng petsa", "Maglarawan ng tauhan lamang", "Magtala ng pangalan", "Hikayatin ang mambabasa na maniwala o kumilos"), "Hikayatin ang mambabasa na maniwala o kumilos"),
                    FilipinoQuestion("17. Aling panandang nag-uugnay ang nagpapakita ng bunga?", listOf("kaya", "samantala", "halimbawa", "una"), "kaya"),
                    FilipinoQuestion("18. Ano ang mahalagang bahagi ng liham ng pagmumungkahi?", listOf("Pananakot sa tatanggap", "Impormasyong walang kaugnayan", "Di-pormal na biro lamang", "Malinaw na mungkahi at makatuwirang dahilan"), "Malinaw na mungkahi at makatuwirang dahilan"),
                    FilipinoQuestion("19. Paano susuriin ang pagkamakatotohanan ng tekstong multimedia?", listOf("Bilangin lamang ang larawan", "Paniwalaan ang pinakamaraming like", "Suriin ang sanggunian, petsa, ebidensiya, pagkiling, at pananaw ng eksperto", "Tingnan lamang ang kulay"), "Suriin ang sanggunian, petsa, ebidensiya, pagkiling, at pananaw ng eksperto"),
                    FilipinoQuestion("20. Aling tugon ang pinakamapanuri?", listOf("Totoo ito dahil maganda ang larawan.", "Makatotohanan ang pahayag dahil tugma ito sa datos mula sa dalawang mapanaligang sanggunian.", "Tama ito dahil mahaba ang post.", "Kapani-paniwala ito dahil sikat ang nagbahagi."), "Makatotohanan ang pahayag dahil tugma ito sa datos mula sa dalawang mapanaligang sanggunian.")
                )
            )
        )
    )
)
