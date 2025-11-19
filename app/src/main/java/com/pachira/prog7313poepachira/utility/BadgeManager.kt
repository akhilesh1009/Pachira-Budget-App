package com.pachira.prog7313poepachira.utility

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pachira.prog7313poepachira.data.Badge
import com.pachira.prog7313poepachira.data.BudgetGoal
import java.util.*

class BadgeManager(private val context: Context) {

    companion object {
        private const val TAG = "BadgeManager"
    }

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Initialize all available badges
    fun initializeBadges() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val badgesRef = database.child("users").child(userId).child("badges")

        // Check if badges are already initialized
        badgesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Initialize all badges as unearned
                    createAllBadges(badgesRef)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to check badges: ${error.message}")
            }
        })
    }

    private fun createAllBadges(badgesRef: DatabaseReference) {
        val badges = listOf(
            // Savings Badges
            Badge(
                id = "first_goal",
                name = "First Steps",
                description = "Complete your first budget goal",
                iconName = "ic_badge_first_goal",
                colorHex = "#4CAF50",
                category = "savings",
                requirement = "Complete 1 budget goal",
                rarity = "common"
            ),
            Badge(
                id = "goal_master",
                name = "Goal Master",
                description = "Complete 5 budget goals",
                iconName = "ic_badge_goal_master",
                colorHex = "#FF9800",
                category = "savings",
                requirement = "Complete 5 budget goals",
                rarity = "rare"
            ),
            Badge(
                id = "savings_legend",
                name = "Savings Legend",
                description = "Complete 10 budget goals",
                iconName = "ic_badge_savings_legend",
                colorHex = "#9C27B0",
                category = "savings",
                requirement = "Complete 10 budget goals",
                rarity = "epic"
            ),

            // Milestone Badges
            Badge(
                id = "thousand_saver",
                name = "Thousand Saver",
                description = "Save R1,000 in total",
                iconName = "ic_badge_thousand",
                colorHex = "#2196F3",
                category = "milestone",
                requirement = "Save R1,000 total",
                rarity = "common"
            ),
            Badge(
                id = "ten_thousand_saver",
                name = "Ten Thousand Club",
                description = "Save R10,000 in total",
                iconName = "ic_badge_ten_thousand",
                colorHex = "#FF5722",
                category = "milestone",
                requirement = "Save R10,000 total",
                rarity = "rare"
            ),
            Badge(
                id = "hundred_thousand_saver",
                name = "Savings Millionaire",
                description = "Save R100,000 in total",
                iconName = "ic_badge_hundred_thousand",
                colorHex = "#FFD700",
                category = "milestone",
                requirement = "Save R100,000 total",
                rarity = "legendary"
            ),

            // Speed Badges
            Badge(
                id = "quick_saver",
                name = "Quick Saver",
                description = "Complete a goal in under 7 days",
                iconName = "ic_badge_quick_saver",
                colorHex = "#00BCD4",
                category = "speed",
                requirement = "Complete goal in 7 days",
                rarity = "rare"
            ),
            Badge(
                id = "lightning_saver",
                name = "Lightning Saver",
                description = "Complete a goal in under 24 hours",
                iconName = "ic_badge_lightning",
                colorHex = "#FFEB3B",
                category = "speed",
                requirement = "Complete goal in 1 day",
                rarity = "epic"
            ),

            // Streak Badges
            Badge(
                id = "consistent_saver",
                name = "Consistent Saver",
                description = "Complete 3 goals in a row",
                iconName = "ic_badge_consistent",
                colorHex = "#8BC34A",
                category = "streak",
                requirement = "Complete 3 consecutive goals",
                rarity = "rare"
            ),
            Badge(
                id = "dedication_master",
                name = "Dedication Master",
                description = "Complete 5 goals in a row",
                iconName = "ic_badge_dedication",
                colorHex = "#E91E63",
                category = "streak",
                requirement = "Complete 5 consecutive goals",
                rarity = "epic"
            ),

            // Special Badges
            Badge(
                id = "big_dreamer",
                name = "Big Dreamer",
                description = "Achieve a goal worth R50,000 or more",
                iconName = "ic_badge_big_dreamer",
                colorHex = "#673AB7",
                category = "special",
                requirement = "Set goal ≥ R50,000",
                rarity = "epic"
            ),
            Badge(
                id = "perfectionist",
                name = "Perfectionist",
                description = "Complete a goal with exact target amount",
                iconName = "ic_badge_perfectionist",
                colorHex = "#607D8B",
                category = "special",
                requirement = "Reach exact goal amount",
                rarity = "rare"
            )
        )

        badges.forEach { badge ->
            badgesRef.child(badge.id).setValue(badge)
        }

        Log.d(TAG, "Initialized ${badges.size} badges")
    }

    // Check and award badges when a goal is completed
    fun checkAndAwardBadges(completedGoal: BudgetGoal, onBadgeEarned: (Badge) -> Unit) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Get all completed goals to check for various achievements
        database.child("users").child(userId).child("budgetGoals")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allGoals = mutableListOf<BudgetGoal>()
                    for (goalSnapshot in snapshot.children) {
                        val goal = goalSnapshot.getValue(BudgetGoal::class.java)
                        goal?.let { allGoals.add(it) }
                    }

                    val completedGoals = allGoals.filter { it.currentAmount >= it.targetAmount }
                    checkBadgeEligibility(completedGoals, completedGoal, onBadgeEarned)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load goals for badge checking: ${error.message}")
                }
            })
    }

    private fun checkBadgeEligibility(
        completedGoals: List<BudgetGoal>,
        newlyCompletedGoal: BudgetGoal,
        onBadgeEarned: (Badge) -> Unit
    ) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        val badgesRef = database.child("users").child(userId).child("badges")

        // Check First Goal badge
        if (completedGoals.size == 1) {
            awardBadge("first_goal", badgesRef, onBadgeEarned)
        }

        // Check Goal Master badge (5 goals)
        if (completedGoals.size == 5) {
            awardBadge("goal_master", badgesRef, onBadgeEarned)
        }

        // Check Savings Legend badge (10 goals)
        if (completedGoals.size == 10) {
            awardBadge("savings_legend", badgesRef, onBadgeEarned)
        }

        // Check milestone badges based on total saved amount
        val totalSaved = completedGoals.sumOf { it.targetAmount }
        when {
            totalSaved >= 100000 -> awardBadge("hundred_thousand_saver", badgesRef, onBadgeEarned)
            totalSaved >= 10000 -> awardBadge("ten_thousand_saver", badgesRef, onBadgeEarned)
            totalSaved >= 1000 -> awardBadge("thousand_saver", badgesRef, onBadgeEarned)
        }

        // Check speed badges
        val goalDuration = System.currentTimeMillis() - newlyCompletedGoal.createdAt
        val dayInMillis = 24 * 60 * 60 * 1000L
        when {
            goalDuration <= dayInMillis -> awardBadge("lightning_saver", badgesRef, onBadgeEarned)
            goalDuration <= 7 * dayInMillis -> awardBadge("quick_saver", badgesRef, onBadgeEarned)
        }

        // Check big dreamer badge
        if (newlyCompletedGoal.currentAmount >= 50000) {
            awardBadge("big_dreamer", badgesRef, onBadgeEarned)
        }

        // Check perfectionist badge (exact amount)
        if (newlyCompletedGoal.currentAmount == newlyCompletedGoal.targetAmount) {
            awardBadge("perfectionist", badgesRef, onBadgeEarned)
        }

        // Check streak badges (this would need more complex logic to track consecutive completions)
        checkStreakBadges(completedGoals, badgesRef, onBadgeEarned)
    }

    private fun checkStreakBadges(
        completedGoals: List<BudgetGoal>,
        badgesRef: DatabaseReference,
        onBadgeEarned: (Badge) -> Unit
    ) {
        // Sort goals by completion date (when currentAmount reached targetAmount)
        val sortedGoals = completedGoals.sortedBy { it.createdAt }

        // Simple streak logic - check if last 3 or 5 goals were completed
        if (sortedGoals.size >= 5) {
            awardBadge("dedication_master", badgesRef, onBadgeEarned)
        } else if (sortedGoals.size >= 3) {
            awardBadge("consistent_saver", badgesRef, onBadgeEarned)
        }
    }

    private fun awardBadge(badgeId: String, badgesRef: DatabaseReference, onBadgeEarned: (Badge) -> Unit) {
        badgesRef.child(badgeId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val badge = snapshot.getValue(Badge::class.java)
                if (badge != null && !badge.earned) {
                    // Award the badge
                    val updatedBadge = badge.copy(
                        earned = true,
                        earnedAt = System.currentTimeMillis()
                    )

                    badgesRef.child(badgeId).setValue(updatedBadge)
                        .addOnSuccessListener {
                            Log.d(TAG, "Badge awarded: ${badge.name}")
                            onBadgeEarned(updatedBadge)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to award badge: ${exception.message}")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to check badge status: ${error.message}")
            }
        })
    }
}
