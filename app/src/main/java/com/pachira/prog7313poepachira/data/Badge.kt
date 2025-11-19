package com.pachira.prog7313poepachira.data

data class Badge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconName: String = "",
    val colorHex: String = "#6200EE",
    val category: String = "", // "savings", "spending", "streak", "milestone"
    val requirement: String = "", // Description of how to earn it
    val earnedAt: Long = 0L,
    val earned: Boolean = false,
    val rarity: String = "common" // "common", "rare", "epic", "legendary"
)
