package com.pachira.prog7313poepachira.data

/**
 * Data class representing a financial wallet/account.
 * Used for organizing different payment methods and accounts in the budgeting app.
 */
data class Wallet(
    val id: String = "",                  // Unique identifier (e.g., Firebase key)
    val name: String = "",                // Name of the wallet (e.g., "Main Account", "Credit Card")
    val balance: Double = 0.0,            // Current balance in the wallet
    val iconName: String = "",            // Icon resource name (must match a drawable in /res)
    val colorHex: String = "#3F51B5",     // Background color in hex format for UI (default: Indigo)
    val type: String = "bank",            // Type: "bank", "credit", "cash", "savings", etc.
    val isActive: Boolean = true,         // Whether the wallet is currently active
    val createdAt: Long = System.currentTimeMillis() // Creation timestamp
) {
    // Required empty constructor for Firebase deserialization
    constructor() : this("", "", 0.0, "", "#3F51B5", "bank", true, System.currentTimeMillis())
}