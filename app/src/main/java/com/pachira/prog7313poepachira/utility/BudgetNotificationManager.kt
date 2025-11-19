package com.pachira.prog7313poepachira.utility

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pachira.prog7313poepachira.data.BudgetGoal

class BudgetNotificationManager(private val context: Context) {

    companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    private val reminderScheduler = BudgetReminderScheduler(context)
    private val notificationHelper = NotificationHelper(context)

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissions granted by default on older versions
        }
    }

    fun setupBudgetReminders(budgetGoals: List<BudgetGoal>) {
        if (!hasNotificationPermission()) {
            return
        }

        budgetGoals.forEach { budgetGoal ->
            if (budgetGoal.currentAmount < budgetGoal.targetAmount) {
                reminderScheduler.scheduleRecurringReminder(budgetGoal)
            }
        }
    }

    fun updateBudgetReminder(budgetGoal: BudgetGoal) {
        if (!hasNotificationPermission()) {
            return
        }

        reminderScheduler.updateBudgetReminder(budgetGoal)
    }

    fun cancelBudgetReminder(budgetGoalId: String) {
        reminderScheduler.cancelBudgetReminder(budgetGoalId)
    }

    fun showGoalAchievedNotification(budgetGoal: BudgetGoal) {
        if (!hasNotificationPermission()) {
            return
        }

        notificationHelper.showGoalAchievedNotification(budgetGoal)
    }

    fun testNotification(budgetGoal: BudgetGoal) {
        if (!hasNotificationPermission()) {
            return
        }

        notificationHelper.showBudgetReminderNotification(budgetGoal)
    }
}
