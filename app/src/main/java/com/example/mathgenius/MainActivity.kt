package com.example.mathgenius

import android.os.Bundle
import android.media.MediaPlayer
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import kotlinx.coroutines.delay

enum class Grade(val label: String) { TODDLER("Toddler"), GRADE_1("Grade 1"), GRADE_3("Grade 3"), GRADE_4("Grade 4"), GRADE_6("Grade 6") }
enum class Topic(val label: String) { COUNTING("Counting"), ADDITION("Addition"), SUBTRACTION("Subtraction"), MULTIPLICATION("Multiplication"), DIVISION("Division"), FRACTIONS("Fractions"), PEMDAS("PEMDAS"), ALGEBRA("Algebra"), GEOMETRY("Geometry"), WORD_PROBLEMS("Word Problems") }
data class WrongAnswer(val question: String, val userAnswer: String, val correctAnswer: String)

data class ScoreRecord(
    val nickname: String,
    val subject: String,
    val category: String,
    val score: Int,
    val total: Int,
    val date: String
)

sealed class MainScreen {
    object Hub : MainScreen()
    object MathApp : MainScreen()
    object EnglishApp : MainScreen()
    object ScienceApp : MainScreen()
    object History : MainScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GeniusHub()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GeniusHub() {
    var currentScreen by remember { mutableStateOf<MainScreen>(MainScreen.Hub) }
    var globalName by remember { mutableStateOf("") }
    var hasEnteredName by remember { mutableStateOf(false) }
    val scoreHistory = remember { mutableStateListOf<ScoreRecord>() }

    fun saveScore(record: ScoreRecord) {
        scoreHistory.add(0, record) // Newest first
    }

    if (!hasEnteredName) {
        GlobalStartScreen(globalName, onNameChange = { globalName = it }, onStart = { hasEnteredName = true })
    } else {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() with fadeOut() },
            label = "MainTransition"
        ) { screen ->
            when (screen) {
                is MainScreen.Hub -> HubScreen(
                    userName = globalName,
                    onMathSelected = { currentScreen = MainScreen.MathApp },
                    onEnglishSelected = { currentScreen = MainScreen.EnglishApp },
                    onScienceSelected = { currentScreen = MainScreen.ScienceApp },
                    onHistorySelected = { currentScreen = MainScreen.History }
                )
                is MainScreen.MathApp -> {
                    BackHandler { currentScreen = MainScreen.Hub }
                    MathExamApp(
                        userName = globalName, 
                        onExit = { currentScreen = MainScreen.Hub },
                        onSaveScore = { saveScore(it) }
                    )
                }
                is MainScreen.EnglishApp -> {
                    BackHandler { currentScreen = MainScreen.Hub }
                    EnglishApp(
                        userName = globalName, 
                        onExit = { currentScreen = MainScreen.Hub },
                        onSaveScore = { saveScore(it) }
                    )
                }
                is MainScreen.ScienceApp -> {
                    BackHandler { currentScreen = MainScreen.Hub }
                    ScienceApp(
                        userName = globalName, 
                        onExit = { currentScreen = MainScreen.Hub },
                        onSaveScore = { saveScore(it) }
                    )
                }
                is MainScreen.History -> {
                    BackHandler { currentScreen = MainScreen.Hub }
                    HistoryScreen(scoreHistory) { currentScreen = MainScreen.Hub }
                }
            }
        }
    }
}

