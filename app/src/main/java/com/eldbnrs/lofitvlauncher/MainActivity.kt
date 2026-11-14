package com.eldbnrs.lofitvlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class AppItem(
    val name: String,
    val packageName: String,
    val launchIntent: Intent?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LofiHomeScreen()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LofiHomeScreen() {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    val apps = remember { getTvApps(context) }

    // Live Clock Updater
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A162B), Color(0xFF0D0B14))
                )
            )
            .padding(48.dp)
    ) {
        // Top Left: Clock, Weather & Quote
        Column(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = currentTime,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDEBD0)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currentDate  •  72° Clear  •  Chill Station",
                fontSize = 20.sp,
                color = Color(0xFFBDC3C7)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "\"Slow down and enjoy the simple things.\"",
                fontSize = 18.sp,
                color = Color(0xFF95A5A6)
            )
        }

        // Bottom: Horizontal TV App Dock
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = "APPS",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7F8C8D),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCard(app: AppItem, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(190.dp)
            .height(105.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isFocused) Color(0xFF382C5E) else Color(0xFF221C35)
            )
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) Color(0xFFFFB74D) else Color(0xFF3E345B),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .focusable()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = app.name,
            fontSize = 18.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            color = if (isFocused) Color(0xFFFFF8E7) else Color(0xFFD0ECE7)
        )
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
            if (pkg == context.packageName) return@mapNotNull null // Hide our own launcher from the list
            AppItem(
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = pkg,
                launchIntent = pm.getLaunchIntentForPackage(pkg) ?: pm.getLeanbackLaunchIntentForPackage(pkg)
            )
        }
        .sortedBy { it.name.lowercase() }
}
