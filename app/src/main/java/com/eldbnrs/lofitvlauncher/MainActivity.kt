package com.eldbnrs.lofitvlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

val PixelFont = FontFamily(Font(R.font.pixel_font))

data class AppItem(
    val name: String,
    val packageName: String,
    val launchIntent: Intent?
)

data class AmbientScene(
    val title: String,
    val quoteMain: String,
    val quoteHighlight: String,
    val author: String,
    val imageUrl: String,
    val streamUrl: String,
    val gradientColors: List<Color>
)

val LOFI_SCENES = listOf(
    AmbientScene(
        title = "Cozy Autumn Night",
        quoteMain = "We're all just walking each other ",
        quoteHighlight = "home.",
        author = "- RAM DASS",
        imageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=1920&q=80",
        streamUrl = "https://ice2.somafm.com/groovesalad-128-mp3",
        gradientColors = listOf(Color(0xFF101B2B), Color(0xFF070B14))
    ),
    AmbientScene(
        title = "Midnight Chill Synth",
        quoteMain = "Either you run the day or the day runs ",
        quoteHighlight = "you.",
        author = "- JIM ROHN",
        imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1920&q=80",
        streamUrl = "https://stream.nightride.fm/chillsynth.m4a",
        gradientColors = listOf(Color(0xFF0F2027), Color(0xFF203A43))
    ),
    AmbientScene(
        title = "Desert Starlight",
        quoteMain = "Almost everything will work again if you unplug ",
        quoteHighlight = "it.",
        author = "- ANNE LAMOTT",
        imageUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?auto=format&fit=crop&w=1920&q=80",
        streamUrl = "https://ice2.somafm.com/dronezone-128-mp3",
        gradientColors = listOf(Color(0xFF23153C), Color(0xFF120924))
    ),
    AmbientScene(
        title = "Rainy Window Lofi",
        quoteMain = "Slow down and enjoy the simple ",
        quoteHighlight = "things.",
        author = "- LOFI WISDOM",
        imageUrl = "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=1920&q=80",
        streamUrl = "https://ice2.somafm.com/Lush-128-mp3",
        gradientColors = listOf(Color(0xFF141E30), Color(0xFF243B55))
    )
)

