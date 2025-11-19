package com.pachira.prog7313poepachira.data

/**
 * Represents a single income or expense transaction in the app.
 */

//(Kotlin 2024)
data class Transaction(
    val id: String = "",             // Unique ID for the transaction (Firebase key or UUID)
    val amount: Double = 0.0,        // Transaction amount
    val category: String = "",       // Human-readable category name
    val categoryId: String = "",     // Category ID (useful for joins or lookups)
    val description: String = "",    // Optional description (e.g., "Lunch at Nando's")
    val date: Long = 0,              // Timestamp (in milliseconds) when the transaction occurred
    val type: String = "",           // "income" or "expense"
    val imageData: String = "",      // Base64-encoded image string for receipts (expenses only)
    val walletId: String = ""        // ID of the wallet this transaction belongs to
) {
    // Required empty constructor for Firebase deserialization
    constructor() : this("", 0.0, "", "", "", 0, "", "", "")
}


//REFERENCES
// Kotlin. 2024. “Kotlin Help”. Kotlin Help. 2024
// <https://kotlinlang.org/docs/data-classes.html#properties-declared-in-the-class-body>
// [accessed 30 April 2025].