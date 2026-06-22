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
        val lastRatingDate = AchievementRepository.getString(context, "last_rating_date") ?: ""
        val today = sdf.format(Date())
        if (totalRated > -1 && lastRatingDate != today) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DATE, -1)
            val yesterday = sdf.format(calendar.time)
            
            var streak = AchievementRepository.getInt(context, "rating_streak")
            if (lastRatingDate == yesterday) {
                streak++
            } else {
                streak = 1
            }
            AchievementRepository.saveString(context, "last_rating_date", today)
            AchievementRepository.saveInt(context, "rating_streak", streak)
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
                "10" -> progress = currentStreak.coerceAtMost(5)
                "11" -> if (totalAlbums >= 0) progress = totalAlbums.coerceAtMost(10)
                "12" -> {
                    val maxAlbumsByOneArtist = albumsList?.groupBy { it.artist.id }?.maxOfOrNull { it.value.size } ?: 0
                    progress = maxAlbumsByOneArtist.coerceAtMost(3)
                }
                "13" -> {
                    val allRatings = albumsList?.flatMap { it.songs }?.mapNotNull { it.rating } ?: emptyList()
                    if (allRatings.isNotEmpty() && allRatings.average() > 8.0) progress = 1
                }
                "15" -> if (totalRated >= 0) progress = totalRated.coerceAtMost(100)
                "16" -> if (fullAlbums >= 0) progress = fullAlbums.coerceAtMost(5)
                "17" -> progress = currentStreak.coerceAtMost(14)
                "18" -> if (uniqueArtists >= 0) progress = uniqueArtists.coerceAtMost(20)
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
                // manual increments: "7" (Bilingua), "14" (Domosed), "19" (Statistic), "27" (Audiophile)
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
