package com.pachira.prog7313poepachira.utility

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pachira.prog7313poepachira.BudgetsActivity
import com.pachira.prog7313poepachira.R
import com.pachira.prog7313poepachira.data.BudgetGoal
import java.text.NumberFormat
import java.util.*

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "budget_reminders"
        const val CHANNEL_NAME = "Budget Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications to remind you to add funds to your budget goals"
        const val NOTIFICATION_ID_BASE = 1000
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showBudgetReminderNotification(budgetGoal: BudgetGoal) {
        val intent = Intent(context, BudgetsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("budget_goal_id", budgetGoal.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            budgetGoal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingAmount = budgetGoal.targetAmount - budgetGoal.currentAmount
        val progressPercentage = if (budgetGoal.targetAmount > 0) {
            ((budgetGoal.currentAmount / budgetGoal.targetAmount) * 100).toInt()
        } else 0

        val title = "Budget Reminder: ${budgetGoal.name}"
        val message = when {
            remainingAmount <= 0 -> "Congratulations! You've reached your budget goal!"
            progressPercentage < 25 -> "Time to add funds to your budget! ${currencyFormatter.format(remainingAmount)} remaining"
            progressPercentage < 75 -> "Keep going! ${currencyFormatter.format(remainingAmount)} left to reach your goal"
            else -> "Almost there! Only ${currencyFormatter.format(remainingAmount)} more to go!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.img)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_money,
                "Add Funds",
                createAddFundsIntent(budgetGoal)
            )
            .build()

        // Check permission before showing notification
        if (hasNotificationPermission()) {
            try {
                notificationManager.notify(
                    NOTIFICATION_ID_BASE + budgetGoal.id.hashCode(),
                    notification
                )
            } catch (e: SecurityException) {
                // Handle the case where permission was revoked between check and call
                android.util.Log.w("NotificationHelper", "Failed to show notification: ${e.message}")
            }
        } else {
            android.util.Log.w("NotificationHelper", "Notification permission not granted")
        }
    }

    private fun createAddFundsIntent(budgetGoal: BudgetGoal): PendingIntent {
        val intent = Intent(context, BudgetsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("budget_goal_id", budgetGoal.id)
            putExtra("show_add_funds", true)
        }

        return PendingIntent.getActivity(
            context,
            (budgetGoal.id + "_add_funds").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelBudgetReminder(budgetGoalId: String) {
        if (hasNotificationPermission()) {
            try {
                notificationManager.cancel(NOTIFICATION_ID_BASE + budgetGoalId.hashCode())
            } catch (e: SecurityException) {
                android.util.Log.w("NotificationHelper", "Failed to cancel notification: ${e.message}")
            }
        }
    }

    fun showGoalAchievedNotification(budgetGoal: BudgetGoal) {
        android.util.Log.d("NotificationHelper", "=== GOAL ACHIEVED NOTIFICATION ===")
        android.util.Log.d("NotificationHelper", "Goal: ${budgetGoal.name}")
        android.util.Log.d("NotificationHelper", "Target: ${budgetGoal.targetAmount}")
        android.util.Log.d("NotificationHelper", "Current: ${budgetGoal.currentAmount}")
        android.util.Log.d("NotificationHelper", "Has permission: ${hasNotificationPermission()}")
        android.util.Log.d("NotificationHelper", "Notifications enabled: ${NotificationManagerCompat.from(context).areNotificationsEnabled()}")

        // Check if notifications are enabled at system level
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            android.util.Log.w("NotificationHelper", "Notifications are disabled at system level")
            return
        }

        val intent = Intent(context, BudgetsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("budget_goal_id", budgetGoal.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (budgetGoal.id + "_achieved").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = NOTIFICATION_ID_BASE + (budgetGoal.id + "_achieved").hashCode()
        android.util.Log.d("NotificationHelper", "Using notification ID: $notificationId")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.img)
            .setContentTitle("🎉 Goal Achieved!")
            .setContentText("Congratulations! You've reached your goal for ${budgetGoal.name}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Congratulations! You've successfully reached your budget goal for ${budgetGoal.name}. Total saved: ${currencyFormatter.format(budgetGoal.targetAmount)}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // Check permission before showing notification
        if (hasNotificationPermission()) {
            try {
                android.util.Log.d("NotificationHelper", "Attempting to show notification...")
                notificationManager.notify(notificationId, notification)
                android.util.Log.d("NotificationHelper", "✅ Goal achieved notification sent successfully!")

                // Also try to show with NotificationManager directly as backup
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                systemNotificationManager.notify(notificationId + 1, notification)
                android.util.Log.d("NotificationHelper", "✅ Backup notification also sent!")

            } catch (e: SecurityException) {
                android.util.Log.e("NotificationHelper", "❌ SecurityException: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationHelper", "❌ Exception showing notification: ${e.message}")
            }
        } else {
            android.util.Log.w("NotificationHelper", "❌ Notification permission not granted")

            // Log detailed permission info
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPostNotifications = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                android.util.Log.w("NotificationHelper", "POST_NOTIFICATIONS permission: $hasPostNotifications")
            }
        }
    }
}
