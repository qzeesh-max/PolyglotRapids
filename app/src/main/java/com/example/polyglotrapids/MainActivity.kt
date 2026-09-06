package com.example.polyglotrapids

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val game = PolyglotRapidsGame(this)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    var currentScreen by remember { mutableStateOf("rules") }
                    var soundEnabled by remember { mutableStateOf(false) }

                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner, soundEnabled, currentScreen) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_PAUSE) {
                                mediaPlayer?.pause()
                            } else if (event == Lifecycle.Event.ON_RESUME) {
                                if (soundEnabled && currentScreen != "rules") {
                                    mediaPlayer?.start()
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            mediaPlayer?.release()
                            mediaPlayer = null
                        }
                    }

                    LaunchedEffect(soundEnabled, currentScreen) {
                        if (soundEnabled && currentScreen != "rules") {
                            if (mediaPlayer == null) {
                                mediaPlayer = MediaPlayer.create(this@MainActivity, R.raw.river).apply {
                                    isLooping = true
                                    start()
                                }
                            } else if (mediaPlayer?.isPlaying == false) {
                                mediaPlayer?.start()
                            }
                        } else {
                            mediaPlayer?.pause()
                            mediaPlayer?.seekTo(0)
                        }
                    }

                    when (currentScreen) {
                        "rules" -> RulesScreen(
                            onNext = { 
                                soundEnabled = true
                                currentScreen = "languages" 
                            }
                        )
                        "languages" -> LanguageSelectionScreen(
                            onStart = { selectedLangs ->
                                game.startGame(selectedLangs)
                                currentScreen = "game"
                            }
                        )
                        "game" -> GameScreen(
                            game = game,
                            onNewGame = {
                                game.reset()
                                currentScreen = "rules"
                                soundEnabled = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RulesScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    var logoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        try {
            context.assets.open("polyglot_logo.png").use {
                logoBitmap = BitmapFactory.decodeStream(it).asImageBitmap()
            }
        } catch(e: Exception) {}
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        logoBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Logo",
                modifier = Modifier.height(150.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = """
            - Steer your raft by tapping the LEFT and RIGHT sides of the screen.
            - Avoid obstacles: Rocks, Logs, and Shallows damage your raft.
            - Hit green ramps to jump over obstacles.
            - Collect letters to form words from your chosen languages.
            - Forming valid words restores health and boosts your score.
            - The game gets faster the more words you find!
            
            Note: Choose from our top 23 languages! We support 6,000 words per language.
            
            Press 'Next' to select your languages.
            """.trimIndent(),
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onNext) {
            Text("Next")
        }
    }
}

val LANG_CODES = listOf(
    "en", "es", "zh", "hi", "fr", "ar", "bn", "ru", "pt", "id", 
    "ur", "de", "ja", "te", "tr", "ta", "vi", "tl", 
    "ko", "fa", "it", "th", "pl"
)
val LANG_NAMES = listOf(
    "English", "Spanish", "Mandarin", "Hindi", "French", "Arabic", "Bengali", "Russian", "Portuguese", "Indonesian",
    "Urdu", "German", "Japanese", "Telugu", "Turkish", "Tamil", "Vietnamese", "Tagalog",
    "Korean", "Persian", "Italian", "Thai", "Polish"
)

@Composable
fun LanguageSelectionScreen(onStart: (List<String>) -> Unit) {
    val selectedLangs = remember { mutableStateListOf("en") }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Languages", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(LANG_CODES.size) { i ->
                val code = LANG_CODES[i]
                val name = LANG_NAMES[i]
                val isSelected = selectedLangs.contains(code)
                
                val bgColor = if (isSelected) Color(0xFF226622) else Color(0xFF444444)
                val borderColor = if (isSelected) Color.Green else Color.Gray
                
                var flagBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(code) {
                    try {
                        context.assets.open("flags/$code.png").use {
                            flagBitmap = BitmapFactory.decodeStream(it).asImageBitmap()
                        }
                    } catch(e: Exception) {}
                }
                
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .background(bgColor, RoundedCornerShape(4.dp))
                        .border(2.dp, borderColor, RoundedCornerShape(4.dp))
                        .clickable {
                            if (isSelected) selectedLangs.remove(code) else selectedLangs.add(code)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    flagBitmap?.let {
                        Image(bitmap = it, contentDescription = null, modifier = Modifier.size(30.dp, 20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(name, color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (selectedLangs.isEmpty()) selectedLangs.add("en")
                onStart(selectedLangs)
            },
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text("Start Rafting!", fontSize = 18.sp)
        }
    }
}

@Composable
fun GameScreen(game: PolyglotRapidsGame, onNewGame: () -> Unit) {
    var time by remember { mutableStateOf(0f) }
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    LaunchedEffect(game.status) {
        var lastTime = System.nanoTime()
        while (game.status == "playing") {
            withFrameNanos { frameTime ->
                val dt = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                
                if (leftPressed) game.moveRaft(-0.015f)
                if (rightPressed) game.moveRaft(0.015f)
                
                game.update(minOf(dt, 0.05f))
                time += 0.05f
            }
        }
    }
    
    if (game.status == "gameover") {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Game Over!") },
            text = { Text("Score: ${game.score}") },
            confirmButton = {
                Button(onClick = onNewGame) { Text("Play Again") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Game Canvas + Input
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            if (offset.y > size.height / 2) {
                                val isLeft = offset.x < size.width / 2
                                if (isLeft) leftPressed = true else rightPressed = true
                                tryAwaitRelease()
                                leftPressed = false
                                rightPressed = false
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // River
                drawRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF0A64B4), Color(0xFF1478C8))),
                    size = Size(w, h)
                )
                
                // Flowing lines
                for (i in 0 until 15) {
                    val xBase = (w / 16) * (i + 1)
                    val alpha = 20 + (i % 3) * 15
                    val path = Path()
                    var drawing = true
                    for (y in 0 until h.toInt() step 20) {
                        val offset = (sin(time + y * 0.01 + i) * 15).toFloat()
                        if (y == 0) path.moveTo(xBase + offset, y.toFloat())
                        else path.lineTo(xBase + offset, y.toFloat())
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = alpha / 255f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f + (i % 2))
                    )
                }
                
                // Obstacles
                for (obs in game.obstacles) {
                    if (obs.hit && obs.type != 3) continue
                    val ox = obs.x * w
                    val oy = obs.y * h
                    when (obs.type) {
                        0 -> { // Rock
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFAAAAAA), Color(0xFF555555)),
                                    center = Offset(ox - 5, oy - 5),
                                    radius = 20f
                                ),
                                center = Offset(ox, oy),
                                radius = 20f
                            )
                        }
                        1 -> { // Log
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF8B4513), Color(0xFFA0522D), Color(0xFF5C3317)),
                                    startY = oy - 40,
                                    endY = oy + 40
                                ),
                                topLeft = Offset(ox - 10, oy - 40),
                                size = Size(20f, 80f),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                        }
                        2 -> { // Shallows
                            drawCircle(
                                color = Color(0x8087CEFA),
                                center = Offset(ox, oy),
                                radius = 30f
                            )
                        }
                        3 -> { // Ramp
                            val path = Path()
                            path.moveTo(ox - 20, oy + 20)
                            path.lineTo(ox + 20, oy + 20)
                            path.lineTo(ox, oy - 20)
                            path.close()
                            drawPath(path, color = Color(0xFF32CD32))
                            
                            val highPath = Path()
                            highPath.moveTo(ox, oy - 20)
                            highPath.lineTo(ox - 20, oy + 20)
                            highPath.lineTo(ox, oy + 20)
                            highPath.close()
                            drawPath(highPath, color = Color(0xFF7CFC00))
                        }
                    }
                }
                
                // Letters
                for (letter in game.letters) {
                    if (letter.collected) continue
                    val lx = letter.x * w
                    val ly = letter.y * h
                    drawCircle(color = Color(0xFFDAA520).copy(alpha = 0.5f), center = Offset(lx, ly), radius = 35f)
                    drawCircle(color = Color(0xFFFFD700), center = Offset(lx, ly), radius = 35f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#FFD700")
                        textSize = 48f
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(letter.character, lx, ly + 16, textPaint)
                    }
                }
                
                // Raft
                val rx = game.raftX * w
                val ry = 0.9f * h
                
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF5DEB3), Color(0xFF8B4513)),
                        startY = ry - 30, endY = ry + 30
                    ),
                    topLeft = Offset(rx - 20, ry - 30),
                    size = Size(40f, 60f),
                    cornerRadius = CornerRadius(15f, 15f)
                )
                
                drawRoundRect(
                    color = Color(0xFFCD853F),
                    topLeft = Offset(rx - 10, ry - 20),
                    size = Size(20f, 40f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                
                // Boaters
                drawCircle(color = Color.Red, center = Offset(rx, ry - 10), radius = 6f)
                drawCircle(color = Color.Blue, center = Offset(rx, ry + 10), radius = 6f)
            }
        }
        
        // HUD Overlay
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Text("Score: ${game.score}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Health: ${game.health}", color = Color.White, fontSize = 16.sp)
            Box(modifier = Modifier.size(120.dp, 10.dp).background(Color(0xFFAA0000), RoundedCornerShape(5.dp))) {
                Box(modifier = Modifier.size((game.health * 1.2).dp, 10.dp).background(Color(0xFF00AA00), RoundedCornerShape(5.dp)))
            }
        }
        
        // Active Word Overlay
        if (!drawerOpen) {
            val builtWord = game.pendingWord + game.activeWord
            if (builtWord.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(builtWord, color = Color.Yellow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Retractable Drawer
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(100.dp)
                    .align(Alignment.CenterVertically)
                    .background(Color(0xFF333333).copy(alpha = 0.8f), RoundedCornerShape(topStart = 15.dp, bottomStart = 15.dp))
                    .clickable { drawerOpen = !drawerOpen },
                contentAlignment = Alignment.Center
            ) {
                Text(if (drawerOpen) ">" else "<", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            AnimatedVisibility(
                visible = drawerOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                // UI Panel Content
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF333333).copy(alpha = 0.9f))
                        .padding(10.dp)
                ) {
                    Button(
                        onClick = onNewGame,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("New Game")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (game.wordLockTimer > 0) {
                        Text("Lock Timer: %.1fs".format(game.wordLockTimer), color = Color(0xFFFFA500))
                        Box(modifier = Modifier.size(160.dp, 10.dp).background(Color.Gray, RoundedCornerShape(5.dp))) {
                            Box(modifier = Modifier.size(((game.wordLockTimer / 15f) * 160).dp, 10.dp).background(Color.Yellow, RoundedCornerShape(5.dp)))
                        }
                    } else {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Building:", color = Color.Yellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(game.pendingWord + game.activeWord, color = Color.Yellow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Found Words:", color = Color.White, fontSize = 16.sp)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF222222), RoundedCornerShape(5.dp))
                            .padding(8.dp)
                    ) {
                        items(game.foundWordsList.reversed()) { word ->
                            Text(word, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
