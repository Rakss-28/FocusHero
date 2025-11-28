package com.example.focushero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focushero.data.Mood
import com.example.focushero.data.SessionRepository
import com.example.focushero.ui.theme.FocusHeroTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize AdMob
        MobileAds.initialize(this) {}
        
        setContent {
            FocusHeroTheme {
                FocusHeroApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun FocusHeroApp() {
    val viewModel: MainViewModel = viewModel()
    val repository = remember { SessionRepository(viewModel.getApplication()) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == viewModel.currentDestination,
                    onClick = { viewModel.currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            Column(modifier = modifier.fillMaxSize()) {
                // Content Area
                Column(modifier = Modifier.weight(1f)) {
                    when (viewModel.currentDestination) {
                        AppDestinations.HOME -> HomeScreen(
                            viewModel = viewModel
                        )
                        AppDestinations.STATS -> StatsScreen(
                            repository = repository
                        )
                        AppDestinations.PROFILE -> ProfileScreen(
                            points = viewModel.points,
                            streak = viewModel.streak
                        )
                    }
                }
                
                // Banner Ad at the bottom
                BannerAd(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // Use Test Ad Unit ID for development. 
                // Replace with your real Ad Unit ID from AdMob dashboard before release.
                adUnitId = "ca-app-pub-3940256099942544/6300978111" 
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Focus", Icons.Default.PlayArrow),
    STATS("Stats", Icons.Default.Home),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    if (viewModel.showSummary) {
        SessionSummaryScreen(
            points = viewModel.lastSessionPoints,
            onDismiss = { viewModel.dismissSummary() }
        )
    } else if (viewModel.isRunning) {
        TimerScreen(
            timeLeft = viewModel.timeLeft,
            totalTime = viewModel.totalTime,
            isPaused = viewModel.isPaused,
            onPauseResume = { if (viewModel.isPaused) viewModel.startTimer() else viewModel.pauseTimer() },
            onStop = { viewModel.stopTimer() }
        )
    } else {
        SetupScreen(
            modifier = modifier,
            subject = viewModel.subject,
            onSubjectChange = { viewModel.updateSubject(it) },
            selectedMood = viewModel.selectedMood,
            onMoodChange = { viewModel.updateMood(it) },
            onStart = { viewModel.startTimer() }
        )
    }
}

@Composable
fun SetupScreen(
    modifier: Modifier = Modifier,
    subject: String,
    onSubjectChange: (String) -> Unit,
    selectedMood: Mood,
    onMoodChange: (Mood) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Adaptive Focus Engine™",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = subject,
            onValueChange = onSubjectChange,
            label = { Text("Subject / Task") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("How are you feeling?", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Mood.values().forEach { mood ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = (mood == selectedMood),
                    onClick = { onMoodChange(mood) }
                )
                Column {
                    Text(text = mood.label, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${mood.focusMinutes}m Focus • ${mood.breakMinutes}m Break",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Start Focus Session")
        }
    }
}

@Composable
fun TimerScreen(
    timeLeft: Long,
    totalTime: Long,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onPauseResume) {
                Text(if (isPaused) "Resume" else "Pause")
            }
            OutlinedButton(onClick = onStop) {
                Text("Give Up")
            }
        }
    }
}

@Composable
fun SessionSummaryScreen(
    points: Int,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.height(64.dp).width(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Session Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("You earned $points Focus Points", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onDismiss) {
            Text("Continue")
        }
    }
}

@Composable
fun StatsScreen(modifier: Modifier = Modifier, repository: SessionRepository) {
    // Using a state that updates when sessions are saved would be even better, 
    // but reading from repo on compose is acceptable for this scale.
    val sessions = remember(repository.getPoints()) { repository.getSessions().reversed() }

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(
                "Recent Sessions",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(sessions) { session ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(session.subject, fontWeight = FontWeight.Bold)
                        Text("+${session.pointsEarned} pts", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${session.durationSeconds / 60} min • ${session.mood.label}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val date = Date(session.timestamp)
                        val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        Text(format.format(date), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (sessions.isEmpty()) {
            item {
                Text("No sessions yet. Start focusing!", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, points: Int, streak: Int) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountBox,
            contentDescription = null,
            modifier = Modifier
                .height(100.dp)
                .width(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Focus Hero", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = points.toString(), label = "Focus Points")
            StatItem(value = streak.toString(), label = "Day Streak")
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
