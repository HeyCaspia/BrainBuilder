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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

enum class ScienceDifficultyLevel(val label: String) {
    TODDLER("Toddler"), GRADE_3("Grade 3"), GRADE_4("Grade 4"), GRADE_5("Grade 5")
}

enum class ScienceGameMode(val label: String) {
    QUIZ("Quiz"), TIME_TRIAL("Time Trial")
}

data class ScienceQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

data class ScienceTopic(
    val name: String,
    val questions: List<ScienceQuestion>
)

sealed class ScienceScreen {
    object LevelSelection : ScienceScreen()
    data class TopicSelection(val level: ScienceDifficultyLevel) : ScienceScreen()
    data class GameModeSelection(val level: ScienceDifficultyLevel, val topic: ScienceTopic) : ScienceScreen()
    data class Exercise(val level: ScienceDifficultyLevel, val topic: ScienceTopic, val mode: ScienceGameMode) : ScienceScreen()
    data class Result(val score: Int, val total: Int, val level: ScienceDifficultyLevel) : ScienceScreen()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ScienceApp(userName: String, onExit: () -> Unit, onSaveScore: (ScoreRecord) -> Unit) {
    var currentScreen by remember { mutableStateOf<ScienceScreen>(ScienceScreen.LevelSelection) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SCIENCE FOR ALL", fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentScreen) {
                            is ScienceScreen.LevelSelection -> onExit()
                            is ScienceScreen.TopicSelection -> currentScreen = ScienceScreen.LevelSelection
                            is ScienceScreen.GameModeSelection -> currentScreen = ScienceScreen.TopicSelection((currentScreen as ScienceScreen.GameModeSelection).level)
                            is ScienceScreen.Exercise -> currentScreen = ScienceScreen.TopicSelection((currentScreen as ScienceScreen.Exercise).level)
                            is ScienceScreen.Result -> currentScreen = ScienceScreen.LevelSelection
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFF9800),
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
                label = "ScienceTransition"
            ) { screen ->
                when (screen) {
                    is ScienceScreen.LevelSelection -> {
                        ScienceLevelSelectionScreen(userName) { currentScreen = ScienceScreen.TopicSelection(it) }
                    }
                    is ScienceScreen.TopicSelection -> {
                        BackHandler { currentScreen = ScienceScreen.LevelSelection }
                        ScienceTopicSelectionScreen(screen.level) { currentScreen = ScienceScreen.GameModeSelection(screen.level, it) }
                    }
                    is ScienceScreen.GameModeSelection -> {
                        BackHandler { currentScreen = ScienceScreen.TopicSelection(screen.level) }
                        ScienceGameModeSelectionScreen(screen.topic) { currentScreen = ScienceScreen.Exercise(screen.level, screen.topic, it) }
                    }
                    is ScienceScreen.Exercise -> {
                        BackHandler { currentScreen = ScienceScreen.TopicSelection(screen.level) }
                        ScienceExerciseApp(
                            userName = userName,
                            topic = screen.topic,
                            mode = screen.mode,
                            onBack = { currentScreen = ScienceScreen.TopicSelection(screen.level) },
                            onSaveScore = onSaveScore
                        )
                    }
                    is ScienceScreen.Result -> {}
                }
            }
        }
    }
}

