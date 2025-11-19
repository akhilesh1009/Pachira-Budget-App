package com.pachira.prog7313poepachira.utility

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pachira.prog7313poepachira.data.BudgetGoal

class BudgetReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "BudgetReminderWorker"
    }

    override fun doWork(): Result {
        return try {
            val budgetGoalId = inputData.getString(BudgetReminderScheduler.BUDGET_GOAL_ID_KEY)
                ?: return Result.failure()

            Log.d(TAG, "Processing budget reminder for goal: $budgetGoalId")

            // Get current user
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user found")
                return Result.failure()
            }

            // Fetch the latest budget goal data from Firebase
            fetchAndProcessBudgetGoal(currentUser.uid, budgetGoalId)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing budget reminder", e)
            Result.retry()
        }
    }

    private fun fetchAndProcessBudgetGoal(userId: String, budgetGoalId: String) {
        val database = FirebaseDatabase.getInstance().reference

        database.child("users").child(userId).child("budgetGoals").child(budgetGoalId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val budgetGoal = snapshot.getValue(BudgetGoal::class.java)

                    if (budgetGoal != null) {
                        processBudgetGoalReminder(budgetGoal)
                    } else {
                        Log.w(TAG, "Budget goal not found: $budgetGoalId")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to fetch budget goal: ${error.message}")
                }
            })
    }

    private fun processBudgetGoalReminder(budgetGoal: BudgetGoal) {
        val notificationHelper = NotificationHelper(applicationContext)
        val scheduler = BudgetReminderScheduler(applicationContext)

        // Check if goal is already achieved
        if (budgetGoal.currentAmount >= budgetGoal.targetAmount) {
            Log.d(TAG, "Budget goal ${budgetGoal.id} already achieved, showing achievement notification")
            notificationHelper.showGoalAchievedNotification(budgetGoal)
            // Cancel any future reminders for this achieved goal
            scheduler.cancelBudgetReminder(budgetGoal.id)
            return
        }

        // Show reminder notification
        Log.d(TAG, "Showing budget reminder for goal: ${budgetGoal.name}")
        notificationHelper.showBudgetReminderNotification(budgetGoal)

        // RESCHEDULE for non-periodic work (monthly, yearly, biweekly)
        val recurrence = budgetGoal.recurrence?.lowercase() ?: "monthly"
        if (recurrence in listOf("monthly", "yearly", "biweekly")) {
            Log.d(TAG, "Rescheduling next reminder for $recurrence goal: ${budgetGoal.name}")
            scheduler.scheduleRecurringReminder(budgetGoal)
        }
        // For daily and weekly, the PeriodicWorkRequest handles automatic rescheduling
    }
}