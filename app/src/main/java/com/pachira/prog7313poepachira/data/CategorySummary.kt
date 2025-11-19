package com.pachira.prog7313poepachira.data

/**
 * Represents a summarized view of a category's financial status.
 * Typically used in dashboard visualizations for budget tracking.
 */
//(Kotlin 2024)
data class CategorySummary(
    val categoryId: String,      // The ID of the associated category
    val name: String,            // Name of the category
    val colorHex: String,        // UI color in hex (same as category's color)
    val iconName: String,        // Icon name (should map to a drawable)
    val amount: Double,          // Total amount spent/earned in this category
    val limit: Double,           // Budget limit set for this category
    val percentage: Int,         // How much of the limit is used (e.g., 85%)
    val type: String             // "income" or "expense"
)

//REFERENCES
// Kotlin. 2024. “Kotlin Help”. Kotlin Help. 2024
// <https://kotlinlang.org/docs/data-classes.html#properties-declared-in-the-class-body>
// [accessed 30 April 2025].
