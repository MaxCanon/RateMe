package com.example.rateme.data

import android.content.Context
import com.example.rateme.data.model.Achievement
import java.text.SimpleDateFormat
import java.util.*

object AchievementManager {
    private val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun increment(context: Context, achId: String, amount: Int = 1, onNewAchievement: (Achievement) -> Unit) {
        val current = AchievementRepository.getProgress(context, achId)
        val newProgress = current + amount
        AchievementRepository.saveProgress(context, achId, newProgress)
        
        // Re-run check to see if unlocked
        checkAndUpdate(context, onNewAchievement = onNewAchievement)
    }

    fun checkAndUpdate(
        context: Context,
        totalRated: Int = -1,
        totalAlbums: Int = -1,
        uniqueArtists: Int = -1,
        perfect10: Int = -1,
        fullAlbums: Int = -1,
        isDarkTheme: Boolean? = null,
        albumsList: List<AlbumWithArtistAndSongs>? = null,
        onNewAchievement: (Achievement) -> Unit
    ) {
        val achievements = AchievementRepository.getAllAchievements()

        // Streak logic
        val lastRatingDateStr = AchievementRepository.getString(context, "last_rating_date") ?: ""
        val today = sdf.format(Date())
        
        if (totalRated > -1 && lastRatingDateStr != today) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DATE, -1)
            val yesterday = sdf.format(calendar.time)
            
            var streak = AchievementRepository.getInt(context, "rating_streak")
            
            if (lastRatingDateStr == yesterday) {
                streak++
            } else if (lastRatingDateStr.isNotEmpty()) {
                // If last rating was not yesterday and not today, and we actually had a previous rating, streak is broken
                streak = 1
            } else {
                // First rating ever
                streak = 1
            }
            
            AchievementRepository.saveString(context, "last_rating_date", today)
            AchievementRepository.saveInt(context, "rating_streak", streak)
        }
        
        // Reset streak if missed days (check on app open)
        if (totalRated == -2) { // Special code for app open check
            val lastDate = AchievementRepository.getString(context, "last_rating_date") ?: ""
            if (lastDate.isNotEmpty()) {
                val lastRatingDate = sdf.parse(lastDate)
                val diff = Date().time - lastRatingDate.time
                val days = diff / (1000 * 60 * 60 * 24)
                if (days > 1) {
                    AchievementRepository.saveInt(context, "rating_streak", 0)
                }
            }
        }

        val currentStreak = AchievementRepository.getInt(context, "rating_streak")

        for (ach in achievements) {
            if (AchievementRepository.isUnlocked(context, ach.id)) continue

            var progress = AchievementRepository.getProgress(context, ach.id)

            when (ach.id) {
                "1" -> if (totalRated >= 1) progress = 1
                "2" -> if (totalRated >= 0) progress = totalRated.coerceAtMost(10)
                "3" -> if (totalAlbums >= 0) progress = totalAlbums.coerceAtMost(3)
                "4" -> if (uniqueArtists >= 0) progress = uniqueArtists.coerceAtMost(3)
                "5" -> if (isDarkTheme == true) progress = 1
                "6" -> if (isDarkTheme == false) progress = 1
                "8" -> if (totalRated >= 0) progress = totalRated.coerceAtMost(50)
                "9" -> if (perfect10 >= 0) progress = perfect10.coerceAtMost(5)
                "10" -> progress = currentStreak.coerceAtMost(10)
                "11" -> if (totalAlbums >= 0) progress = totalAlbums.coerceAtMost(25)
                "12" -> {
                    val maxAlbumsByOneArtist = albumsList?.groupBy { it.artist.id }?.maxOfOrNull { it.value.size } ?: 0
                    progress = maxAlbumsByOneArtist.coerceAtMost(3)
                }
                "13" -> {
                    val allRatings = albumsList?.flatMap { it.songs }?.mapNotNull { it.rating } ?: emptyList()
                    if (allRatings.isNotEmpty() && allRatings.average() > 8.0) progress = 1
                }
                "14" -> progress = AchievementRepository.getProgress(context, "14").coerceAtMost(30)
                "15" -> if (totalRated >= 0) progress = totalRated.coerceAtMost(100)
                "16" -> if (fullAlbums >= 0) progress = fullAlbums.coerceAtMost(5)
                "17" -> progress = currentStreak.coerceAtMost(14)
                "18" -> if (uniqueArtists >= 0) progress = uniqueArtists.coerceAtMost(20)
                "19" -> progress = AchievementRepository.getProgress(context, "19").coerceAtMost(10)
                "20" -> {
                    val hasAlbumWithSeven = albumsList?.any { album -> 
                        val ratings = album.songs.mapNotNull { it.rating }
                        ratings.isNotEmpty() && String.format(Locale.getDefault(), "%.1f", ratings.average()) == "7.0"
                    } ?: false
                    if (hasAlbumWithSeven) progress = 1
                }
                "21" -> if (totalRated >= 0) progress = totalRated.coerceAtMost(500)
                "22" -> if (totalAlbums >= 0) progress = totalAlbums.coerceAtMost(50)
                "23" -> {
                    val decades = albumsList?.mapNotNull { it.album.year?.take(3) }?.distinct()?.size ?: 0
                    progress = decades.coerceAtMost(5)
                }
                "24" -> if (perfect10 >= 0) progress = perfect10.coerceAtMost(50)
                "25" -> if (totalRated >= 0) progress = totalRated.coerceAtMost(1000)
                "26" -> {
                    val hasPerfectAlbum = albumsList?.any { album ->
                        album.songs.isNotEmpty() && album.songs.all { it.rating == 10 }
                    } ?: false
                    if (hasPerfectAlbum) progress = 1
                }
                "27" -> progress = AchievementRepository.getProgress(context, "27").coerceAtMost(1000)
                "28" -> if (totalRated >= 0) progress = totalRated.coerceAtMost(10000)
                "29" -> {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    if (totalRated > -1 && hour in 0..5) progress = 1
                }
                "30" -> {
                    val hasOldAlbum = albumsList?.any { it.album.year?.toIntOrNull()?.let { y -> y < 1970 } == true && it.songs.any { s -> s.rating != null } } ?: false
                    if (hasOldAlbum) progress = 1
                }
            }

            if (progress > AchievementRepository.getProgress(context, ach.id)) {
                AchievementRepository.saveProgress(context, ach.id, progress)
            }

            if (progress >= ach.goal) {
                AchievementRepository.setUnlocked(context, ach.id, sdf.format(Date()))
                onNewAchievement(ach)
            }
        }
    }
}
