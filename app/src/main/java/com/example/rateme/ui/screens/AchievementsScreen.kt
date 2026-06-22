package com.example.rateme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rateme.R
import com.example.rateme.data.AchievementRepository
import com.example.rateme.data.model.Achievement
import com.example.rateme.data.model.Tier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val achievements = remember { AchievementRepository.getAllAchievements() }
    var progressMap by remember { mutableStateOf(AchievementRepository.loadProgress(context)) }

// Перезагружаем при каждом показе экрана
    LaunchedEffect(Unit) {
        progressMap = AchievementRepository.loadProgress(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.achievements), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(achievements) { ach ->
                val progress = progressMap[ach.id] ?: 0
                val unlocked = AchievementRepository.isUnlocked(context, ach.id)
                AchievementCard(ach, progress, unlocked)
            }
        }
    }
}

@Composable
fun AchievementCard(ach: Achievement, progress: Int, unlocked: Boolean) {
    val tierColor = when (ach.tier) {
        Tier.BRONZE -> Color(0xFFCD7F32)
        Tier.SILVER -> Color(0xFFC0C0C0)
        Tier.GOLD -> Color(0xFFFFD700)
        Tier.PLATINUM -> Color(0xFFE5E4E2)
        Tier.LEGENDARY -> Color(0xFFFF4500)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) tierColor.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(ach.icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ach.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (unlocked) { Spacer(modifier = Modifier.width(8.dp)); Text("✅") }
                }
                Text(ach.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (progress.toFloat() / ach.goal).coerceAtMost(1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (unlocked) tierColor else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text("$progress / ${ach.goal}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}