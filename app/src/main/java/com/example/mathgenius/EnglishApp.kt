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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
            // Add Random Reading Comprehension option if level has comprehension topics
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
fun EnglishExerciseApp(userName: String, topic: EnglishTopic, onBack: () -> Unit, onSaveScore: (ScoreRecord) -> Unit) {
    var currentExerciseIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var timeSpentSeconds by remember { mutableStateOf(0) }

    val exercises = remember(topic) { 
        topic.exercises.shuffled().take(10).map { it.copy(options = it.options.shuffled()) }
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
            mistakes = emptyList(),
            timeSpent = timeSpentSeconds,
            onRestart = onBack
        )
    } else {
        val exercise = exercises[currentExerciseIndex]
        Column {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFE3F2FD)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Examinee: $userName", fontWeight = FontWeight.Bold)
                Text("Category: ${topic.name}", style = MaterialTheme.typography.bodySmall)
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
                    val color = when {
                        isAnswerCorrect != null && option == exercise.correctAnswer -> Color(0xFF4CAF50)
                        isAnswerCorrect != null && isSelected && isAnswerCorrect == false -> Color(0xFFF44336)
                        isSelected -> Color(0xFF2196F3).copy(alpha = 0.7f)
                        else -> Color.White
                    }
                    
                    OutlinedButton(
                        onClick = {
                            if (selectedOption == null) {
                                selectedOption = option
                                val correct = option == exercise.correctAnswer
                                isAnswerCorrect = correct
                                if (correct) score++
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
                EnglishExercise("The apple is ____.", listOf("Red", "Blue", "Black"), "Red")
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
                EnglishExercise("How many syllables are in the word 'Apple'?", listOf("1", "2", "3"), "2")
            )),
            EnglishTopic("grammar_logic", "Grammar & Logic 💡", listOf(
                EnglishExercise("Which word should always start with a capital letter?", listOf("monday", "apple", "school"), "monday"),
                EnglishExercise("Which sentence is correct?", listOf("The dog bark.", "The dog barks.", "The dog barking."), "The dog barks.")
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
                passage = "Honeybees are incredible insects that play a vital role in our environment. They live in large groups called colonies, usually found in hives. A single hive can have thousands of bees, including one queen, many workers, and a few drones. Workers fly from flower to flower, collecting nectar and pollen. As they do this, they help pollinate plants, which allows fruits and vegetables to grow. Without honeybees, our food supply would be greatly affected.",
                exercises = listOf(
                    EnglishExercise("What is a group of bees called?", listOf("A herd", "A colony", "A pack"), "A colony"),
                    EnglishExercise("Which bee is the leader of the hive?", listOf("The worker", "The drone", "The queen"), "The queen"),
                    EnglishExercise("What do bees collect from flowers?", listOf("Nectar and pollen", "Water", "Honey"), "Nectar and pollen"),
                    EnglishExercise("Why is pollination important?", listOf("It makes flowers smell good", "It helps fruits grow", "It keeps bees warm"), "It helps fruits grow"),
                    EnglishExercise("Where do honeybees usually live?", listOf("In nests", "In hives", "Underground"), "In hives")
                )
            ),
            EnglishTopic(
                id = "reading_comp_g3_2",
                name = "Reading Comprehension: The Water Cycle 💧",
                passage = "The water cycle is the path that all water follows as it moves around Earth in different states. Liquid water is found in oceans, rivers, lakes, and even underground. It can be solid ice in glaciers and snow. It can also be a gas called water vapor in the atmosphere. The cycle has four main stages: evaporation, condensation, precipitation, and collection. This continuous process has been going on for billions of years, ensuring that Earth's water is recycled and distributed.",
                exercises = listOf(
                    EnglishExercise("What is water vapor?", listOf("Solid water", "Liquid water", "Gaseous water"), "Gaseous water"),
                    EnglishExercise("Which stage turns water into clouds?", listOf("Evaporation", "Condensation", "Precipitation"), "Condensation"),
                    EnglishExercise("Where is most liquid water found?", listOf("Glaciers", "Oceans", "The atmosphere"), "Oceans"),
                    EnglishExercise("What is precipitation?", listOf("Water turning to gas", "Rain or snow falling", "Water collection"), "Rain or snow falling"),
                    EnglishExercise("How long has the water cycle existed?", listOf("Thousands of years", "Millions of years", "Billions of years"), "Billions of years")
                )
            ),
            EnglishTopic("nouns", "Noun Hunt 🕵️‍♂️", listOf(
                EnglishExercise("Which of these is a PROPER noun?", listOf("city", "Paris", "building"), "Paris"),
                EnglishExercise("Identify the noun: 'The happiness spread through the room.'", listOf("Spread", "Happiness", "Through"), "Happiness")
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
                passage = "An ecosystem is a community of living organisms interacting with their non-living environment. Living things, like plants and animals, are biotic factors, while non-living things, like sunlight, water, and soil, are abiotic factors. Every part of an ecosystem has a role to play. For example, producers (plants) use sunlight to make food, while consumers (animals) eat plants or other animals. Decomposers break down dead matter, returning nutrients to the soil. If one part of the ecosystem is removed, the entire balance can be disrupted.",
                exercises = listOf(
                    EnglishExercise("What are biotic factors?", listOf("Living things", "Non-living things", "Only plants"), "Living things"),
                    EnglishExercise("Which of these is an abiotic factor?", listOf("Bacteria", "Sunlight", "Fungi"), "Sunlight"),
                    EnglishExercise("What do producers use to make food?", listOf("Water only", "Sunlight", "Other plants"), "Sunlight"),
                    EnglishExercise("What is the role of decomposers?", listOf("To hunt animals", "To return nutrients to soil", "To make oxygen"), "To return nutrients to soil"),
                    EnglishExercise("What happens if one part is removed?", listOf("Nothing happens", "The balance is disrupted", "Everything grows faster"), "The balance is disrupted")
                )
            ),
            EnglishTopic(
                id = "reading_comp_g4_2",
                name = "Reading Comprehension: States of Matter 🧪",
                passage = "Everything in the universe that has mass and takes up space is matter. Matter exists in three main states on Earth: solid, liquid, and gas. Solids have a definite shape and volume. Liquids have a definite volume but take the shape of their container. Gases have no definite shape or volume and spread out to fill any space. Changing temperature can cause matter to change from one state to another, such as ice melting into water or water boiling into steam.",
                exercises = listOf(
                    EnglishExercise("What defines matter?", listOf("Something that moves", "Mass and volume", "Only solids"), "Mass and volume"),
                    EnglishExercise("Which state takes the shape of its container?", listOf("Solid", "Liquid", "Gas"), "Liquid"),
                    EnglishExercise("Which state has no definite volume?", listOf("Solid", "Liquid", "Gas"), "Gas"),
                    EnglishExercise("What causes matter to change state?", listOf("Pressure only", "Temperature changes", "Color changes"), "Temperature changes"),
                    EnglishExercise("What is steam an example of?", listOf("Solid", "Liquid", "Gas"), "Gas")
                )
            ),
            EnglishTopic("adjectives", "Describing Words ✨", listOf(
                EnglishExercise("Which is the superlative form of 'Fast'?", listOf("Fasters", "Fastest", "More fast"), "Fastest")
            ))
        )
    ),
    EnglishLevel(
        id = "grade6",
        name = "Grade 6 🎓",
        topics = listOf(
            EnglishTopic(
                id = "reading_comp_g6_1",
                name = "Reading Comprehension: Genetics 🧬",
                passage = "Genetics is the branch of biology concerned with the study of genes, genetic variation, and heredity in organisms. DNA, or deoxyribonucleic acid, is the molecule that carries genetic instructions for the development, functioning, growth, and reproduction of all known organisms. The Human Genome Project, completed in 2003, was an international research effort to determine the DNA sequence of the entire human genome. This breakthrough has revolutionized medicine, allowing scientists to identify genetic predispositions to diseases and develop personalized treatments.",
                exercises = listOf(
                    EnglishExercise("What does DNA stand for?", listOf("Deoxyribonucleic acid", "Dynamic Natural Acid", "Digital Neural Array"), "Deoxyribonucleic acid"),
                    EnglishExercise("What was the goal of the Human Genome Project?", listOf("To cure all cancers", "To sequence the human genome", "To create new species"), "To sequence the human genome"),
                    EnglishExercise("When was the Human Genome Project completed?", listOf("1999", "2003", "2010"), "2003"),
                    EnglishExercise("How has sequencing the genome affected medicine?", listOf("It made medicine cheaper", "It allowed for personalized treatments", "It removed the need for doctors"), "It allowed for personalized treatments"),
                    EnglishExercise("What is heredity?", listOf("The study of weather", "The passing of traits from parents", "A type of plant growth"), "The passing of traits from parents")
                )
            ),
            EnglishTopic(
                id = "reading_comp_g6_2",
                name = "Reading Comprehension: Climate Change 🌡️",
                passage = "Climate change refers to long-term shifts in temperatures and weather patterns. These shifts may be natural, but since the 1800s, human activities have been the main driver of climate change, primarily due to the burning of fossil fuels like coal, oil, and gas. Burning fossil fuels generates greenhouse gas emissions that act like a blanket wrapped around the Earth, trapping the sun’s heat and raising temperatures. Consequences of climate change include intense droughts, water scarcity, severe fires, rising sea levels, and catastrophic storms.",
                exercises = listOf(
                    EnglishExercise("What is the main driver of modern climate change?", listOf("Natural cycles", "Human activities", "Volcanic eruptions"), "Human activities"),
                    EnglishExercise("How do greenhouse gases affect the Earth?", listOf("They block sunlight", "They trap heat", "They cool the atmosphere"), "They trap heat"),
                    EnglishExercise("Which is a consequence of climate change?", listOf("More forests", "Rising sea levels", "Lower temperatures"), "Rising sea levels"),
                    EnglishExercise("Since when have humans been the main driver?", listOf("1700s", "1800s", "1900s"), "1800s"),
                    EnglishExercise("What acts like a 'blanket' around Earth?", listOf("Clouds", "Greenhouse gas emissions", "The ozone layer"), "Greenhouse gas emissions")
                )
            ),
            EnglishTopic("grammar", "Grammar Master 👑", listOf(
                EnglishExercise("Which sentence is in the PRESENT PERFECT tense?", listOf("I go.", "I went.", "I have gone."), "I have gone.")
            ))
        )
    )
)
