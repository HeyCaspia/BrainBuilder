package com.example.mathgenius

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import kotlin.random.Random

data class EnglishLevel(
    val id: String,
    val name: String,
    val topics: List<EnglishTopic>
)

data class EnglishTopic(
    val id: String,
    val name: String,
    val exercises: List<EnglishExercise>,
    val passage: String? = null
)

data class EnglishExercise(
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)

sealed class EnglishScreen {
    object LevelSelection : EnglishScreen()
    data class TopicSelection(val level: EnglishLevel) : EnglishScreen()
    data class ExerciseScreen(val topic: EnglishTopic) : EnglishScreen()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun EnglishApp(userName: String, onExit: () -> Unit, onSaveScore: (ScoreRecord) -> Unit) {
    var currentScreen by remember { mutableStateOf<EnglishScreen>(EnglishScreen.LevelSelection) }
    
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
        val processedText = text.replace(Regex("_+"), " blank ")
        tts?.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ENGLISH FOR ALL", fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentScreen) {
                            is EnglishScreen.LevelSelection -> onExit()
                            is EnglishScreen.TopicSelection -> currentScreen = EnglishScreen.LevelSelection
                            is EnglishScreen.ExerciseScreen -> {
                                val topic = (currentScreen as EnglishScreen.ExerciseScreen).topic
                                currentScreen = EnglishScreen.TopicSelection(englishLevels.find { it.topics.any { t -> t.id == topic.id } }!!)
                            }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF2196F3),
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
                label = "EnglishTransition"
            ) { screen ->
                when (screen) {
                    is EnglishScreen.LevelSelection -> {
                        EnglishLevelSelectionScreen(userName) { currentScreen = EnglishScreen.TopicSelection(it) }
                    }
                    is EnglishScreen.TopicSelection -> {
                        BackHandler { currentScreen = EnglishScreen.LevelSelection }
                        EnglishTopicSelectionScreen(screen.level) { currentScreen = EnglishScreen.ExerciseScreen(it) }
                    }
                    is EnglishScreen.ExerciseScreen -> {
                        val backScreen = EnglishScreen.TopicSelection(englishLevels.find { it.topics.any { t -> t.id == screen.topic.id } }!!)
                        BackHandler { currentScreen = backScreen }
                        EnglishExerciseApp(
                            userName = userName,
                            topic = screen.topic,
                            showSpeakButton = (englishLevels.find { it.topics.any { t -> t.id == screen.topic.id } }?.id in listOf("kinder", "grade1")),
                            onSpeak = { speak(it) },
                            onBack = { currentScreen = backScreen },
                            onSaveScore = onSaveScore
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnglishLevelSelectionScreen(name: String, onLevelSelected: (EnglishLevel) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Welcome, $name", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Select your level:", modifier = Modifier.padding(bottom = 32.dp))
        englishLevels.forEach { level ->
            Button(
                onClick = { onLevelSelected(level) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text(level.name, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun EnglishTopicSelectionScreen(level: EnglishLevel, onTopicSelected: (EnglishTopic) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(level.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Select a topic:", modifier = Modifier.padding(bottom = 32.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(level.topics) { topic ->
                Button(
                    onClick = { onTopicSelected(topic) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF2196F3))
                ) {
                    Text(topic.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            val compPool = level.topics.filter { it.passage != null }
            if (compPool.isNotEmpty()) {
                item {
                    Button(
                        onClick = { onTopicSelected(compPool.random()) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE082), contentColor = Color(0xFFE65100))
                    ) {
                        Text("Random Reading Passage 🎲", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun EnglishExerciseApp(userName: String, topic: EnglishTopic, showSpeakButton: Boolean = false, onSpeak: (String) -> Unit = {}, onBack: () -> Unit, onSaveScore: (ScoreRecord) -> Unit) {
    var currentExerciseIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var timeSpentSeconds by remember { mutableStateOf(0) }
    val wrongAnswersList = remember { mutableStateListOf<WrongAnswer>() }

    val exercises = remember(topic) { 
        if (topic.id.startsWith("summative_test")) {
            topic.exercises
        } else {
            topic.exercises.shuffled().take(10).map { it.copy(options = it.options.shuffled()) }
        }
    }

    LaunchedEffect(showResult) {
        if (showResult) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            onSaveScore(ScoreRecord(userName, "ENGLISH", topic.name, score, exercises.size, sdf.format(Date())))
        }
    }

    LaunchedEffect(showResult) {
        if (!showResult) {
            while (true) {
                delay(1000L)
                timeSpentSeconds++
            }
        }
    }

    if (showResult) {
        ResultsScreen(
            name = userName,
            score = score,
            total = exercises.size,
            modeName = topic.name,
            mistakes = wrongAnswersList,
            timeSpent = timeSpentSeconds,
            onRestart = onBack
        )
    } else {
        val exercise = exercises[currentExerciseIndex]
        Column {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFE3F2FD)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Examinee: $userName", fontWeight = FontWeight.Bold)
                        Text("Category: ${topic.name}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (showSpeakButton) {
                        IconButton(onClick = { onSpeak(exercise.question) }) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "Speak", tint = Color(0xFF2196F3))
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Question ${currentExerciseIndex + 1} of ${exercises.size}")
                
                if (topic.passage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(0.45f).padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                    ) {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            item { 
                                Text(
                                    text = topic.passage, 
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 18.sp,
                                    lineHeight = 26.sp
                                ) 
                            }
                        }
                    }
                }

                val displayFontSize = if (exercise.question.length > 30) 20.sp else 24.sp
                Text(
                    text = exercise.question,
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = displayFontSize * 1.2f,
                    modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth()
                )

                exercise.options.forEach { option ->
                    val isSelected = selectedOption == option
                    // Neutral color for selection, don't show correct/incorrect yet
                    val color = if (isSelected) Color(0xFF2196F3) else Color.White
                    
                    OutlinedButton(
                        onClick = {
                            if (selectedOption == null) {
                                selectedOption = option
                                val correct = option == exercise.correctAnswer
                                isAnswerCorrect = correct
                                if (correct) {
                                    score++
                                } else {
                                    wrongAnswersList.add(WrongAnswer(exercise.question, option, exercise.correctAnswer))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = color, contentColor = if (color == Color.White) Color.Black else Color.White),
                        enabled = selectedOption == null
                    ) {
                        Text(option, fontSize = 16.sp)
                    }
                }

                if (isAnswerCorrect != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (currentExerciseIndex < exercises.size - 1) {
                                currentExerciseIndex++
                                selectedOption = null
                                isAnswerCorrect = null
                            } else {
                                showResult = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text(if (currentExerciseIndex < exercises.size - 1) "NEXT" else "FINISH")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(0.5f)) { Text("GO BACK") }
            }
        }
    }
}

val englishLevels = listOf(
    EnglishLevel(
        id = "kinder",
        name = "Kinder 🎒",
        topics = listOf(
            EnglishTopic("cvc_words", "Word Building 🧩", listOf(
                EnglishExercise("Finish the word: C _ T (Meow!)", listOf("A", "O", "I"), "A"),
                EnglishExercise("Finish the word: P _ G (Oink!)", listOf("E", "I", "U"), "I"),
                EnglishExercise("Which word has the 'sh' sound?", listOf("Sun", "Fish", "Sat"), "Fish"),
                EnglishExercise("Which word has the 'ch' sound?", listOf("Cat", "Chair", "Car"), "Chair"),
                EnglishExercise("What is the middle sound in 'HOP'?", listOf("O", "A", "E"), "O"),
                EnglishExercise("What is the middle sound in 'TEN'?", listOf("A", "E", "I"), "E"),
                EnglishExercise("Which word rhymes with 'STAR'?", listOf("Car", "Sun", "Tree"), "Car"),
                EnglishExercise("Which word rhymes with 'BLUE'?", listOf("Shoe", "Boat", "Red"), "Shoe"),
                EnglishExercise("How many sounds are in the word 'DOG'?", listOf("2", "3", "4"), "3"),
                EnglishExercise("Which letter is missing in 'B _ X'?", listOf("O", "U", "A"), "O")
            )),
            EnglishTopic("reading_logic", "Reading & Logic 🧠", listOf(
                EnglishExercise("Where is the ball? 'The ball is ___ the box.'", listOf("in", "at", "to"), "in"),
                EnglishExercise("Which word is an action?", listOf("Run", "Red", "Rat"), "Run"),
                EnglishExercise("Which word describes a color?", listOf("Yellow", "Happy", "Fast"), "Yellow"),
                EnglishExercise("I ____ my dog.", listOf("love", "is", "am"), "love"),
                EnglishExercise("Which word is a person?", listOf("Mother", "Milk", "Moon"), "Mother"),
                EnglishExercise("Find the opposite of 'Happy'.", listOf("Sad", "Glad", "Fun"), "Sad"),
                EnglishExercise("The apple is ____.", listOf("Red", "Blue", "Black"), "Red"),
                EnglishExercise("Which word is a place?", listOf("School", "Sit", "Small"), "School"),
                EnglishExercise("I use my ____ to see.", listOf("Eyes", "Ears", "Nose"), "Eyes"),
                EnglishExercise("Find the opposite of 'Day'.", listOf("Night", "Noon", "Sun"), "Night")
            ))
        )
    ),
    EnglishLevel(
        id = "grade1",
        name = "Grade 1 🎒",
        topics = listOf(
            EnglishTopic("word_building", "Word Building 🧩", listOf(
                EnglishExercise("Which word has a silent 'e' at the end?", listOf("Hop", "Hope", "Hip"), "Hope"),
                EnglishExercise("Which blend starts the word 'Street'?", listOf("St", "Str", "Tr"), "Str"),
                EnglishExercise("How many syllables are in the word 'Apple'?", listOf("1", "2", "3"), "2"),
                EnglishExercise("Change 'h' in 'Hat' to 'r'. New word?", listOf("Rat", "Bat", "Cat"), "Rat"),
                EnglishExercise("Which word has a long 'O' sound?", listOf("Hot", "Boat", "Not"), "Boat"),
                EnglishExercise("Which word has a short 'E' sound?", listOf("Tree", "Red", "Eat"), "Red"),
                EnglishExercise("Opposite of 'Front'?", listOf("Beside", "Back", "Under"), "Back"),
                EnglishExercise("Which word rhymes with 'Light'?", listOf("Night", "Late", "Look"), "Night"),
                EnglishExercise("Root word of 'Running'?", listOf("Run", "Runner", "Runs"), "Run"),
                EnglishExercise("Which word starts like 'Chair'?", listOf("Ship", "Cheese", "Shoes"), "Cheese")
            )),
            EnglishTopic("grammar_logic", "Grammar & Logic 💡", listOf(
                EnglishExercise("Which word starts with a capital letter?", listOf("monday", "apple", "school"), "monday"),
                EnglishExercise("Which sentence is correct?", listOf("The dog bark.", "The dog barks.", "Dog barking."), "The dog barks."),
                EnglishExercise("Find the compound word.", listOf("Running", "Raincoat", "Redder"), "Raincoat"),
                EnglishExercise("Which word is an action (verb)?", listOf("Quickly", "Dance", "Beautiful"), "Dance"),
                EnglishExercise("Which word is a pronoun?", listOf("She", "Sally", "Sister"), "She"),
                EnglishExercise("What is the plural of 'Foot'?", listOf("Foots", "Feet", "Feets"), "Feet"),
                EnglishExercise("She ____ going to the park.", listOf("is", "are", "am"), "is"),
                EnglishExercise("Find the opposite of 'Fast'.", listOf("Slow", "Quick", "Rapid"), "Slow"),
                EnglishExercise("I have ____ orange.", listOf("a", "an", "the"), "an"),
                EnglishExercise("The cat is ____ the chair.", listOf("under", "at", "to"), "under")
            ))
        )
    ),
    EnglishLevel(
        id = "grade3",
        name = "Grade 3 📚",
        topics = listOf(
            EnglishTopic(
                id = "reading_comp_g3_1",
                name = "Reading Comprehension: Honeybees 🐝",
                passage = "Honeybees are incredible insects that play a vital role in our environment. They live in large groups called colonies, usually found in hives. A single hive can have thousands of bees, including one queen, many workers, and a few drones. Workers fly from flower to flower, collecting nectar and pollen. As they do this, they help pollinate plants, which allows fruits and vegetables to grow. Without honeybees, our food supply would be greatly affected. They also communicate through a special 'dance' to tell others where the best flowers are. Bees are very hardworking and organized.",
                exercises = listOf(
                    EnglishExercise("What is a group of bees called?", listOf("A herd", "A colony", "A pack"), "A colony"),
                    EnglishExercise("Which bee is the leader of the hive?", listOf("The worker", "The drone", "The queen"), "The queen"),
                    EnglishExercise("What do bees collect from flowers?", listOf("Nectar and pollen", "Water", "Honey"), "Nectar and pollen"),
                    EnglishExercise("Why is pollination important?", listOf("It makes flowers smell good", "It helps fruits grow", "It keeps bees warm"), "It helps fruits grow"),
                    EnglishExercise("Where do honeybees usually live?", listOf("In nests", "In hives", "Underground"), "In hives"),
                    EnglishExercise("How many queens are usually in a hive?", listOf("One", "Two", "Many"), "One"),
                    EnglishExercise("How do bees communicate locations?", listOf("They sing", "They dance", "They buzz"), "They dance"),
                    EnglishExercise("Which bees fly from flower to flower?", listOf("Workers", "Queens", "Drones"), "Workers"),
                    EnglishExercise("What would happen without bees?", listOf("Nothing", "Food supply affected", "More flowers"), "Food supply affected"),
                    EnglishExercise("What kind of animals are bees?", listOf("Mammals", "Insects", "Birds"), "Insects")
                )
            ),
            EnglishTopic(
                id = "reading_comp_g3_2",
                name = "Reading Comprehension: The Water Cycle 💧",
                passage = "The water cycle is the path that all water follows as it moves around Earth in different states. Liquid water is found in oceans, rivers, lakes, and even underground. It can be solid ice in glaciers and snow. It can also be a gas called water vapor in the atmosphere. The cycle has four main stages: evaporation (sun heats water up), condensation (water turns to clouds), precipitation (rain or snow), and collection. This continuous process has been going on for billions of years, ensuring that Earth's water is recycled and distributed. It is powered by the sun's energy.",
                exercises = listOf(
                    EnglishExercise("What is water vapor?", listOf("Solid water", "Liquid water", "Gaseous water"), "Gaseous water"),
                    EnglishExercise("Which stage turns water into clouds?", listOf("Evaporation", "Condensation", "Precipitation"), "Condensation"),
                    EnglishExercise("Where is most liquid water found?", listOf("Glaciers", "Oceans", "The atmosphere"), "Oceans"),
                    EnglishExercise("What is precipitation?", listOf("Water turning to gas", "Rain or snow falling", "Water collection"), "Rain or snow falling"),
                    EnglishExercise("How long has the water cycle existed?", listOf("Thousands of years", "Millions of years", "Billions of years"), "Billions of years"),
                    EnglishExercise("What powers the water cycle?", listOf("Wind", "The Sun", "Moon"), "The Sun"),
                    EnglishExercise("What is evaporation?", listOf("Water cooling", "Water heating up", "Water freezing"), "Water heating up"),
                    EnglishExercise("Where can you find solid water?", listOf("Rivers", "Glaciers", "Clouds"), "Glaciers"),
                    EnglishExercise("What is the final stage mentioned?", listOf("Collection", "Condensation", "Evaporation"), "Collection"),
                    EnglishExercise("Is the water cycle a new process?", listOf("Yes", "No, billions of years", "Only since humans"), "No, billions of years")
                )
            ),
            EnglishTopic("nouns", "Noun Hunt 🕵️‍♂️", listOf(
                EnglishExercise("Which is a PROPER noun?", listOf("city", "Paris", "building"), "Paris"),
                EnglishExercise("Identify the noun: 'Happiness is key.'", listOf("Is", "Happiness", "Key"), "Happiness"),
                EnglishExercise("Plural of 'Child'?", listOf("Childs", "Children", "Childrens"), "Children"),
                EnglishExercise("Collective noun for lions?", listOf("Pride", "Pack", "Herd"), "Pride"),
                EnglishExercise("Identify the noun: 'Honesty is best.'", listOf("Best", "Policy", "Honesty"), "Honesty"),
                EnglishExercise("Which is NOT a noun?", listOf("Teacher", "Quickly", "Class"), "Quickly"),
                EnglishExercise("Plural of 'Mouse'?", listOf("Mouses", "Mice", "Mices"), "Mice"),
                EnglishExercise("Common noun for 'Dr. Smith'?", listOf("Doctor", "Man", "Person"), "Doctor"),
                EnglishExercise("Concrete noun (can touch)?", listOf("Love", "Computer", "Idea"), "Computer"),
                EnglishExercise("Proper noun for a day?", listOf("sunday", "Sunday", "day"), "Sunday")
            ))
        )
    ),
    EnglishLevel(
        id = "grade4",
        name = "Grade 4 📖",
        topics = listOf(
            EnglishTopic(
                id = "reading_comp_g4_1",
                name = "Reading Comprehension: Ecosystems 🌏",
                passage = "An ecosystem is a community of living organisms interacting with their non-living environment. Living things, like plants and animals, are biotic factors, while non-living things, like sunlight, water, and soil, are abiotic factors. Every part of an ecosystem has a role to play. For example, producers (plants) use sunlight to make food through photosynthesis, while consumers (animals) eat plants or other animals. Decomposers, like fungi and bacteria, break down dead matter, returning nutrients to the soil. If one part of the ecosystem is removed, the entire balance can be disrupted. Biodiversity is important for a healthy ecosystem.",
                exercises = listOf(
                    EnglishExercise("What are biotic factors?", listOf("Living things", "Non-living things", "Only plants"), "Living things"),
                    EnglishExercise("Which is an abiotic factor?", listOf("Bacteria", "Sunlight", "Fungi"), "Sunlight"),
                    EnglishExercise("What do producers use to make food?", listOf("Water only", "Sunlight", "Other plants"), "Sunlight"),
                    EnglishExercise("What is the role of decomposers?", listOf("To hunt animals", "To return nutrients to soil", "To make oxygen"), "To return nutrients to soil"),
                    EnglishExercise("What happens if one part is removed?", listOf("Nothing happens", "The balance is disrupted", "Everything grows faster"), "The balance is disrupted"),
                    EnglishExercise("Process plants use to make food?", listOf("Respiration", "Photosynthesis", "Digestion"), "Photosynthesis"),
                    EnglishExercise("Example of a decomposer?", listOf("Lion", "Fungi", "Grass"), "Fungi"),
                    EnglishExercise("What are animals in an ecosystem?", listOf("Producers", "Consumers", "Decomposers"), "Consumers"),
                    EnglishExercise("What is an ecosystem?", listOf("Only animals", "A community of organisms", "Only plants"), "A community of organisms"),
                    EnglishExercise("Is soil a biotic factor?", listOf("Yes", "No", "Sometimes"), "No")
                )
            ),
            EnglishTopic(
                id = "reading_comp_g4_2",
                name = "Reading Comprehension: States of Matter 🧪",
                passage = "Everything in the universe that has mass and takes up space is matter. Matter exists in three main states on Earth: solid, liquid, and gas. Solids have a definite shape and volume because their particles are packed closely together. Liquids have a definite volume but take the shape of their container as their particles can move around each other. Gases have no definite shape or volume and spread out to fill any space available. Changing temperature can cause matter to change from one state to another, such as ice melting into water or water boiling into steam. All matter is made of tiny atoms.",
                exercises = listOf(
                    EnglishExercise("What defines matter?", listOf("Something that moves", "Mass and volume", "Only solids"), "Mass and volume"),
                    EnglishExercise("Which state takes the shape of its container?", listOf("Solid", "Liquid", "Gas"), "Liquid"),
                    EnglishExercise("Which state has no definite volume?", listOf("Solid", "Liquid", "Gas"), "Gas"),
                    EnglishExercise("What causes matter to change state?", listOf("Pressure only", "Temperature changes", "Color changes"), "Temperature changes"),
                    EnglishExercise("What is steam an example of?", listOf("Solid", "Liquid", "Gas"), "Gas"),
                    EnglishExercise("Why do solids have a definite shape?", listOf("Particles far apart", "Particles packed close", "No particles"), "Particles packed close"),
                    EnglishExercise("Tiny building blocks of matter?", listOf("Cells", "Atoms", "Dust"), "Atoms"),
                    EnglishExercise("Example of a solid?", listOf("Water", "Ice", "Air"), "Ice"),
                    EnglishExercise("Can liquids be compressed easily?", listOf("Yes", "No", "Always"), "No"),
                    EnglishExercise("What state is the air we breathe?", listOf("Solid", "Liquid", "Gas"), "Gas")
                )
            ),
            EnglishTopic("adjectives", "Describing Words ✨", listOf(
                EnglishExercise("Superlative form of 'Fast'?", listOf("Fasters", "Fastest", "More fast"), "Fastest"),
                EnglishExercise("Identify the adjective: 'Mysterious stranger.'", listOf("Stranger", "Mysterious", "Arrived"), "Mysterious"),
                EnglishExercise("Which fits: 'The ____ cake ever!'", listOf("better", "goodest", "best"), "best"),
                EnglishExercise("Comparative of 'Heavy'?", listOf("Heavier", "Heaviest", "More heavy"), "Heavier"),
                EnglishExercise("Which is an adjective?", listOf("Slowly", "Courageous", "Running"), "Courageous"),
                EnglishExercise("Adjective for size?", listOf("Gigantic", "Blue", "Happy"), "Gigantic"),
                EnglishExercise("Comparative of 'Good'?", listOf("Gooder", "Better", "Best"), "Better"),
                EnglishExercise("Opposite of 'Old'?", listOf("New", "Modern", "Both"), "Both"),
                EnglishExercise("Identify adjective: 'Gloomy weather.'", listOf("Weather", "Was", "Gloomy"), "Gloomy"),
                EnglishExercise("Which describes color?", listOf("Fast", "Emerald", "Strong"), "Emerald")
            )),
            EnglishTopic(
                id = "summative_test_g4",
                name = "First Summative Test 📝",
                exercises = listOf(
                    EnglishExercise("1. Which word means the same as “rapid”?", listOf("fast", "slow", "quiet", "late"), "fast"),
                    EnglishExercise("2. Which word is opposite in meaning to “ancient”?", listOf("old", "modern", "historic", "traditional"), "modern"),
                    EnglishExercise("3. What does the prefix “un-” mean in “unhappy”?", listOf("again", "before", "not", "full of"), "not"),
                    EnglishExercise("4. Which context clue helps define “arid” in “The arid land was dry and had little rain”?", listOf("land", "was", "the", "dry and had little rain"), "dry and had little rain"),
                    EnglishExercise("5. Which sentence has a compound subject?", listOf("Lina reads.", "Read the book.", "Lina and Ben read.", "In the library."), "Lina and Ben read."),
                    EnglishExercise("6. Which sentence has a compound predicate?", listOf("The pupils and teacher read.", "The pupils read and discuss the story.", "Reading the story", "The long story"), "The pupils read and discuss the story."),
                    EnglishExercise("7. Which conjunction shows contrast?", listOf("but", "and", "or", "so"), "but"),
                    EnglishExercise("8. Which sentence is compound?", listOf("The players returned.", "After the rain", "Returning players", "The rain stopped, so the players returned."), "The rain stopped, so the players returned."),
                    EnglishExercise("9. Read: “Mangroves protect shorelines and provide homes for young fish.” What is the main idea?", listOf("Mangroves help shores and marine life.", "Fish live only in rivers.", "Shorelines have roads.", "Mangroves are tall buildings."), "Mangroves help shores and marine life."),
                    EnglishExercise("10. Which detail supports the main idea?", listOf("They grow on mountains only.", "They are made of plastic.", "They provide homes for young fish.", "They remove all water."), "They provide homes for young fish."),
                    EnglishExercise("11. What text structure lists similarities and differences?", listOf("sequence", "cause and effect", "problem and solution", "compare and contrast"), "compare and contrast"),
                    EnglishExercise("12. Which signal word suggests cause and effect?", listOf("first", "because", "similarly", "beside"), "because"),
                    EnglishExercise("13. What is the author’s purpose in a safety guide?", listOf("to inform and instruct", "to entertain with fantasy only", "to sell an unrelated product", "to hide information"), "to inform and instruct"),
                    EnglishExercise("14. Which statement is a fact?", listOf("Rain is the best weather.", "Blue is the prettiest color.", "Water freezes at 0°C under standard conditions.", "All parks are exciting."), "Water freezes at 0°C under standard conditions."),
                    EnglishExercise("15. Which is an opinion?", listOf("The program starts at 8:00 a.m.", "The festival dance is beautiful.", "The poster has three pictures.", "The hall is in the barangay."), "The festival dance is beautiful."),
                    EnglishExercise("16. Which source is most reliable for a weather warning?", listOf("Anonymous message", "Unverified post", "A guess", "Official weather agency advisory"), "Official weather agency advisory"),
                    EnglishExercise("17. Which summary is best?", listOf("Mangroves protect coasts and support young fish.", "Mangroves are green, and I like green.", "The text has nine words.", "Fish are fun to watch."), "Mangroves protect coasts and support young fish."),
                    EnglishExercise("18. What can be inferred when dark clouds gather and people open umbrellas?", listOf("It is snowing.", "The day is becoming hotter.", "The umbrellas are broken.", "Rain is likely."), "Rain is likely."),
                    EnglishExercise("19. Which sentence uses correct subject-verb agreement?", listOf("The children helps the community.", "The child help the community.", "The children help the community.", "The children helping the community."), "The children helping the community."),
                    EnglishExercise("20. Which response uses evidence?", listOf("I just think so.", "I think mangroves are important because the text says they protect shorelines.", "Everyone must agree.", "The page looks nice."), "I think mangroves are important because the text says they protect shorelines.")
                )
            )
        )
    ),
    EnglishLevel(
        id = "grade6",
        name = "Grade 6 🎓",
        topics = listOf(
            EnglishTopic(
                id = "reading_comp_g6_1",
                name = "Reading Comprehension: Genetics 🧬",
                passage = "Genetics is the branch of biology concerned with the study of genes, genetic variation, and heredity in organisms. DNA, or deoxyribonucleic acid, is the molecule that carries genetic instructions for the development, functioning, growth, and reproduction of all known organisms. The Human Genome Project, completed in 2003, was an international research effort to determine the DNA sequence of the entire human genome. This breakthrough has revolutionized medicine, allowing scientists to identify genetic predispositions to diseases and develop personalized treatments. Genes are passed from parents to offspring, determining traits like eye color and height. Research continues to uncover how genes interact with the environment.",
                exercises = listOf(
                    EnglishExercise("What does DNA stand for?", listOf("Deoxyribonucleic acid", "Dynamic Natural Acid", "Digital Neural Array"), "Deoxyribonucleic acid"),
                    EnglishExercise("Goal of the Human Genome Project?", listOf("Cure all cancers", "Sequence the genome", "Create new species"), "Sequence the human genome"),
                    EnglishExercise("When was the HGP completed?", listOf("1999", "2003", "2010"), "2003"),
                    EnglishExercise("Effect of HGP on medicine?", listOf("Medicine cheaper", "Personalized treatments", "No doctors"), "It allowed for personalized treatments"),
                    EnglishExercise("What is heredity?", listOf("Study of weather", "Passing of traits", "Plant growth"), "The passing of traits from parents"),
                    EnglishExercise("Unit of heredity passed to offspring?", listOf("Gene", "Cell", "Blood"), "Gene"),
                    EnglishExercise("What carries genetic instructions?", listOf("Protein", "DNA", "Sugar"), "DNA"),
                    EnglishExercise("Example of a genetic trait?", listOf("Language", "Eye color", "Skill"), "Eye color"),
                    EnglishExercise("Is genetics a branch of Chemistry?", listOf("Yes", "No, Biology", "Both"), "No, Biology"),
                    EnglishExercise("What determines how genes express?", listOf("Only DNA", "Genes and environment", "Only luck"), "Genes and environment")
                )
            ),
            EnglishTopic(
                id = "reading_comp_g6_2",
                name = "Reading Comprehension: Climate Change 🌡️",
                passage = "Climate change refers to long-term shifts in temperatures and weather patterns. These shifts may be natural, but since the 1800s, human activities have been the main driver of climate change, primarily due to the burning of fossil fuels like coal, oil, and gas. Burning fossil fuels generates greenhouse gas emissions that act like a blanket wrapped around the Earth, trapping the sun’s heat and raising temperatures. Consequences include intense droughts, water scarcity, severe fires, rising sea levels, and catastrophic storms. Reducing emissions and transitioning to renewable energy are critical steps in mitigating these effects. The global community is working towards limiting temperature increases.",
                exercises = listOf(
                    EnglishExercise("Main driver of modern climate change?", listOf("Natural cycles", "Human activities", "Volcanoes"), "Human activities"),
                    EnglishExercise("How do greenhouse gases affect Earth?", listOf("Block sunlight", "Trap heat", "Cool Earth"), "They trap heat"),
                    EnglishExercise("Consequence of climate change?", listOf("More forests", "Rising sea levels", "Lower temps"), "Rising sea levels"),
                    EnglishExercise("Since when human-driven?", listOf("1700s", "1800s", "1900s"), "1800s"),
                    EnglishExercise("Acts like a 'blanket'?", listOf("Clouds", "Greenhouse emissions", "Ozone"), "Greenhouse gas emissions"),
                    EnglishExercise("Critical step in mitigation?", listOf("Renewable energy", "Burning more coal", "Deforestation"), "Renewable energy"),
                    EnglishExercise("Main fossil fuels mentioned?", listOf("Wood", "Coal, oil, gas", "Sunlight"), "Coal, oil, gas"),
                    EnglishExercise("What generates greenhouse emissions?", listOf("Planting trees", "Burning fossil fuels", "Wind mills"), "Burning fossil fuels"),
                    EnglishExercise("Effect on sea levels?", listOf("Lowering", "Rising", "No effect"), "Rising"),
                    EnglishExercise("Global goal for temperature?", listOf("Increase quickly", "Limit increases", "Ignore it"), "Limit increases")
                )
            ),
            EnglishTopic("grammar", "Grammar Master 👑", listOf(
                EnglishExercise("Present perfect of 'Go'?", listOf("I go.", "I went.", "I have gone."), "I have gone."),
                EnglishExercise("Conjunction in: 'I ran, but was late.'", listOf("Ran", "But", "Late"), "But"),
                EnglishExercise("Correct agreement?", listOf("They runs.", "He run.", "She runs."), "She runs."),
                EnglishExercise("Preposition: 'Under the table.'", listOf("Hid", "Under", "Table"), "Under"),
                EnglishExercise("Which is an adverb?", listOf("Beautiful", "Quickly", "Friend"), "Quickly"),
                EnglishExercise("Past participle of 'Fly'?", listOf("Flies", "Flew", "Flown"), "Flown"),
                EnglishExercise("Interjection?", listOf("Wow", "That", "Amazing"), "Wow"),
                EnglishExercise("Passive voice?", listOf("He broke it.", "It was broken.", "It broke."), "It was broken."),
                EnglishExercise("Complex sentence?", listOf("I like milk.", "Milk and cookies.", "Since hungry, I ate."), "Since I was hungry, I ate."),
                EnglishExercise("Plural of 'Phenomenon'?", listOf("Phenomenons", "Phenomena", "Phenomenies"), "Phenomena")
            )),
            EnglishTopic(
                id = "summative_test_g6",
                name = "First Summative Test 📝",
                exercises = listOf(
                    EnglishExercise("1. In the sentence \"The resilient community rebuilt after the storm,\" what does resilient mean?", listOf("able to recover from difficulty", "easily broken", "unwilling to work", "very quiet"), "able to recover from difficulty"),
                    EnglishExercise("2. Which word has the most positive connotation?", listOf("stubborn", "determined", "reckless", "careless"), "determined"),
                    EnglishExercise("3. Which sentence uses formal language?", listOf("The group's gonna check it out.", "Hey, look at this stuff!", "The committee will review the proposal tomorrow.", "That plan is kinda okay."), "The committee will review the proposal tomorrow."),
                    EnglishExercise("4. Which point of view uses \"I\" or \"we\"?", listOf("second person", "third person omniscient", "third person objective", "first person"), "first person"),
                    EnglishExercise("5. Which event should come first in a logical narrative?", listOf("The solution is evaluated.", "The results are reported.", "The learner identifies the problem.", "The conclusion is written."), "The learner identifies the problem."),
                    EnglishExercise("6. What is the climax of a story?", listOf("The list of characters", "The point of greatest tension or turning point", "The opening description only", "The author's biography"), "The point of greatest tension or turning point"),
                    EnglishExercise("7. Which detail best supports the theme \"cooperation solves problems\"?", listOf("Neighbors worked together to repair the bridge.", "The bridge was painted blue.", "The river was wide.", "It rained on Tuesday."), "Neighbors worked together to repair the bridge."),
                    EnglishExercise("8. What does an author's point of view affect?", listOf("The number of pages only", "The font size", "The date of printing", "How events and ideas are presented"), "How events and ideas are presented"),
                    EnglishExercise("9. Which statement is an inference?", listOf("The empty shelves suggest supplies sold quickly.", "The shelves are wooden.", "The sign says OPEN.", "There are four aisles."), "The empty shelves suggest supplies sold quickly."),
                    EnglishExercise("10. What is the purpose of an informational text with steps?", listOf("To entertain with imaginary characters", "To express rhythm only", "To explain how to do something", "To hide information"), "To explain how to do something"),
                    EnglishExercise("11. Which is a fact?", listOf("Summer is the best season.", "This book is beautiful.", "Blue is the happiest color.", "Water freezes at 0°C under standard conditions."), "Water freezes at 0°C under standard conditions."),
                    EnglishExercise("12. Which evidence is most relevant to a claim about school waste?", listOf("A joke about garbage", "Measured amounts of waste collected each day", "A drawing with no data", "An unrelated weather report"), "Measured amounts of waste collected each day"),
                    EnglishExercise("13. Which sentence is compound?", listOf("Lina researched the topic, and Marco organized the notes.", "Lina researched carefully.", "After Lina researched, she wrote.", "Researching the topic, Lina smiled."), "Lina researched the topic, and Marco organized the notes."),
                    EnglishExercise("14. Which sentence is complex?", listOf("The source was reliable.", "We read and took notes.", "Because the source was reliable, we used its data.", "Reliable sources and clear notes."), "Because the source was reliable, we used its data."),
                    EnglishExercise("15. Which transition shows cause and effect?", listOf("meanwhile", "therefore", "for example", "first"), "therefore"),
                    EnglishExercise("16. Which revision improves clarity?", listOf("The council, stuff, did it.", "It was done by them somehow.", "The thing happened there.", "The council approved the project after reviewing the evidence."), "The council approved the project after reviewing the evidence."),
                    EnglishExercise("17. Which headline is most objective?", listOf("Community Opens New Reading Center", "Amazing Center Changes Everything!", "Best Project Ever!", "You Will Not Believe This Center!"), "Community Opens New Reading Center"),
                    EnglishExercise("18. Which statement shows propaganda through bandwagon?", listOf("The product costs ₱50.", "The event begins at 8:00.", "The report lists three findings.", "Everyone is joining, so you should join too."), "Everyone is joining, so you should join too."),
                    EnglishExercise("19. Before sharing an online article, what should a learner do?", listOf("Share it based on the title", "Trust it because it has many likes", "Check the author, evidence, date, and other reliable sources", "Ignore the publication date"), "Check the author, evidence, date, and other reliable sources"),
                    EnglishExercise("20. Which response best evaluates a text?", listOf("I agree because it is long.", "The claim is convincing because two reliable sources provide consistent evidence.", "It must be true because it is online.", "The pictures are colorful, so every claim is correct."), "The claim is convincing because two reliable sources provide consistent evidence.")
                )
            )
        )
    )
)