@Composable
fun ScienceLevelSelectionScreen(name: String, onLevelSelected: (ScienceDifficultyLevel) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Welcome, $name", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Select your level:", modifier = Modifier.padding(bottom = 32.dp))
        ScienceDifficultyLevel.values().forEach { level ->
            Button(
                onClick = { onLevelSelected(level) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Text(level.label, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun ScienceTopicSelectionScreen(level: ScienceDifficultyLevel, onTopicSelected: (ScienceTopic) -> Unit) {
    val topics = ScienceContent.content[level] ?: emptyList()
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(level.label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Select a topic:", modifier = Modifier.padding(bottom = 32.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(topics) { topic ->
                Button(
                    onClick = { onTopicSelected(topic) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3E0), contentColor = Color(0xFFE65100))
                ) {
                    Text(topic.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScienceGameModeSelectionScreen(topic: ScienceTopic, onModeSelected: (ScienceGameMode) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(topic.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Choose mode:", modifier = Modifier.padding(bottom = 32.dp))
        listOf(ScienceGameMode.QUIZ, ScienceGameMode.TIME_TRIAL).forEach { mode ->
            Button(
                onClick = { onModeSelected(mode) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Text(mode.label, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun ScienceExerciseApp(userName: String, topic: ScienceTopic, mode: ScienceGameMode, onBack: () -> Unit, onSaveScore: (ScoreRecord) -> Unit) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(15) }
    var timeSpentSeconds by remember { mutableStateOf(0) }

    val currentQuestion = topic.questions[currentQuestionIndex]

    LaunchedEffect(showResult) {
        if (showResult) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            onSaveScore(ScoreRecord(userName, "SCIENCE", "${topic.name} (${mode.label})", score, topic.questions.size, sdf.format(Date())))
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

    if (mode == ScienceGameMode.TIME_TRIAL && !showResult) {
        LaunchedEffect(currentQuestionIndex, timeLeft) {
            if (timeLeft > 0) {
                delay(1000)
                timeLeft--
            } else {
                if (currentQuestionIndex < topic.questions.size - 1) {
                    currentQuestionIndex++
                    timeLeft = 15
                    selectedOption = null
                    showExplanation = false
                } else {
                    showResult = true
                }
            }
        }
    }

    if (showResult) {
        ResultsScreen(
            name = userName,
            score = score,
            total = topic.questions.size,
            modeName = "${topic.name} (${mode.label})",
            mistakes = emptyList(),
            timeSpent = timeSpentSeconds,
            onRestart = onBack
        )
    } else {
        Column {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E0)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Examinee: $userName", fontWeight = FontWeight.Bold)
                Text("Category: ${topic.name} - ${mode.label}", style = MaterialTheme.typography.bodySmall)
                if (mode == ScienceGameMode.TIME_TRIAL) {
                    Text("Time: ${timeLeft}s", color = if (timeLeft < 5) Color.Red else Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Question ${currentQuestionIndex + 1} of ${topic.questions.size}")
                
                val displayFontSize = if (currentQuestion.question.length > 30) 20.sp else 28.sp
                Text(
                    text = currentQuestion.question,
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = displayFontSize * 1.2f,
                    modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth()
                )

                currentQuestion.options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == index
                    val color = when {
                        (showExplanation || selectedOption != null) && index == currentQuestion.correctAnswerIndex -> Color(0xFF4CAF50)
                        isSelected && index != currentQuestion.correctAnswerIndex -> Color(0xFFF44336)
                        else -> Color.White
                    }

                    OutlinedButton(
                        onClick = {
                            if (selectedOption == null) {
                                selectedOption = index
                                if (index == currentQuestion.correctAnswerIndex) score++
                                showExplanation = true
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

                if (showExplanation || selectedOption != null) {
                    if (showExplanation) {
                        Text(
                            text = currentQuestion.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (currentQuestionIndex < topic.questions.size - 1) {
                                currentQuestionIndex++
                                selectedOption = null
                                showExplanation = false
                                timeLeft = 15
                            } else {
                                showResult = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text(if (currentQuestionIndex < topic.questions.size - 1) "NEXT" else "FINISH")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(0.5f)) { Text("GO BACK") }
            }
        }
    }
}

object ScienceContent {
    val content = mapOf(
        ScienceDifficultyLevel.TODDLER to listOf(
            ScienceTopic("Animals", listOf(
                ScienceQuestion(1, "What sound does a cow make?", listOf("Meow", "Moo", "Woof", "Oink"), 1, "Cows say Moo!"),
                ScienceQuestion(2, "Which animal has a long trunk?", listOf("Cat", "Dog", "Elephant", "Mouse"), 2, "Elephants use trunks to drink and eat."),
                ScienceQuestion(3, "Where does a fish live?", listOf("Tree", "House", "Water", "Cloud"), 2, "Fish breathe underwater!"),
                ScienceQuestion(4, "What animal is yellow and says 'Quack'?", listOf("Duck", "Lion", "Bear", "Pig"), 0, "Ducks love to swim and quack."),
                ScienceQuestion(5, "Which animal is very tall?", listOf("Ant", "Giraffe", "Rabbit", "Turtle"), 1, "Giraffes have long necks to reach leaves."),
                ScienceQuestion(6, "What animal hops?", listOf("Snake", "Frog", "Fish", "Bird"), 1, "Frogs hop on lily pads."),
                ScienceQuestion(7, "Who has feathers?", listOf("Dog", "Bird", "Cat", "Cow"), 1, "Birds use feathers to fly."),
                ScienceQuestion(8, "What animal gives us milk?", listOf("Lion", "Tiger", "Cow", "Wolf"), 2, "Cows provide healthy milk."),
                ScienceQuestion(9, "Which animal is small and loves cheese?", listOf("Elephant", "Mouse", "Horse", "Bear"), 1, "Mice are tiny and like cheese."),
                ScienceQuestion(10, "What animal says 'Woof'?", listOf("Cat", "Dog", "Bird", "Pig"), 1, "Dogs are man's best friend.")
            )),
            ScienceTopic("The Five Senses", listOf(
                ScienceQuestion(1, "What do you use to see?", listOf("Ears", "Eyes", "Nose", "Hands"), 1, "Eyes are for seeing!"),
                ScienceQuestion(2, "What do you use to smell a flower?", listOf("Nose", "Mouth", "Eyes", "Ears"), 0, "Noses smell pretty flowers."),
                ScienceQuestion(3, "What do you use to hear music?", listOf("Eyes", "Nose", "Ears", "Hands"), 2, "Ears listen to sounds."),
                ScienceQuestion(4, "What do you use to taste an apple?", listOf("Nose", "Tongue", "Eyes", "Hands"), 1, "The tongue tastes flavors."),
                ScienceQuestion(5, "What do you use to feel a soft teddy bear?", listOf("Hands", "Eyes", "Ears", "Nose"), 0, "Skin and hands are for touching."),
                ScienceQuestion(6, "Which sense helps you find a bird singing?", listOf("Sight", "Hearing", "Taste", "Smell"), 1, "Hearing helps you find sounds."),
                ScienceQuestion(7, "Which sense helps you see a rainbow?", listOf("Sight", "Smell", "Touch", "Taste"), 0, "Sight lets you see colors."),
                ScienceQuestion(8, "Is ice hot or cold to touch?", listOf("Hot", "Cold", "Spicy", "Sweet"), 1, "Ice feels very cold!"),
                ScienceQuestion(9, "Which sense do you use to know if a lemon is sour?", listOf("Sight", "Taste", "Hearing", "Smell"), 1, "Taste tells us if something is sour."),
                ScienceQuestion(10, "Can you hear with your eyes?", listOf("Yes", "No"), 1, "No, we hear with our ears.")
            )),
            ScienceTopic("Weather", listOf(
                ScienceQuestion(1, "What is yellow and shines in the sky?", listOf("Moon", "Star", "Sun", "Cloud"), 2, "The Sun gives us light and heat."),
                ScienceQuestion(2, "What falls from the sky when it's raining?", listOf("Snow", "Rocks", "Water", "Leaves"), 2, "Rain is water falling from clouds."),
                ScienceQuestion(3, "What is white and fluffy in the sky?", listOf("Sun", "Cloud", "Bird", "Plane"), 1, "Clouds are made of tiny water drops."),
                ScienceQuestion(4, "What do you use when it rains?", listOf("Sunglasses", "Umbrella", "Hat", "Gloves"), 1, "Umbrellas keep us dry."),
                ScienceQuestion(5, "What is very cold and white that falls in winter?", listOf("Rain", "Snow", "Dust", "Sand"), 1, "Snow is frozen water."),
                ScienceQuestion(6, "What makes the trees move?", listOf("Sun", "Rain", "Wind", "Clouds"), 2, "Wind is moving air."),
                ScienceQuestion(7, "Where does the sun go at night?", listOf("Sleeps", "Other side of Earth", "Inside a cloud", "Disappears"), 1, "The Earth spins away from the Sun."),
                ScienceQuestion(8, "What color is the sky on a clear day?", listOf("Green", "Red", "Blue", "Purple"), 2, "The sky looks blue on sunny days."),
                ScienceQuestion(9, "What do we wear when it's very cold?", listOf("Swimsuit", "T-shirt", "Coat", "Shorts"), 2, "Coats keep us warm."),
                ScienceQuestion(10, "A rainbow has many...?", listOf("Sounds", "Colors", "Smells", "Tastes"), 1, "Rainbows have seven colors.")
            ))
        ),
        ScienceDifficultyLevel.GRADE_3 to listOf(
            ScienceTopic("Plants", listOf(
                ScienceQuestion(1, "What part of the plant grows underground?", listOf("Stem", "Leaves", "Roots", "Flower"), 2, "Roots absorb water and nutrients."),
                ScienceQuestion(2, "What do plants need to make their own food?", listOf("Sugar", "Sunlight", "Milk", "Juice"), 1, "Plants use photosynthesis."),
                ScienceQuestion(3, "Which part of the plant makes seeds?", listOf("Roots", "Stem", "Flower", "Leaves"), 2, "Flowers are for reproduction."),
                ScienceQuestion(4, "What is the green substance in leaves called?", listOf("Paint", "Chlorophyll", "Sap", "Water"), 1, "Chlorophyll traps sunlight."),
                ScienceQuestion(5, "Which part of the plant carries water to the leaves?", listOf("Roots", "Stem", "Flower", "Fruit"), 1, "The stem acts like a straw."),
                ScienceQuestion(6, "Plants breathe out which gas that we need?", listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Helium"), 0, "Plants produce oxygen during photosynthesis."),
                ScienceQuestion(7, "What is a baby plant called?", listOf("Seedling", "Sprout", "Leaflet", "Rootlet"), 0, "A seedling is a young plant."),
                ScienceQuestion(8, "Which of these is a decomposer?", listOf("Grass", "Lion", "Mushroom", "Eagle"), 2, "Fungi like mushrooms break down dead matter."),
                ScienceQuestion(9, "Do all plants have flowers?", listOf("Yes", "No"), 1, "Some plants, like ferns and mosses, don't have flowers."),
                ScienceQuestion(10, "What do bees help plants with?", listOf("Eating", "Pollination", "Watering", "Growing roots"), 1, "Bees move pollen from flower to flower.")
            )),
            ScienceTopic("Matter", listOf(
                ScienceQuestion(1, "Which state of matter has a definite shape?", listOf("Liquid", "Gas", "Solid", "Plasma"), 2, "Solids keep their shape."),
                ScienceQuestion(2, "What happens to water when it freezes?", listOf("Becomes gas", "Becomes solid", "Disappears", "Stays liquid"), 1, "Freezing turns liquid water into ice (solid)."),
                ScienceQuestion(3, "Which state of matter fills any container it is in?", listOf("Solid", "Liquid", "Gas", "Stone"), 2, "Gases expand to fill space."),
                ScienceQuestion(4, "What is the process of a liquid turning into gas called?", listOf("Freezing", "Melting", "Evaporation", "Condensation"), 2, "Evaporation happens when water heats up."),
                ScienceQuestion(5, "Everything around us that takes up space is...?", listOf("Energy", "Matter", "Air", "Light"), 1, "Matter is anything that has mass and volume."),
                ScienceQuestion(6, "Which of these is a liquid?", listOf("Ice", "Steam", "Milk", "Rock"), 2, "Milk flows and takes the shape of its container."),
                ScienceQuestion(7, "When ice melts, it becomes a...?", listOf("Solid", "Liquid", "Gas", "Cloud"), 1, "Melting turns solids into liquids."),
                ScienceQuestion(8, "What is the tiny unit that matter is made of?", listOf("Cell", "Atom", "Grain", "Drop"), 1, "Atoms are the building blocks of matter."),
                ScienceQuestion(9, "Can we see all types of gas?", listOf("Yes", "No"), 1, "Many gases, like the air we breathe, are invisible."),
                ScienceQuestion(10, "Which state of matter is steam?", listOf("Solid", "Liquid", "Gas", "Ice"), 2, "Steam is water in its gas state.")
            )),
            ScienceTopic("Solar System", listOf(
                ScienceQuestion(1, "Which is the center of our solar system?", listOf("Earth", "Moon", "Sun", "Mars"), 2, "All planets orbit the Sun."),
                ScienceQuestion(2, "Which planet is known as the Red Planet?", listOf("Venus", "Mars", "Jupiter", "Saturn"), 1, "Mars has iron-rich dust that looks red."),
                ScienceQuestion(3, "How many planets are in our solar system?", listOf("7", "8", "9", "10"), 1, "There are eight major planets."),
                ScienceQuestion(4, "Which is the largest planet?", listOf("Earth", "Saturn", "Jupiter", "Neptune"), 2, "Jupiter is a gas giant and the biggest planet."),
                ScienceQuestion(5, "Which planet is closest to the Sun?", listOf("Venus", "Mercury", "Earth", "Mars"), 1, "Mercury has the shortest orbit."),
                ScienceQuestion(6, "What is the Earth's only natural satellite?", listOf("Sun", "Star", "Moon", "Mars"), 2, "The Moon orbits the Earth."),
                ScienceQuestion(7, "Which planet is famous for its beautiful rings?", listOf("Jupiter", "Saturn", "Uranus", "Neptune"), 1, "Saturn's rings are made of ice and rock."),
                ScienceQuestion(8, "Is the Sun a planet or a star?", listOf("Planet", "Star"), 1, "The Sun is a medium-sized star."),
                ScienceQuestion(9, "Which planet do we live on?", listOf("Mars", "Venus", "Earth", "Jupiter"), 2, "Earth is the third planet from the Sun."),
                ScienceQuestion(10, "What force keeps planets in orbit?", listOf("Magnetism", "Gravity", "Wind", "Friction"), 1, "Sun's gravity keeps planets from drifting away.")
            ))
        ),
        ScienceDifficultyLevel.GRADE_4 to listOf(
            ScienceTopic("Life Cycles", listOf(
                ScienceQuestion(1, "What is the first stage of a butterfly's life cycle?", listOf("Larva", "Pupa", "Egg", "Adult"), 2, "It starts as a tiny egg."),
                ScienceQuestion(2, "A frog starts its life as a...?", listOf("Tadpole", "Egg", "Froglet", "Fish"), 1, "Frogs lay eggs in water."),
                ScienceQuestion(3, "What is the process of a caterpillar changing into a butterfly?", listOf("Growth", "Metamorphosis", "Evolution", "Photosynthesis"), 1, "Metamorphosis means a complete change in form."),
                ScienceQuestion(4, "Which stage comes after the egg in a bird's life cycle?", listOf("Adult", "Chick", "Fledgling", "Hatchling"), 3, "A hatchling just came out of the egg."),
                ScienceQuestion(5, "What do we call the stage where an insect is inside a cocoon?", listOf("Larva", "Pupa", "Nymph", "Adult"), 1, "The pupa stage is the transformation period."),
                ScienceQuestion(6, "Plants start their life cycle as a...?", listOf("Leaf", "Seed", "Stem", "Flower"), 1, "Most plants grow from seeds."),
                ScienceQuestion(7, "Which animal undergoes incomplete metamorphosis?", listOf("Butterfly", "Grasshopper", "Frog", "Bee"), 1, "Grasshoppers have nymph stages instead of larvae/pupae."),
                ScienceQuestion(8, "How many stages are in complete metamorphosis?", listOf("2", "3", "4", "5"), 2, "Egg, Larva, Pupa, Adult."),
                ScienceQuestion(9, "A young frog that still has a tail is a...?", listOf("Toad", "Tadpole", "Newt", "Fish"), 1, "Tadpoles breathe with gills and have tails."),
                ScienceQuestion(10, "Do all animals have the same life cycle?", listOf("Yes", "No"), 1, "Different species have different stages of growth.")
            )),
            ScienceTopic("Energy", listOf(
                ScienceQuestion(1, "What type of energy does a moving ball have?", listOf("Potential", "Kinetic", "Thermal", "Chemical"), 1, "Kinetic energy is the energy of motion."),
                ScienceQuestion(2, "Energy stored in a battery is...?", listOf("Light", "Sound", "Chemical", "Electrical"), 2, "Chemical energy is stored and then converted."),
                ScienceQuestion(3, "What is the primary source of energy for Earth?", listOf("Electricity", "The Sun", "Coal", "Wind"), 1, "The Sun provides light and heat."),
                ScienceQuestion(4, "Which of these is a renewable energy source?", listOf("Coal", "Oil", "Wind", "Natural Gas"), 2, "Wind is never used up."),
                ScienceQuestion(5, "Energy can be created or destroyed.", listOf("True", "False"), 1, "Energy can only be transformed from one form to another."),
                ScienceQuestion(6, "What kind of energy is produced by vibrating objects?", listOf("Light", "Heat", "Sound", "Magnetic"), 2, "Sound is made by vibrations."),
                ScienceQuestion(7, "A toaster converts electrical energy into...?", listOf("Light", "Sound", "Heat", "Mechanical"), 2, "Toasters use heat to brown bread."),
                ScienceQuestion(8, "Which of these is a non-renewable resource?", listOf("Solar", "Hydroelectric", "Coal", "Geothermal"), 2, "Coal takes millions of years to form."),
                ScienceQuestion(9, "What energy does a stretched rubber band have?", listOf("Kinetic", "Potential", "Electrical", "Light"), 1, "Stored energy is potential energy."),
                ScienceQuestion(10, "What device turns sunlight into electricity?", listOf("Generator", "Solar Panel", "Battery", "Turbine"), 1, "Solar panels capture photon energy.")
            )),
            ScienceTopic("Weather & Climate", listOf(
                ScienceQuestion(1, "What instrument measures temperature?", listOf("Barometer", "Anemometer", "Thermometer", "Rain Gauge"), 2, "Thermometers measure degrees."),
                ScienceQuestion(2, "What is the water cycle's process where water turns to clouds?", listOf("Evaporation", "Condensation", "Precipitation", "Runoff"), 1, "Condensation forms clouds."),
                ScienceQuestion(3, "Which layer of the atmosphere do we live in?", listOf("Stratosphere", "Mesosphere", "Troposphere", "Exosphere"), 2, "The troposphere is where weather happens."),
                ScienceQuestion(4, "A scientist who studies weather is a...?", listOf("Geologist", "Meteorologist", "Biologist", "Astronomer"), 1, "Meteorologists predict the weather."),
                ScienceQuestion(5, "What causes the seasons?", listOf("Distance from Sun", "Earth's tilt", "Wind", "Ocean currents"), 1, "Earth's tilt causes different parts to get more sun."),
                ScienceQuestion(6, "What is moving air called?", listOf("Rain", "Wind", "Cloud", "Storm"), 1, "Wind is the movement of air masses."),
                ScienceQuestion(7, "What type of cloud is thin and wispy?", listOf("Cumulus", "Stratus", "Cirrus", "Nimbus"), 2, "Cirrus clouds are high and icy."),
                ScienceQuestion(8, "What is the average weather over a long time called?", listOf("Forecast", "Climate", "Season", "Temperature"), 1, "Climate is the long-term pattern."),
                ScienceQuestion(9, "Which instrument measures wind speed?", listOf("Anemometer", "Barometer", "Thermometer", "Hygrometer"), 0, "Anemometers spin to measure speed."),
                ScienceQuestion(10, "What is the sudden discharge of electricity in the sky?", listOf("Thunder", "Lightning", "Rain", "Hail"), 1, "Lightning is a giant spark.")
            ))
        ),
        ScienceDifficultyLevel.GRADE_5 to listOf(
            ScienceTopic("Human Body Systems", listOf(
                ScienceQuestion(1, "Which system pumps blood around the body?", listOf("Respiratory", "Digestive", "Circulatory", "Nervous"), 2, "The heart is the center of the circulatory system."),
                ScienceQuestion(2, "Where does digestion begin?", listOf("Stomach", "Small Intestine", "Mouth", "Esophagus"), 2, "Saliva starts breaking down food in the mouth."),
                ScienceQuestion(3, "Which organ is the control center of the body?", listOf("Heart", "Lungs", "Brain", "Liver"), 2, "The brain processes all information."),
                ScienceQuestion(4, "What is the main organ of the respiratory system?", listOf("Kidneys", "Lungs", "Stomach", "Heart"), 1, "Lungs exchange oxygen and carbon dioxide."),
                ScienceQuestion(5, "How many bones are in an adult human body?", listOf("150", "206", "300", "250"), 1, "Adults have 206 bones; babies have more!"),
                ScienceQuestion(6, "Which system provides the framework for our body?", listOf("Muscular", "Skeletal", "Integumentary", "Endocrine"), 1, "The skeletal system supports and protects."),
                ScienceQuestion(7, "What are the small tubes that carry blood away from the heart?", listOf("Veins", "Arteries", "Capillaries", "Nerves"), 1, "Arteries carry oxygenated blood out."),
                ScienceQuestion(8, "What do red blood cells carry?", listOf("Nutrients", "Waste", "Oxygen", "Water"), 2, "They carry oxygen to all cells."),
                ScienceQuestion(9, "Which organ filters waste from the blood?", listOf("Liver", "Pancreas", "Kidneys", "Spleen"), 2, "Kidneys produce urine from waste."),
                ScienceQuestion(10, "What are the building blocks of the human body?", listOf("Tissues", "Organs", "Cells", "Systems"), 2, "Cells are the smallest functional units.")
            )),
            ScienceTopic("Ecosystems", listOf(
                ScienceQuestion(1, "What do we call an animal that only eats plants?", listOf("Carnivore", "Herbivore", "Omnivore", "Decomposer"), 1, "Herbivores get energy from plants."),
                ScienceQuestion(2, "A group of the same species living in an area is a...?", listOf("Community", "Population", "Ecosystem", "Biome"), 1, "Population refers to one species."),
                ScienceQuestion(3, "Which of these is an abiotic factor?", listOf("Trees", "Bacteria", "Sunlight", "Insects"), 2, "Abiotic factors are non-living components."),
                ScienceQuestion(4, "In a food chain, what are plants usually called?", listOf("Consumers", "Producers", "Decomposers", "Predators"), 1, "Producers make their own energy."),
                ScienceQuestion(5, "What is the place where an organism lives called?", listOf("Niche", "Habitat", "Environment", "Zone"), 1, "A habitat provides food, water, and shelter."),
                ScienceQuestion(6, "What do we call the role an organism has in its environment?", listOf("Job", "Niche", "Habit", "Level"), 1, "A niche is how an organism survives."),
                ScienceQuestion(7, "Which organism breaks down dead plants and animals?", listOf("Scavenger", "Producer", "Decomposer", "Parasite"), 2, "Decomposers recycle nutrients."),
                ScienceQuestion(8, "What is a network of overlapping food chains?", listOf("Food Web", "Energy Pyramid", "Ecosystem", "Biome"), 0, "Food webs show complex relationships."),
                ScienceQuestion(9, "Which biome is characterized by very little rainfall?", listOf("Rainforest", "Tundra", "Desert", "Grassland"), 2, "Deserts are extremely dry."),
                ScienceQuestion(10, "What happens to energy as it moves up the food pyramid?", listOf("Increases", "Decreases", "Stays the same", "Doubles"), 1, "Only about 10% of energy is passed to the next level.")
            )),
            ScienceTopic("Space Exploration", listOf(
                ScienceQuestion(1, "Who was the first person to walk on the moon?", listOf("Yuri Gagarin", "Buzz Aldrin", "Neil Armstrong", "John Glenn"), 2, "Neil Armstrong said 'One small step for man'."),
                ScienceQuestion(2, "What is the force that pulls objects toward each other?", listOf("Friction", "Magnetism", "Gravity", "Inertia"), 2, "Gravity keeps us on the ground and planets in orbit."),
                ScienceQuestion(3, "What is a large system of stars, gas, and dust called?", listOf("Solar System", "Galaxy", "Universe", "Constellation"), 1, "We live in the Milky Way galaxy."),
                ScienceQuestion(4, "Which telescope was launched into space in 1990?", listOf("James Webb", "Hubble", "Galileo", "Kepler"), 1, "Hubble has provided amazing photos of deep space."),
                ScienceQuestion(5, "What do we call a person who travels in space?", listOf("Pilot", "Astronomer", "Astronaut", "Scientist"), 2, "Astronauts undergo intense training."),
                ScienceQuestion(6, "What is the name of the first artificial satellite?", listOf("Apollo 11", "Sputnik 1", "Explorer 1", "Voyager 1"), 1, "Sputnik was launched by the USSR in 1957."),
                ScienceQuestion(7, "Which planet is often called the morning star?", listOf("Mars", "Venus", "Jupiter", "Saturn"), 1, "Venus is the brightest planet in the night sky."),
                ScienceQuestion(8, "What is a 'shooting star' actually?", listOf("A falling star", "A comet", "A meteor", "A planet"), 2, "Meteors are bits of rock burning in the atmosphere."),
                ScienceQuestion(9, "Which planet has the Great Red Spot?", listOf("Mars", "Jupiter", "Saturn", "Neptune"), 1, "The Great Red Spot is a giant storm."),
                ScienceQuestion(10, "The path an object takes around another in space is called?", listOf("Road", "Circuit", "Orbit", "Spin"), 2, "Planets orbit the Sun.")
            ))
        )
    )
}
