package com.example.rateme.data

import android.content.Context
import com.example.rateme.data.model.Achievement
import com.example.rateme.data.model.Tier

object AchievementRepository {

    fun getAllAchievements(): List<Achievement> = listOf(
        Achievement("1", "Первый шаг", "Оценить 1 трек", "🎵", Tier.BRONZE, 1),
        Achievement("2", "Новичок", "Оценить 10 треков", "👶", Tier.BRONZE, 10),
        Achievement("3", "Коллекционер", "Добавить 3 альбома", "💿", Tier.BRONZE, 3),
        Achievement("4", "Разнообразие", "Оценить треки 3 разных групп", "🎸", Tier.BRONZE, 3),
        Achievement("5", "Ночной меломан", "Оценить трек в тёмной теме", "🌙", Tier.BRONZE, 1),
        Achievement("6", "Солнечный меломан", "Оценить трек в светлой теме", "☀️", Tier.BRONZE, 1),
        Achievement("7", "Билингва", "Сменить язык приложения", "🌍", Tier.BRONZE, 1),
        Achievement("8", "Оценщик", "Оценить 50 треков", "⭐", Tier.SILVER, 50),
        Achievement("9", "Идеальный слух", "Поставить 5 трекам 10/10", "💯", Tier.SILVER, 5),
        Achievement("10", "На огоньке", "5 дней подряд оценивать", "🔥", Tier.SILVER, 5),
        Achievement("11", "Меломан", "Добавить 10 альбомов", "📀", Tier.SILVER, 10),
        Achievement("12", "Фанат", "Оценить 3 альбома одной группы", "🎤", Tier.SILVER, 3),
        Achievement("13", "Эксперт", "Средний балл > 8", "💎", Tier.SILVER, 1),
        Achievement("14", "Домосед", "Открыть приложение 20 раз", "🏠", Tier.SILVER, 20),
        Achievement("15", "Легенда", "Оценить 100 треков", "🏆", Tier.GOLD, 100),
        Achievement("16", "Максималист", "Оценить все треки в 5 альбомах", "💯", Tier.GOLD, 5),
        Achievement("17", "Нон-стоп", "14 дней подряд с оценками", "🔥", Tier.GOLD, 14),
        Achievement("18", "Мировой слушатель", "Оценить треки 20 групп", "🌍", Tier.GOLD, 20),
        Achievement("19", "Статистик", "Посмотреть статистику 10 раз", "📊", Tier.GOLD, 10),
        Achievement("20", "Снайпер", "Средний балл альбома ровно 7.0", "🎯", Tier.GOLD, 1),
        Achievement("21", "Король оценок", "Оценить 500 треков", "👑", Tier.PLATINUM, 500),
        Achievement("22", "Энциклопедия", "Добавить 50 альбомов", "📚", Tier.PLATINUM, 50),
        Achievement("23", "Машина времени", "Оценить альбомы из 5 десятилетий", "🔄", Tier.PLATINUM, 5),
        Achievement("24", "Все звёзды", "Поставить 50 трекам 10/10", "🌟", Tier.PLATINUM, 50),
        Achievement("25", "Зал славы", "Оценить 1000 треков", "🏅", Tier.LEGENDARY, 1000),
        Achievement("26", "Идеальный альбом", "Всем трекам альбома 10/10", "✨", Tier.LEGENDARY, 1),
        Achievement("27", "Аудиофил", "Прослушать превью 100 раз", "🎧", Tier.LEGENDARY, 100),
    )

    fun loadProgress(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
        return getAllAchievements().associate { it.id to prefs.getInt(it.id, 0) }
    }

    fun saveProgress(context: Context, id: String, progress: Int) {
        context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
            .edit().putInt(id, progress).apply()
    }

    fun saveUnlocked(context: Context, id: String, date: String) {
        context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
            .edit().putBoolean("${id}_unlocked", true)
            .putString("${id}_date", date)
            .apply()
    }

    fun isUnlocked(context: Context, id: String): Boolean {
        return context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
            .getBoolean("${id}_unlocked", false)
    }
}