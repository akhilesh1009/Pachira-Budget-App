package com.pachira.prog7313poepachira.data

/**
 * Data class representing a budget goal.
 * Used for tracking progress toward a financial target.
 */
//(Kotlin 2024)
data class BudgetGoal(
    var id: String = "",                  // Unique identifier (e.g., Firebase key)
    val name: String = "",                // Name of the goal (e.g., "Vacation Fund")
    var targetAmount: Double = 0.0,       // Total amount the user wants to save
    var currentAmount: Double = 0.0,      // Amount currently saved toward the goal
    var createdAt: Long = 0,              // Timestamp of when the goal was created (milliseconds since epoch)
    var categoryId: String? = null,       // ID of the associated category (nullable)
    var walletId: String? = null,         // ID of the associated wallet (nullable)
    var recurrence: String = "Monthly"    // Recurrence type (Daily, Weekly, Biweekly, Monthly, Yearly)
) {
    // Required empty constructor for Firebase deserialization
    constructor() : this("", "", 0.0, 0.0, 0, null, null, "Monthly")
}

//REFERENCES
// Kotlin. 2024. "Kotlin Help". Kotlin Help. 2024
// <https://kotlinlang.org/docs/data-classes.html#properties-declared-in-the-class-body>
// [accessed 30 April 2025].