// Exact 5x7 LED Pixel Grid Map matching the screenshot
val PIXEL_DIGITS = mapOf(
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00110", "01000", "10000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("01110", "10000", "11110", "10001", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
    ':' to listOf("000", "010", "111", "010", "010", "111", "010") // Double plus '+' colon from screenshot!
)

@Composable
fun PixelMatrixClock(timeString: String, pixelSize: Dp = 9.dp, gap: Dp = 2.dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(pixelSize * 1.3f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        timeString.forEach { char ->
            val grid = PIXEL_DIGITS[char]
            if (grid != null) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    grid.forEach { rowStr ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            rowStr.forEach { cell ->
                                Box(
                                    modifier = Modifier
                                        .size(pixelSize)
                                        .background(if (cell == '1') Color.White else Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private var lastInteractionTime by mutableStateOf(System.currentTimeMillis())
    private var isAmbientMode by mutableStateOf(false)
    private var currentSceneIndex by mutableStateOf(0)
    private var isPlayingMusic by mutableStateOf(false)
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exoPlayer = ExoPlayer.Builder(this).build()

        setContent {
            LaunchedEffect(lastInteractionTime, isAmbientMode) {
                if (!isAmbientMode) {
                    delay(120_000L)
                    enterAmbientMode()
                }
            }

            LofiLauncherUI(
                isAmbientMode = isAmbientMode,
                scene = LOFI_SCENES[currentSceneIndex],
                isPlayingMusic = isPlayingMusic,
                onEnterAmbient = { enterAmbientMode() }
            )
        }
    }

    private fun enterAmbientMode() {
        isAmbientMode = true
        playCurrentSceneAudio()
    }

    private fun exitAmbientMode() {
        isAmbientMode = false
        isPlayingMusic = false
        exoPlayer?.pause()
    }

    private fun playCurrentSceneAudio() {
        val scene = LOFI_SCENES[currentSceneIndex]
        exoPlayer?.let { player ->
            player.setMediaItem(MediaItem.fromUri(scene.streamUrl))
            player.prepare()
            player.play()
            isPlayingMusic = true
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            lastInteractionTime = System.currentTimeMillis()

            if (isAmbientMode) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        currentSceneIndex = (currentSceneIndex + 1) % LOFI_SCENES.size
                        playCurrentSceneAudio()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        currentSceneIndex = if (currentSceneIndex - 1 < 0) LOFI_SCENES.size - 1 else currentSceneIndex - 1
                        playCurrentSceneAudio()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        exoPlayer?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                                isPlayingMusic = false
                            } else {
                                player.play()
                                isPlayingMusic = true
                            }
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_BACK -> {
                        exitAmbientMode()
                        return true
                    }
                }
            } else {
                if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    enterAmbientMode()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
        isPlayingMusic = false
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LofiLauncherUI(
    isAmbientMode: Boolean,
    scene: AmbientScene,
    isPlayingMusic: Boolean,
    onEnterAmbient: () -> Unit
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("7:54") }
    var currentDate by remember { mutableStateOf("SATURDAY OCT 31") }
    var weatherText by remember { mutableStateOf("52° Cloudy") }
    val apps = remember { getTvApps(context) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE MMM d", Locale.getDefault()).format(now).uppercase()
            delay(1000L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val fetched = fetchLocalWeather()
            if (fetched != null) weatherText = fetched
            delay(900_000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(scene.gradientColors))
    ) {
        AsyncImage(
            model = scene.imageUrl,
            contentDescription = scene.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isAmbientMode) 0.28f else 0.48f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 68.dp, vertical = 56.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                // True 5x7 Block Pixel Matrix Clock with Double-Plus Colon
                PixelMatrixClock(
                    timeString = currentTime,
                    pixelSize = 9.dp,
                    gap = 2.dp
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Retro Pixel Date
                Text(
                    text = currentDate,
                    fontSize = 13.sp,
                    fontFamily = PixelFont,
                    color = Color(0xFFAEC6CF)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Cloud Icon + Weather
                Text(
                    text = "☁ $weatherText",
                    fontSize = 13.sp,
                    fontFamily = PixelFont,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(38.dp))

                // Pixel Quote with Gold Highlighted Last Word
                Column(modifier = Modifier.width(640.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White)) {
                                append(scene.quoteMain)
                            }
                            withStyle(SpanStyle(color = Color(0xFFE5A93B))) {
                                append(scene.quoteHighlight)
                            }
                        },
                        fontSize = 22.sp,
                        fontFamily = PixelFont,
                        lineHeight = 36.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = scene.author,
                        fontSize = 11.sp,
                        fontFamily = PixelFont,
                        color = Color(0xFFAEC6CF)
                    )
                }
            }

            // Bottom Left: Equalizer Bars + "lofi radio"
            if (isAmbientMode) {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Pixel equalizer bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(Modifier.width(3.dp).height(8.dp).background(Color(0xFFE5A93B)))
                        Box(Modifier.width(3.dp).height(15.dp).background(Color(0xFFE5A93B)))
                        Box(Modifier.width(3.dp).height(5.dp).background(Color(0xFFE5A93B)))
                        Box(Modifier.width(3.dp).height(12.dp).background(Color(0xFFE5A93B)))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPlayingMusic) "lofi radio" else "lofi paused",
                        fontSize = 11.sp,
                        fontFamily = PixelFont,
                        color = Color.White
                    )
                }
            }

            // Bottom App Row (Hides in Ambient Mode)
            AnimatedVisibility(
                visible = !isAmbientMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Column {
                    Text(
                        text = "APPS  (PRESS ▲ FOR CHILL STATION)",
                        fontSize = 10.sp,
                        fontFamily = PixelFont,
                        color = Color(0xFFAEC6CF),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 32.dp)
                    ) {
                        items(apps) { app ->
                            AppCard(app = app) {
                                app.launchIntent?.let { context.startActivity(it) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCard(app: AppItem, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(185.dp)
            .height(92.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) Color(0xDD25283D) else Color(0xAA0E111A))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFFE5A93B) else Color(0x44FFFFFF),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .focusable()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = app.name,
            fontSize = 11.sp,
            fontFamily = PixelFont,
            color = if (isFocused) Color(0xFFE5A93B) else Color.White
        )
    }
}

suspend fun fetchLocalWeather(): String? = withContext(Dispatchers.IO) {
    try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=33.7206&longitude=-116.2156&current_weather=true&temperature_unit=fahrenheit"
        val response = URL(url).readText()
        val current = JSONObject(response).getJSONObject("current_weather")
        val temp = Math.round(current.getDouble("temperature"))
        val code = current.getInt("weathercode")
        val condition = when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55, 61, 63, 65 -> "Rain"
            else -> "Cloudy"
        }
        "${temp}° $condition"
    } catch (e: Exception) {
        null
    }
}

fun getTvApps(context: Context): List<AppItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    }
    return pm.queryIntentActivities(intent, 0)
        .mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            AppItem(
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = pkg,
                launchIntent = pm.getLaunchIntentForPackage(pkg) ?: pm.getLeanbackLaunchIntentForPackage(pkg)
            )
        }
        .sortedBy { it.name.lowercase() }
}
