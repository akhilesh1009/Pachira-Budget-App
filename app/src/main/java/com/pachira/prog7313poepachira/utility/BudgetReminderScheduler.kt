package com.pachira.prog7313poepachira.utility

import android.content.Context
import androidx.work.*
import com.pachira.prog7313poepachira.data.BudgetGoal
import java.util.*
import java.util.concurrent.TimeUnit

class BudgetReminderScheduler(private val context: Context) {

    companion object {
        const val WORK_TAG_PREFIX = "budget_reminder_"
        const val BUDGET_GOAL_ID_KEY = "budget_goal_id"
        const val BUDGET_GOAL_NAME_KEY = "budget_goal_name"
        const val BUDGET_GOAL_TARGET_KEY = "budget_goal_target"
        const val BUDGET_GOAL_CURRENT_KEY = "budget_goal_current"
        const val BUDGET_GOAL_RECURRENCE_KEY = "budget_goal_recurrence"
    }

    fun scheduleBudgetReminder(budgetGoal: BudgetGoal) {
        // Cancel any existing reminder for this budget goal
        cancelBudgetReminder(budgetGoal.id)

        // Don't schedule reminders for completed goals
        if (budgetGoal.currentAmount >= budgetGoal.targetAmount) {
            return
        }

        val delay = calculateNextReminderDelay(budgetGoal.recurrence)
        if (delay <= 0) return

        val inputData = Data.Builder()
            .putString(BUDGET_GOAL_ID_KEY, budgetGoal.id)
            .putString(BUDGET_GOAL_NAME_KEY, budgetGoal.name)
            .putDouble(BUDGET_GOAL_TARGET_KEY, budgetGoal.targetAmount)
            .putDouble(BUDGET_GOAL_CURRENT_KEY, budgetGoal.currentAmount)
            .putString(BUDGET_GOAL_RECURRENCE_KEY, budgetGoal.recurrence)
            .build()

        val reminderWork = OneTimeWorkRequestBuilder<BudgetReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(WORK_TAG_PREFIX + budgetGoal.id)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(reminderWork)
    }

    fun scheduleRecurringReminder(budgetGoal: BudgetGoal) {
        // Cancel any existing reminder
        cancelBudgetReminder(budgetGoal.id)

        // Don't schedule reminders for completed goals
        if (budgetGoal.currentAmount >= budgetGoal.targetAmount) {
            return
        }

        val repeatInterval = getRepeatInterval(budgetGoal.recurrence)

        if (repeatInterval > 0 && repeatInterval <= 7) {
            // Use PeriodicWorkRequest for daily and weekly only
            val inputData = Data.Builder()
                .putString(BUDGET_GOAL_ID_KEY, budgetGoal.id)
                .putString(BUDGET_GOAL_NAME_KEY, budgetGoal.name)
                .putDouble(BUDGET_GOAL_TARGET_KEY, budgetGoal.targetAmount)
                .putDouble(BUDGET_GOAL_CURRENT_KEY, budgetGoal.currentAmount)
                .putString(BUDGET_GOAL_RECURRENCE_KEY, budgetGoal.recurrence)
                .build()

            val reminderWork = PeriodicWorkRequestBuilder<BudgetReminderWorker>(
                repeatInterval, TimeUnit.DAYS
            )
                .setInputData(inputData)
                .addTag(WORK_TAG_PREFIX + budgetGoal.id)
                .setInitialDelay(repeatInterval, TimeUnit.DAYS) // Prevent immediate trigger
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(reminderWork)
        } else {
            // Use OneTimeWorkRequest for longer intervals (monthly, yearly, biweekly)
            scheduleBudgetReminder(budgetGoal)
        }
    }

    private fun calculateNextReminderDelay(recurrence: String): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        when (recurrence.lowercase()) {
            "daily" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 10) // 10 AM reminder
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            "weekly" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // Monday reminder
                calendar.set(Calendar.HOUR_OF_DAY, 10)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            "biweekly" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 2)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 10)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            "monthly" -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.set(Calendar.DAY_OF_MONTH, 1) // First day of month
                calendar.set(Calendar.HOUR_OF_DAY, 10)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            "yearly" -> {
                calendar.add(Calendar.YEAR, 1)
                calendar.set(Calendar.DAY_OF_YEAR, 1) // First day of year
                calendar.set(Calendar.HOUR_OF_DAY, 10)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            else -> return 0 // Unknown recurrence
        }

        val delay = calendar.timeInMillis - now

        // Ensure minimum delay of 1 hour to prevent immediate triggers
        return if (delay < 3600000) { // 1 hour in milliseconds
            delay + 86400000 // Add 24 hours if too soon
        } else {
            delay
        }
    }

    private fun getRepeatInterval(recurrence: String): Long {
        return when (recurrence.lowercase()) {
            "daily" -> 1
            "weekly" -> 7
            "biweekly" -> 0 // Use OneTimeWorkRequest for biweekly
            "monthly" -> 0 // Use OneTimeWorkRequest for monthly
            "yearly" -> 0  // Use OneTimeWorkRequest for yearly
            else -> 0
        }
    }

    fun cancelBudgetReminder(budgetGoalId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_PREFIX + budgetGoalId)
    }

    fun cancelAllBudgetReminders() {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_PREFIX)
    }

    fun updateBudgetReminder(budgetGoal: BudgetGoal) {
        // Cancel existing reminder and schedule new one
        cancelBudgetReminder(budgetGoal.id)

        if (budgetGoal.currentAmount < budgetGoal.targetAmount) {
            scheduleRecurringReminder(budgetGoal)
        }
    }
}