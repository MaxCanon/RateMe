package com.example.rateme.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.rateme.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onBack: () -> Unit,
    onStatsClick: () -> Unit = {},
    onLanguageChange: () -> Unit = {},
    onRefreshMetadata: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val savedLang = prefs.getString("language", "ru") ?: "ru"

    var expandedLanguage by remember { mutableStateOf(false) }
    val languageMap = mapOf(
        "ru" to stringResource(R.string.language_ru),
        "en" to stringResource(R.string.language_en),
        "de" to stringResource(R.string.language_de),
        "fr" to stringResource(R.string.language_fr)
    )
    var selectedLanguage by remember { mutableStateOf(languageMap[savedLang] ?: "Русский") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Box {
                SettingsItem(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.language),
                    subtitle = selectedLanguage,
                    onClick = { expandedLanguage = true }
                )
                DropdownMenu(expanded = expandedLanguage, onDismissRequest = { expandedLanguage = false }) {
                    languageMap.forEach { (code, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            if (code != savedLang) {
                                onLanguageChange()
                            }

                            expandedLanguage = false
                            prefs.edit()
                                .putString("language", code)
                                .apply()
                            
                            val appLocale: androidx.core.os.LocaleListCompat = androidx.core.os.LocaleListCompat.forLanguageTags(code)
                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                        })
                    }
                }
            }

            SettingsItem(
                icon = Icons.Filled.BarChart,
                title = "Статистика",
                subtitle = "Альбомы, треки, рейтинг",
                onClick = onStatsClick
            )

            SettingsItem(
                icon = Icons.Filled.Update,
                title = "Обновить данные",
                subtitle = "Исправить года выпуска альбомов",
                onClick = {
                    onRefreshMetadata()
                    android.widget.Toast.makeText(context, "Обновление запущено...", android.widget.Toast.LENGTH_SHORT).show()
                }
            )

            SettingsItem(
                icon = Icons.Filled.History,
                title = "Очистить историю",
                subtitle = "Удалить запросы поиска",
                onClick = {
                    com.example.rateme.data.SearchHistory.clearHistory(context)
                    android.widget.Toast.makeText(context, "История очищена", android.widget.Toast.LENGTH_SHORT).show()
                }
            )

            SettingsItem(
                icon = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                title = stringResource(R.string.theme),
                subtitle = if (isDarkTheme) stringResource(R.string.dark_theme) else stringResource(R.string.light_theme),
                onClick = onThemeToggle
            )

            SettingsItem(
                icon = Icons.Filled.ChatBubbleOutline,
                title = "Поддержка",
                subtitle = "Связаться с автором в Telegram",
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/brikiton"))
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RateMe v0.8",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = "Created with ❤️ by @brikiton",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}