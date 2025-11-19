package com.pachira.prog7313poepachira.data

/**
 * Data class representing a financial category (e.g., Food, Transport).
 * Used for organizing income and expenses in the budgeting app.
 */
//(Kotlin 2024)
data class Category(
    val id: String = "",                  // Unique identifier (e.g., Firebase key)
    val name: String = "",                // Name of the category (e.g., "Food", "Salary")
    val colorHex: String = "#3F51B5",     // Background color in hex format for UI (default: Indigo)
    val iconName: String = "",            // Icon resource name (must match a drawable in /res)
    val type: String = "expense",         // Either "expense" or "income"
    val budgetLimit: Double = 0.0         // Optional limit set for the category
) {
    // Required empty constructor for Firebase deserialization
    constructor() : this("", "", "#3F51B5", "", "expense", 0.0)
}

//REFERENCES
// Kotlin. 2024. “Kotlin Help”. Kotlin Help. 2024
// <https://kotlinlang.org/docs/data-classes.html#properties-declared-in-the-class-body>
// [accessed 30 April 2025].
