package com.example.rateme.data.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String, // emoji
    val tier: Tier,
    val goal: Int,
    var progress: Int = 0,
    var unlocked: Boolean = false,
    var unlockedDate: String? = null
)

enum class Tier { BRONZE, SILVER, GOLD, PLATINUM, LEGENDARY }