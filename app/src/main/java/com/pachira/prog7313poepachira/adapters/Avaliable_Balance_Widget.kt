package com.pachira.prog7313poepachira.adapters

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pachira.prog7313poepachira.DashboardActivity
import com.pachira.prog7313poepachira.R
import java.text.NumberFormat
import java.util.*

class Avaliable_Balance_Widget : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.pachira.prog7313poepachira.UPDATE_WIDGET"

        // Static method to update widget from anywhere in your app
        fun updateWidget(context: Context) {
            val intent = Intent(context, Avaliable_Balance_Widget::class.java)
            intent.action = ACTION_UPDATE_WIDGET
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, Avaliable_Balance_Widget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, Avaliable_Balance_Widget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.avaliable__balance__widget)

    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    val intent = Intent(context, DashboardActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    // Wrap it in a PendingIntent
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Set the onClickPendingIntent on the root layout or any clickable view in your widget layout
    views.setOnClickPendingIntent(R.id.budgetSummaryContainer, pendingIntent)

    // Update widget with the RemoteViews including the click handler
    appWidgetManager.updateAppWidget(appWidgetId, views)

    val currentUser = auth.currentUser
    if (currentUser == null) {
        views.setTextViewText(R.id.tvBalanceAmount, formatCurrency(0.0))
        views.setTextViewText(R.id.tvIncomeAmount, formatCurrency(0.0))
        views.setTextViewText(R.id.tvExpenseAmount, formatCurrency(0.0))
        appWidgetManager.updateAppWidget(appWidgetId, views)
        return
    }

    val userId = currentUser.uid
    var totalIncome = 0.0
    var totalExpenses = 0.0

    database.child("users").child(userId).child("income")
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (incomeSnapshot in snapshot.children) {
                    val income = incomeSnapshot.child("amount").getValue(Double::class.java)
                    income?.let {
                        totalIncome += it
                    }
                }

                database.child("users").child(userId).child("expenses")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (expenseSnapshot in snapshot.children) {
                                val expense = expenseSnapshot.child("amount").getValue(Double::class.java)
                                expense?.let {
                                    totalExpenses += it
                                }
                            }

                            var currentBalance = totalIncome - totalExpenses
                            if (currentBalance < 0) {
                                currentBalance = 0.0
                            }

                            views.setTextViewText(R.id.tvBalanceAmount, formatCurrency(currentBalance))
                            views.setTextViewText(R.id.tvIncomeAmount, formatCurrency(totalIncome))
                            views.setTextViewText(R.id.tvExpenseAmount, formatCurrency(totalExpenses))

                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            views.setTextViewText(R.id.tvBalanceAmount, "Error")
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
                views.setTextViewText(R.id.tvBalanceAmount, "Error")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        })
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    return format.format(amount)
}