@Composable
fun GlobalStartScreen(name: String, onNameChange: (String) -> Unit, onStart: () -> Unit) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFE3F2FD), Color(0xFFFFFFFF))
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BRAIN",
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 8.sp,
            color = Color(0xFF1976D2),
            textAlign = TextAlign.Center
        )
        Text(
            text = "BUILDER",
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 8.sp,
            color = Color(0xFF1976D2),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Unlock Your Potential",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(64.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Enter Your Nickname") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStart,
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            Text("ENTER HUB", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(userName: String, onMathSelected: () -> Unit, onEnglishSelected: () -> Unit, onScienceSelected: () -> Unit, onHistorySelected: () -> Unit) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFE3F2FD), Color(0xFFFFFFFF))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BRAIN BUILDER", fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color(0xFF1976D2)) },
                actions = {
                    IconButton(onClick = onHistorySelected) {
                        Icon(Icons.Default.Menu, contentDescription = "History", tint = Color(0xFF1976D2))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome, $userName!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Text("Choose your subject", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            SubjectCard("MATH FOR ALL", "🔢", Color(0xFF4CAF50), onMathSelected)
            Spacer(modifier = Modifier.height(16.dp))
            SubjectCard("ENGLISH FOR ALL", "📚", Color(0xFF2196F3), onEnglishSelected)
            Spacer(modifier = Modifier.height(16.dp))
            SubjectCard("SCIENCE FOR ALL", "🔬", Color(0xFFFF9800), onScienceSelected)
            
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = onHistorySelected,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1976D2))
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF1976D2))
                Spacer(modifier = Modifier.width(8.dp))
                Text("VIEW SCORE HISTORY", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(records: List<ScoreRecord>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SCORE HISTORY", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No scores recorded yet!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(records) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(record.subject, fontWeight = FontWeight.ExtraBold, color = when(record.subject) {
                                    "MATH" -> Color(0xFF4CAF50)
                                    "ENGLISH" -> Color(0xFF2196F3)
                                    else -> Color(0xFFFF9800)
                                })
                                Text(record.date, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(record.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Name: ${record.nickname}", style = MaterialTheme.typography.bodyMedium)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Score: ${record.score} / ${record.total}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectCard(title: String, emoji: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(2.dp, color)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            Text(text = emoji, fontSize = 40.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathExamApp(userName: String, onExit: () -> Unit, onSaveScore: (ScoreRecord) -> Unit) {
    var selectedGrade by remember { mutableStateOf<Grade?>(null) }
    var selectedTopic by remember { mutableStateOf<Topic?>(null) }
    
    var currentQuestionNum by remember { mutableStateOf(1) }
    var score by remember { mutableStateOf(0) }
    var isExamFinished by remember { mutableStateOf(false) }
    val wrongAnswersList = remember { mutableStateListOf<WrongAnswer>() }
    var questionText by remember { mutableStateOf("") }
    var actualAnswer by remember { mutableStateOf("") }
    var userAnswer by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf(60) }
    var timeSpentSeconds by remember { mutableStateOf(0) }
    val totalQuestions = 10

    LaunchedEffect(isExamFinished) {
        if (isExamFinished) {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val currentDate = sdf.format(java.util.Date())
            onSaveScore(ScoreRecord(userName, "MATH", "${selectedGrade?.label} - ${selectedTopic?.label}", score, totalQuestions, currentDate))
        }
    }

    // ... (rest of helper functions same)
    fun simplify(num: Int, den: Int): String {
        fun getGcd(a: Int, b: Int): Int = if (b == 0) a else getGcd(b, a % b)
        val common = getGcd(num, den)
        val sNum = num / common
        val sDen = den / common
        return if (sDen == 1) "$sNum" else "$sNum/$sDen"
    }

    fun generateQuestion(grade: Grade, topic: Topic) {
        userAnswer = ""
        when (topic) {
            Topic.COUNTING -> {
                val num = Random.nextInt(1, 10)
                questionText = "How many apples? " + "🍎".repeat(num)
                actualAnswer = num.toString()
            }
            Topic.ADDITION -> {
                val (a, b) = if (grade == Grade.TODDLER) {
                    Random.nextInt(1, 10) to Random.nextInt(1, 10)
                } else if (grade == Grade.GRADE_1) {
                    val t = Random.nextInt(1, 10)
                    val o1 = Random.nextInt(0, 10)
                    val o2 = Random.nextInt(0, 10 - o1)
                    (t * 10 + o1) to o2
                } else {
                    val range = if (grade == Grade.GRADE_3) 100..1000 else 1000..10000
                    Random.nextInt(range.first, range.last) to Random.nextInt(range.first, range.last)
                }
                questionText = "$a + $b"; actualAnswer = (a + b).toString()
            }
            Topic.SUBTRACTION -> {
                val (a, b) = if (grade == Grade.TODDLER) {
                    val x = Random.nextInt(1, 10); val y = Random.nextInt(1, x + 1)
                    x to y
                } else if (grade == Grade.GRADE_1) {
                    val t = Random.nextInt(1, 10)
                    val o1 = Random.nextInt(0, 10)
                    val o2 = Random.nextInt(0, o1 + 1)
                    (t * 10 + o1) to o2
                } else {
                    val x = Random.nextInt(200, 1000)
                    x to Random.nextInt(100, x)
                }
                questionText = "$a - $b"; actualAnswer = (a - b).toString()
            }
            Topic.MULTIPLICATION -> {
                val (a, b) = when (grade) {
                    Grade.GRADE_1 -> Random.nextInt(1, 10) to Random.nextInt(1, 10)
                    Grade.GRADE_3 -> {
                        if (Random.nextBoolean()) Random.nextInt(10, 100) to Random.nextInt(2, 10)
                        else Random.nextInt(10, 100) to Random.nextInt(10, 100)
                    }
                    else -> Random.nextInt(10, 100) to Random.nextInt(10, 100)
                }
                questionText = "$a × $b"; actualAnswer = (a * b).toString()
            }
            Topic.DIVISION -> {
                val (total, divisor) = when (grade) {
                    Grade.GRADE_3 -> {
                        val div = Random.nextInt(2, 10)
                        val res = if (Random.nextBoolean()) Random.nextInt(2, 11) else Random.nextInt(11, 100)
                        (div * res) to div
                    }
                    Grade.GRADE_4 -> {
                        val div = Random.nextInt(2, 10)
                        val res = Random.nextInt(5, 50)
                        (div * res) to div
                    }
                    else -> {
                        val div = Random.nextInt(11, 20)
                        val res = Random.nextInt(5, 50)
                        (div * res) to div
                    }
                }
                questionText = "$total ÷ $divisor"; actualAnswer = (total / divisor).toString()
            }
            Topic.FRACTIONS -> {
                val dens = if (grade == Grade.GRADE_4) listOf(2, 4, 8) else listOf(3, 5, 6, 7, 9, 10)
                var d1 = dens.random(); var d2 = if (grade == Grade.GRADE_4) d1 else dens.random()
                var n1 = Random.nextInt(1, d1); var n2 = Random.nextInt(1, d2)

                val isAddition = Random.nextBoolean()
                if (isAddition) {
                    questionText = "$n1/$d1 + $n2/$d2"
                    actualAnswer = simplify((n1 * d2) + (n2 * d1), d1 * d2)
                } else {
                    if ((n1.toDouble() / d1) < (n2.toDouble() / d2)) {
                        val tempN = n1; val tempD = d1; n1 = n2; d1 = d2; n2 = tempN; d2 = tempD
                    }
                    questionText = "$n1/$d1 - $n2/$d2"
                    actualAnswer = simplify((n1 * d2) - (n2 * d1), d1 * d2)
                }
            }
            Topic.PEMDAS -> {
                val ops = listOf("+", "-", "×")
                var result: Int
                do {
                    val op1 = ops.random(); val op2 = ops.random()
                    val a = Random.nextInt(2, 15); val b = Random.nextInt(2, 10); val c = Random.nextInt(2, 10)
                    val type = Random.nextInt(3)
                    when (type) {
                        0 -> {
                            val res1 = if(op1=="+") a+b else if(op1=="-") a-b else a*b
                            result = if(op2=="+") res1+c else if(op2=="-") res1-c else res1*c
                            questionText = "($a $op1 $b) $op2 $c"
                        }
                        1 -> {
                            val res1 = if(op2=="+") b+c else if(op2=="-") b-c else b*c
                            result = if(op1=="+") a+res1 else if(op1=="-") a-res1 else a*res1
                            questionText = "$a $op1 ($b $op2 $c)"
                        }
                        else -> {
                            result = (a * b) + c
                            questionText = "$a × $b + $c"
                        }
                    }
                } while (result < 0)
                actualAnswer = result.toString()
            }
            Topic.ALGEBRA -> {
                val a = Random.nextInt(2, 6); val x = Random.nextInt(2, 15); val b = Random.nextInt(1, 20)
                val c = (a * x) + b
                questionText = "${a}x + $b = $c "; actualAnswer = x.toString()
            }
            Topic.GEOMETRY -> {
                if (grade == Grade.GRADE_3) {
                    val type = Random.nextInt(2)
                    if (type == 0) {
                        val side = Random.nextInt(2, 11)
                        questionText = "Square Perimeter\nSide = $side"; actualAnswer = (4 * side).toString()
                    } else {
                        val l = Random.nextInt(2, 10); val w = Random.nextInt(2, 10)
                        questionText = "Rectangle Perimeter\nL=$l, W=$w"; actualAnswer = (2 * (l + w)).toString()
                    }
                } else if (grade == Grade.GRADE_4) {
                    val type = Random.nextInt(2)
                    if (type == 0) {
                        val l = Random.nextInt(3, 13); val w = Random.nextInt(3, 13)
                        questionText = "Rectangle Area\nL=$l, W=$w"; actualAnswer = (l * w).toString()
                    } else {
                        val l = Random.nextInt(10, 25); val w = Random.nextInt(5, 15)
                        questionText = "Rectangle Perimeter\nL=$l, W=$w"; actualAnswer = (2 * (l + w)).toString()
                    }
                }
            }
            Topic.WORD_PROBLEMS -> {
                when (grade) {
                    Grade.GRADE_3 -> {
                        val items = listOf("apples", "pens", "books", "stickers").random()
                        val a = Random.nextInt(20, 50); val b = Random.nextInt(10, 20)
                        if (Random.nextBoolean()) {
                            questionText = "Sarah has $a $items. She gave $b to her friend. How many $items are left?"; actualAnswer = (a - b).toString()
                        } else {
                            questionText = "Tom bought $a $items and then $b more. How many $items does he have in total?"; actualAnswer = (a + b).toString()
                        }
                    }
                    Grade.GRADE_4 -> {
                        val a = Random.nextInt(5, 12); val b = Random.nextInt(10, 20)
                        questionText = "A box contains $b pencils. How many pencils are in $a boxes?"; actualAnswer = (a * b).toString()
                    }
                    Grade.GRADE_6 -> {
                        val total = Random.nextInt(5, 15) * 20
                        val spentPercent = listOf(20, 25, 50).random()
                        val result = (total * spentPercent) / 100
                        questionText = "Mark has P$total. He spent $spentPercent% of it on lunch. How much did he spend?"; actualAnswer = result.toString()
                    }
                    else -> {
                        val a = Random.nextInt(10, 20); val b = Random.nextInt(2, 5)
                        questionText = "If 1 notebook costs P$a, how much do $b notebooks cost?"; actualAnswer = (a * b).toString()
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedTopic, isExamFinished) {
        if (selectedTopic == null || isExamFinished) return@LaunchedEffect
        timeSpentSeconds = 0
        while (!isExamFinished) {
            delay(1000L)
            timeSpentSeconds++
        }
    }

    LaunchedEffect(currentQuestionNum, selectedTopic, isExamFinished) {
        if (selectedTopic == null || isExamFinished) return@LaunchedEffect
        
        val hasTimer = when (selectedGrade) {
            Grade.GRADE_3 -> selectedTopic != Topic.GEOMETRY && selectedTopic != Topic.WORD_PROBLEMS
            Grade.GRADE_6 -> selectedTopic != Topic.WORD_PROBLEMS
            else -> false
        }
        
        if (!hasTimer) return@LaunchedEffect

        timeLeft = if (selectedGrade == Grade.GRADE_3) 80 
                   else if (selectedTopic == Topic.FRACTIONS || selectedTopic == Topic.ALGEBRA) 120 
                   else 60

        while (timeLeft > 0 && !isExamFinished) {
            delay(1000L)
            timeLeft--
        }
        if (timeLeft == 0 && !isExamFinished) {
            wrongAnswersList.add(WrongAnswer(questionText, "Timed Out", actualAnswer))
            if (currentQuestionNum < totalQuestions) {
                currentQuestionNum++; generateQuestion(selectedGrade!!, selectedTopic!!)
            } else { isExamFinished = true }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MATH FOR ALL", fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedTopic != null) selectedTopic = null
                        else if (selectedGrade != null) selectedGrade = null
                        else onExit()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isExamFinished) {
                ResultsScreen(userName, score, totalQuestions, "${selectedGrade?.label} - ${selectedTopic?.label}", wrongAnswersList, timeSpentSeconds) {
                    score = 0; currentQuestionNum = 1; wrongAnswersList.clear(); timeSpentSeconds = 0
                    isExamFinished = false; selectedTopic = null; selectedGrade = null
                }
            } else if (selectedGrade == null) {
                GradeSelectionScreen(userName) { grade -> selectedGrade = grade }
            } else if (selectedTopic == null) {
                TopicSelectionScreen(selectedGrade!!) { topic ->
                    selectedTopic = topic; generateQuestion(selectedGrade!!, topic)
                }
            } else {
                Column {
                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Examinee: $userName", fontWeight = FontWeight.Bold)
                        Text("Category: ${selectedGrade?.label} - ${selectedTopic?.label}", style = MaterialTheme.typography.bodySmall)
                        val showTimer = when (selectedGrade) {
                            Grade.GRADE_3 -> selectedTopic != Topic.GEOMETRY && selectedTopic != Topic.WORD_PROBLEMS
                            Grade.GRADE_6 -> selectedTopic != Topic.WORD_PROBLEMS
                            else -> false
                        }
                        if (showTimer) {
                            Text("Time: ${timeLeft}s", color = if (timeLeft < 10) Color.Red else Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    ExamScreen(currentQuestionNum, totalQuestions, questionText, userAnswer,
                        onAnswerChange = { userAnswer = it },
                        onBack = { selectedTopic = null; currentQuestionNum = 1; score = 0; wrongAnswersList.clear() },
                        onSubmit = {
                            if (userAnswer.trim() == actualAnswer) score++
                            else wrongAnswersList.add(WrongAnswer(questionText, userAnswer, actualAnswer))
                            if (currentQuestionNum < totalQuestions) {
                                currentQuestionNum++; generateQuestion(selectedGrade!!, selectedTopic!!)
                            } else { isExamFinished = true }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GradeSelectionScreen(name: String, onGradePicked: (Grade) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Welcome, $name", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Select your level:", modifier = Modifier.padding(bottom = 32.dp))
        Grade.values().forEach { grade ->
            Button(onClick = { onGradePicked(grade) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(grade.label, fontSize = 18.sp, color = Color.White) }
        }
    }
}

@Composable
fun TopicSelectionScreen(grade: Grade, onTopicPicked: (Topic) -> Unit) {
    val topics = when (grade) {
        Grade.TODDLER -> listOf(Topic.COUNTING, Topic.ADDITION, Topic.SUBTRACTION)
        Grade.GRADE_1 -> listOf(Topic.ADDITION, Topic.SUBTRACTION, Topic.MULTIPLICATION)
        Grade.GRADE_3 -> listOf(Topic.ADDITION, Topic.SUBTRACTION, Topic.MULTIPLICATION, Topic.DIVISION, Topic.GEOMETRY, Topic.WORD_PROBLEMS)
        Grade.GRADE_4 -> listOf(Topic.ADDITION, Topic.MULTIPLICATION, Topic.DIVISION, Topic.FRACTIONS, Topic.GEOMETRY, Topic.WORD_PROBLEMS)
        Grade.GRADE_6 -> listOf(Topic.DIVISION, Topic.FRACTIONS, Topic.PEMDAS, Topic.ALGEBRA, Topic.WORD_PROBLEMS)
    }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Grade ${grade.label}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Select a topic:", modifier = Modifier.padding(bottom = 32.dp))
        topics.forEach { topic ->
            Button(onClick = { onTopicPicked(topic) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp)) { Text(topic.label, fontSize = 18.sp) }
        }
    }
}

@Composable
fun ExamScreen(qNum: Int, total: Int, qText: String, uAns: String, onAnswerChange: (String) -> Unit, onBack: () -> Unit, onSubmit: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Question $qNum of $total")
        val displayFontSize = if (qText.length > 15) 28.sp else 48.sp
        Text(
            text = qText,
            fontSize = displayFontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = displayFontSize * 1.2f,
            modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.Center) {
            OutlinedTextField(value = uAns, onValueChange = onAnswerChange, label = { Text("Answer") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).padding(end = 8.dp), singleLine = true)
            Button(onClick = onSubmit, modifier = Modifier.height(56.dp)) { Text("SUBMIT") }
        }
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(0.5f)) { Text("GO BACK") }
    }
}

@Composable
fun ResultsScreen(name: String, score: Int, total: Int, modeName: String, mistakes: List<WrongAnswer>, timeSpent: Int, onRestart: () -> Unit) {
    val context = LocalContext.current
    val minutes = timeSpent / 60
    val seconds = timeSpent % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    LaunchedEffect(Unit) {
        if (mistakes.size >= 4) {
            val mediaPlayer = MediaPlayer.create(context, R.raw.laugh)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } else {
            val mediaPlayer = MediaPlayer.create(context, R.raw.clap)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Examinee: $name", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Category: $modeName", color = Color.Gray)
        Text("Final Score: $score / $total", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color(0xFF388E3C))
        Text("Time Taken: $timeFormatted", fontSize = 18.sp, fontWeight = FontWeight.Medium)

        Surface(color = Color(0xFFFFEB3B), modifier = Modifier.padding(vertical = 16.dp)) {
            Text("PLEASE SCREENSHOT YOUR RESULTS", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.ExtraBold)
        }
        Text("Mistake Review:", modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp), fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(mistakes) { m ->
                Surface(tonalElevation = 3.dp, modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Question: ${m.question}", fontWeight = FontWeight.Bold)
                        Row {
                            Text("Yours: ", fontSize = 14.sp)
                            Text(m.userAnswer, color = Color.Red, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Correct: ", fontSize = 14.sp)
                            Text(m.correctAnswer, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("NEW TEST") }
    }
}
