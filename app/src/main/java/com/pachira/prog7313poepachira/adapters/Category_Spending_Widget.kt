package com.pachira.prog7313poepachira.adapters

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pachira.prog7313poepachira.R
import com.pachira.prog7313poepachira.TrendsActivity
import com.pachira.prog7313poepachira.data.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Implementation of App Widget functionality.
 */
class Category_Spending_Widget : AppWidgetProvider() {

    companion object {
        const val TAG = "CategorySpendingWidget"
        const val ACTION_UPDATE_WIDGET = "com.pachira.prog7313poepachira.UPDATE_SPENDING_WIDGET"
        private const val CHART_CACHE_FILENAME = "chart_cache_v3.png" // Updated version

        // Static method to update widget from anywhere in your app
        fun updateWidgets(context: Context) {
            Log.d(TAG, "updateWidgets called - triggering widget refresh")
            val intent = Intent(context, Category_Spending_Widget::class.java)
            intent.action = ACTION_UPDATE_WIDGET
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, Category_Spending_Widget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        }

        // Helper method to get the cache file
        fun getChartCacheFile(context: Context): File {
            val cacheDir = context.cacheDir
            return File(cacheDir, CHART_CACHE_FILENAME)
        }

        // Helper method to clear the cache file
        fun clearCache(context: Context) {
            val cacheFile = getChartCacheFile(context)
            if (cacheFile.exists()) {
                try {
                    cacheFile.delete()
                    Log.d(TAG, "Cache file deleted successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete cache file: ${e.message}")
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateCategorySpendingWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Handle data update action
        if (intent.action == ACTION_UPDATE_WIDGET) {
            Log.d(TAG, "onReceive: UPDATE_WIDGET action received")
            // Clear cache to force fresh data
            clearCache(context)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, Category_Spending_Widget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(TAG, "Widget enabled")
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d(TAG, "Widget disabled")

        // Clean up cached chart file
        clearCache(context)
    }
}

internal fun updateCategorySpendingWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    Log.d(Category_Spending_Widget.TAG, "Updating widget ID: $appWidgetId")

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.category__spending__widget)

    // Set loading text
    views.setTextViewText(R.id.appwidget_text, "Loading financial data...")
    appWidgetManager.updateAppWidget(appWidgetId, views)

    // Add click intent to open the TrendsActivity
    val intent = Intent(context, TrendsActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)
    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

    // Try to load cached chart first
    val cacheFile = Category_Spending_Widget.getChartCacheFile(context)
    var usedCachedChart = false

    if (cacheFile.exists() && cacheFile.length() > 0) {
        try {
            Log.d(Category_Spending_Widget.TAG, "Attempting to load cached chart from: ${cacheFile.absolutePath}")
            val cachedBitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)

            if (cachedBitmap != null && !cachedBitmap.isRecycled && cachedBitmap.width > 0) {
                Log.d(Category_Spending_Widget.TAG, "Successfully loaded cached chart: ${cachedBitmap.width}x${cachedBitmap.height}")
                views.setImageViewBitmap(R.id.widget_chart, cachedBitmap)
                usedCachedChart = true

                // Update the widget with cached chart immediately
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } else {
                Log.w(Category_Spending_Widget.TAG, "Cached bitmap was null or invalid")
            }
        } catch (e: Exception) {
            Log.e(Category_Spending_Widget.TAG, "Error loading cached chart: ${e.message}")
            e.printStackTrace()
        }
    } else {
        Log.d(Category_Spending_Widget.TAG, "No cache file exists at: ${cacheFile.absolutePath}")
    }

    // Get current month data
    loadCurrentMonthData(context) { bitmap, title, incomeTotal, expenseTotal, netBalance ->
        // Update the widget with the chart
        if (!usedCachedChart || bitmap != null) {
            views.setImageViewBitmap(R.id.widget_chart, bitmap)
        }
        views.setTextViewText(R.id.appwidget_text, title)
        views.setTextViewText(R.id.tv_income_total, incomeTotal)
        views.setTextViewText(R.id.tv_expense_total, expenseTotal)
        views.setTextViewText(R.id.tv_net_balance, netBalance)

        // Cache the bitmap for future use
        if (bitmap != null && !bitmap.isRecycled) {
            try {
                Log.d(Category_Spending_Widget.TAG, "Attempting to cache chart to: ${cacheFile.absolutePath}")

                // Ensure parent directory exists
                cacheFile.parentFile?.mkdirs()

                // Create new file if it doesn't exist
                if (!cacheFile.exists()) {
                    cacheFile.createNewFile()
                    Log.d(Category_Spending_Widget.TAG, "Created new cache file")
                }

                // Write bitmap to file
                FileOutputStream(cacheFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                    Log.d(Category_Spending_Widget.TAG, "Chart cached successfully, file size: ${cacheFile.length()} bytes")
                }

                // Verify file was written
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    Log.d(Category_Spending_Widget.TAG, "Cache file verified: ${cacheFile.length()} bytes")
                } else {
                    Log.w(Category_Spending_Widget.TAG, "Cache file verification failed")
                }
            } catch (e: Exception) {
                Log.e(Category_Spending_Widget.TAG, "Error caching chart: ${e.message}")
                e.printStackTrace()
            }
        } else {
            Log.w(Category_Spending_Widget.TAG, "Cannot cache bitmap: null or recycled")
        }

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

private fun loadCurrentMonthData(
    context: Context,
    callback: (Bitmap?, String, String, String, String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser == null) {
        // Generate sample data for demonstration
        generateSampleChart(context, callback)
        return
    }

    val database = FirebaseDatabase.getInstance().reference
    val userId = currentUser.uid

    Log.d(Category_Spending_Widget.TAG, "Loading data for user: $userId")

    // Calculate current month date range
    val calendar = Calendar.getInstance()
    val startDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val endDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
    val currentMonthTitle = "Total Cash Flow - ${monthFormat.format(calendar.time)}"

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val incomeByDay = mutableMapOf<Int, Float>()
    val expensesByDay = mutableMapOf<Int, Float>()

    // Initialize all days with zero
    for (day in 1..daysInMonth) {
        incomeByDay[day] = 0f
        expensesByDay[day] = 0f
    }

    var totalIncome = 0.0
    var totalExpenses = 0.0
    var incomeLoaded = false
    var expensesLoaded = false

    // Set timeout to prevent hanging if Firebase is slow
    val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
    val timeoutRunnable = Runnable {
        if (!incomeLoaded || !expensesLoaded) {
            Log.w(Category_Spending_Widget.TAG, "Firebase data loading timed out")

            // Calculate with whatever data we have
            val netBalance = totalIncome - totalExpenses

            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            val incomeText = currencyFormatter.format(totalIncome)
            val expenseText = currencyFormatter.format(totalExpenses)
            val netBalanceText = currencyFormatter.format(netBalance)

            try {
                val chartBitmap = generateDualLineChartBitmap(incomeByDay, expensesByDay, context)
                callback(chartBitmap, currentMonthTitle, incomeText, expenseText, netBalanceText)
            } catch (e: Exception) {
                Log.e(Category_Spending_Widget.TAG, "Error generating chart: ${e.message}")
                e.printStackTrace()
                callback(null, currentMonthTitle, incomeText, expenseText, netBalanceText)
            }
        }
    }
    timeoutHandler.postDelayed(timeoutRunnable, 10000) // 10 second timeout

    // Load income data
    database.child("users").child(userId).child("income")
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(Category_Spending_Widget.TAG, "Income data loaded: ${snapshot.childrenCount} transactions")

                snapshot.children.forEach { transactionSnapshot ->
                    val transaction = transactionSnapshot.getValue(Transaction::class.java)
                    transaction?.let {
                        val transactionDate = Date(it.date)
                        totalIncome += it.amount

                        // Check if transaction is in current month
                        if (transactionDate.time >= startDate.timeInMillis &&
                            transactionDate.time <= endDate.timeInMillis) {

                            val transactionCal = Calendar.getInstance().apply { time = transactionDate }
                            val day = transactionCal.get(Calendar.DAY_OF_MONTH)
                            incomeByDay[day] = (incomeByDay[day] ?: 0f) + it.amount.toFloat()
                        }
                    }
                }

                incomeLoaded = true
                timeoutHandler.removeCallbacks(timeoutRunnable)

                // Check if we can generate the chart now
                if (expensesLoaded) {
                    generateChartAndCallback(
                        incomeByDay, expensesByDay, totalIncome, totalExpenses,
                        context, currentMonthTitle, callback
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(Category_Spending_Widget.TAG, "Error loading income data: ${error.message}")
                incomeLoaded = true
                timeoutHandler.removeCallbacks(timeoutRunnable)

                // Check if we can generate the chart now
                if (expensesLoaded) {
                    generateChartAndCallback(
                        incomeByDay, expensesByDay, totalIncome, totalExpenses,
                        context, currentMonthTitle, callback
                    )
                }
            }
        })

    // Load expense data
    database.child("users").child(userId).child("expenses")
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(Category_Spending_Widget.TAG, "Expense data loaded: ${snapshot.childrenCount} transactions")

                snapshot.children.forEach { transactionSnapshot ->
                    val transaction = transactionSnapshot.getValue(Transaction::class.java)
                    transaction?.let {
                        val transactionDate = Date(it.date)
                        totalExpenses += it.amount

                        // Check if transaction is in current month
                        if (transactionDate.time >= startDate.timeInMillis &&
                            transactionDate.time <= endDate.timeInMillis) {

                            val transactionCal = Calendar.getInstance().apply { time = transactionDate }
                            val day = transactionCal.get(Calendar.DAY_OF_MONTH)
                            expensesByDay[day] = (expensesByDay[day] ?: 0f) + it.amount.toFloat()
                        }
                    }
                }

                expensesLoaded = true
                timeoutHandler.removeCallbacks(timeoutRunnable)

                // Check if we can generate the chart now
                if (incomeLoaded) {
                    generateChartAndCallback(
                        incomeByDay, expensesByDay, totalIncome, totalExpenses,
                        context, currentMonthTitle, callback
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(Category_Spending_Widget.TAG, "Error loading expense data: ${error.message}")
                expensesLoaded = true
                timeoutHandler.removeCallbacks(timeoutRunnable)

                // Check if we can generate the chart now
                if (incomeLoaded) {
                    generateChartAndCallback(
                        incomeByDay, expensesByDay, totalIncome, totalExpenses,
                        context, currentMonthTitle, callback
                    )
                }
            }
        })
}

private fun generateChartAndCallback(
    incomeByDay: Map<Int, Float>,
    expensesByDay: Map<Int, Float>,
    totalIncome: Double,
    totalExpenses: Double,
    context: Context,
    currentMonthTitle: String,
    callback: (Bitmap?, String, String, String, String) -> Unit
) {
    val netBalance = totalIncome - totalExpenses

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    val incomeText = currencyFormatter.format(totalIncome)
    val expenseText = currencyFormatter.format(totalExpenses)
    val netBalanceText = currencyFormatter.format(netBalance)

    Log.d(Category_Spending_Widget.TAG, "Generating chart with income: $incomeText, expenses: $expenseText, net: $netBalanceText")

    try {
        val chartBitmap = generateDualLineChartBitmap(incomeByDay, expensesByDay, context)
        callback(chartBitmap, currentMonthTitle, incomeText, expenseText, netBalanceText)
    } catch (e: Exception) {
        Log.e(Category_Spending_Widget.TAG, "Error generating chart: ${e.message}")
        e.printStackTrace()
        callback(null, currentMonthTitle, incomeText, expenseText, netBalanceText)
    }
}

private fun generateSampleChart(
    context: Context,
    callback: (Bitmap?, String, String, String, String) -> Unit
) {
    // Generate sample data for demonstration
    val calendar = Calendar.getInstance()
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val incomeData = mutableMapOf<Int, Float>()
    val expenseData = mutableMapOf<Int, Float>()

    // Initialize all days with zero
    for (day in 1..daysInMonth) {
        incomeData[day] = 0f
        expenseData[day] = 0f
    }

    val random = Random()

    // Add sample income data (typically on specific days like salary days)
    incomeData[1] = 15000f + random.nextFloat() * 5000f // Salary at beginning of month
    incomeData[15] = 2000f + random.nextFloat() * 1000f // Mid-month bonus or freelance

    // Add sample expense data throughout the month
    for (day in 1..daysInMonth) {
        // Random daily expenses
        expenseData[day] = random.nextFloat() * 800f

        // Higher expenses on weekends
        val dayOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, day)
        }.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            expenseData[day] = expenseData[day]!! * 1.5f
        }
    }

    val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
    val currentMonthTitle = "Total Cash Flow - ${monthFormat.format(calendar.time)}"

    val totalIncome = incomeData.values.sum()
    val totalExpenses = expenseData.values.sum()
    val netBalance = totalIncome - totalExpenses

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    val incomeText = currencyFormatter.format(totalIncome)
    val expenseText = currencyFormatter.format(totalExpenses)
    val netBalanceText = currencyFormatter.format(netBalance)

    val chartBitmap = generateDualLineChartBitmap(incomeData, expenseData, context)
    callback(chartBitmap, currentMonthTitle, incomeText, expenseText, netBalanceText)
}

private fun generateDualLineChartBitmap(
    incomeByDay: Map<Int, Float>,
    expensesByDay: Map<Int, Float>,
    context: Context
): Bitmap {
    // Create a bitmap for the chart - reduced size for better memory management
    val width = 720
    val height = 360
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Fill background
    val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

    // Chart dimensions - adjusted for legend at top
    val paddingLeft = 50f
    val paddingRight = 30f
    val paddingTop = 60f // More space for legend
    val paddingBottom = 50f
    val chartWidth = width - paddingLeft - paddingRight
    val chartHeight = height - paddingTop - paddingBottom

    val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)

    // Find max value for scaling (consider both income and expenses)
    val allValues = incomeByDay.values + expensesByDay.values
    val maxValue = allValues.maxOrNull() ?: 1f
    val scaleFactor = if (maxValue > 0) chartHeight / (maxValue * 1.1f) else 1f // Add 10% margin at top

    // Changed income color from purple to green
    val incomeColor = Color.parseColor("#4CAF50") // Green color for income

    // Create paints for income line
    val incomeLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = incomeColor // Green for income
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    val incomeFillPaint = Paint().apply {
        style = Paint.Style.FILL
        shader = LinearGradient(
            0f, paddingTop, 0f, height - paddingBottom,
            intArrayOf(
                Color.argb(128, 76, 175, 80), // Semi-transparent green
                Color.argb(8, 76, 175, 80)    // Very transparent green
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        isAntiAlias = true
    }

    // Create paints for expense line
    val expenseLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#F44336") // Red for expenses
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    val expenseFillPaint = Paint().apply {
        style = Paint.Style.FILL
        shader = LinearGradient(
            0f, paddingTop, 0f, height - paddingBottom,
            intArrayOf(
                Color.parseColor("#40F44336"), // Semi-transparent red
                Color.parseColor("#08F44336")  // Very transparent red
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        isAntiAlias = true
    }

    // Point paints
    val incomePointPaint = Paint().apply {
        style = Paint.Style.FILL
        color = incomeColor // Green for income points
        isAntiAlias = true
    }

    val expensePointPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F44336")
        isAntiAlias = true
    }

    val pointStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2f
        isAntiAlias = true
    }

    val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E8E8E8") // Light grey
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        isAntiAlias = true
    }

    val labelPaint = Paint().apply {
        color = Color.parseColor("#666666") // Dark grey
        textSize = 16f // Adjusted text size for smaller chart
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    val legendPaint = Paint().apply {
        color = Color.parseColor("#666666")
        textSize = 24f // Adjusted legend text size
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    val valuePaint = Paint().apply {
        color = Color.parseColor("#333333")
        textSize = 14f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // Draw horizontal grid lines
    val numGridLines = 3
    for (i in 1..numGridLines) {
        val y = height - paddingBottom - (i * chartHeight / numGridLines)
        canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
    }

    // Draw vertical grid lines (every 5 days, starting at day 1)
    for (day in 1..daysInMonth step 5) {
        val x = paddingLeft + ((day - 1) * chartWidth / (daysInMonth - 1))
        canvas.drawLine(x, paddingTop, x, height - paddingBottom, gridPaint)
    }

    // Create paths for income line and fill
    val incomePath = Path()
    val incomeFillPath = Path()

    // Create paths for expense line and fill
    val expensePath = Path()
    val expenseFillPath = Path()

    // Calculate all points for income
    val incomePoints = mutableListOf<Pair<Float, Float>>()
    for (day in 1..daysInMonth) {
        val x = paddingLeft + ((day - 1) * chartWidth / (daysInMonth - 1))
        val value = incomeByDay[day] ?: 0f
        val y = height - paddingBottom - (value * scaleFactor)
        incomePoints.add(Pair(x, y))
    }

    // Calculate all points for expenses
    val expensePoints = mutableListOf<Pair<Float, Float>>()
    for (day in 1..daysInMonth) {
        val x = paddingLeft + ((day - 1) * chartWidth / (daysInMonth - 1))
        val value = expensesByDay[day] ?: 0f
        val y = height - paddingBottom - (value * scaleFactor)
        expensePoints.add(Pair(x, y))
    }

    // Draw income line and fill
    if (incomePoints.isNotEmpty()) {
        // Start fill path at bottom
        incomeFillPath.moveTo(incomePoints.first().first, height - paddingBottom)

        incomePoints.forEachIndexed { index, (x, y) ->
            if (index == 0) {
                incomePath.moveTo(x, y)
                incomeFillPath.lineTo(x, y)
            } else {
                incomePath.lineTo(x, y)
                incomeFillPath.lineTo(x, y)
            }
        }

        // Complete fill path
        incomeFillPath.lineTo(incomePoints.last().first, height - paddingBottom)
        incomeFillPath.close()

        // Draw income fill and line
        canvas.drawPath(incomeFillPath, incomeFillPaint)
        canvas.drawPath(incomePath, incomeLinePaint)
    }

    // Draw expense line and fill
    if (expensePoints.isNotEmpty()) {
        // Start fill path at bottom
        expenseFillPath.moveTo(expensePoints.first().first, height - paddingBottom)

        expensePoints.forEachIndexed { index, (x, y) ->
            if (index == 0) {
                expensePath.moveTo(x, y)
                expenseFillPath.lineTo(x, y)
            } else {
                expensePath.lineTo(x, y)
                expenseFillPath.lineTo(x, y)
            }
        }

        // Complete fill path
        expenseFillPath.lineTo(expensePoints.last().first, height - paddingBottom)
        expenseFillPath.close()

        // Draw expense fill and line
        canvas.drawPath(expenseFillPath, expenseFillPaint)
        canvas.drawPath(expensePath, expenseLinePaint)
    }

    // Currency formatter for displaying values
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    // Find highest income and expense values
    val highestIncome = incomeByDay.maxByOrNull { it.value }
    val highestExpense = expensesByDay.maxByOrNull { it.value }

    // Draw data points for income (only for non-zero values)
    incomeByDay.forEach { (day, value) ->
        if (value > 0) {
            val x = paddingLeft + ((day - 1) * chartWidth / (daysInMonth - 1))
            val y = height - paddingBottom - (value * scaleFactor)
            canvas.drawCircle(x, y, 5f, pointStrokePaint)
            canvas.drawCircle(x, y, 3f, incomePointPaint)

            // Only show value for the highest income point
            if (highestIncome != null && day == highestIncome.key && value == highestIncome.value) {
                val valueText = currencyFormatter.format(value.toDouble())

                // Create background for value text
                val textBounds = Rect()
                valuePaint.getTextBounds(valueText, 0, valueText.length, textBounds)
                val textWidth = textBounds.width() + 12
                val textHeight = textBounds.height() + 8
                val textBackgroundPaint = Paint().apply {
                    color = Color.WHITE
                    alpha = 230
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                val textBorderPaint = Paint().apply {
                    color = incomeColor // Green border for income value
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                    isAntiAlias = true
                }

                val textRect = RectF(
                    x - textWidth / 2,
                    y - textHeight - 12,
                    x + textWidth / 2,
                    y - 12
                )

                canvas.drawRoundRect(textRect, 4f, 4f, textBackgroundPaint)
                canvas.drawRoundRect(textRect, 4f, 4f, textBorderPaint)
                canvas.drawText(valueText, x, y - 16, valuePaint)
            }
        }
    }

    // Draw data points for expenses (only for non-zero values)
    expensesByDay.forEach { (day, value) ->
        if (value > 0) {
            val x = paddingLeft + ((day - 1) * chartWidth / (daysInMonth - 1))
            val y = height - paddingBottom - (value * scaleFactor)
            canvas.drawCircle(x, y, 5f, pointStrokePaint)
            canvas.drawCircle(x, y, 3f, expensePointPaint)

            // Only show value for the highest expense point
            if (highestExpense != null && day == highestExpense.key && value == highestExpense.value) {
                val valueText = currencyFormatter.format(value.toDouble())

                // Create background for value text
                val textBounds = Rect()
                valuePaint.getTextBounds(valueText, 0, valueText.length, textBounds)
                val textWidth = textBounds.width() + 12
                val textHeight = textBounds.height() + 8
                val textBackgroundPaint = Paint().apply {
                    color = Color.WHITE
                    alpha = 230
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                val textBorderPaint = Paint().apply {
                    color = Color.parseColor("#F44336")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                    isAntiAlias = true
                }

                val textRect = RectF(
                    x - textWidth / 2,
                    y - textHeight - 12,
                    x + textWidth / 2,
                    y - 12
                )

                canvas.drawRoundRect(textRect, 4f, 4f, textBackgroundPaint)
                canvas.drawRoundRect(textRect, 4f, 4f, textBorderPaint)
                canvas.drawText(valueText, x, y - 16, valuePaint)
            }
        }
    }

    // Draw day labels (every 5 days, starting at day 1)
    for (day in 1..daysInMonth step 5) {
        val x = paddingLeft + ((day - 1) * chartWidth / (daysInMonth - 1))
        canvas.drawText(day.toString(), x, height - paddingBottom + 20f, labelPaint)
    }

    // Draw styled legend above the graph (like in LineChartView with full squares)
    val legendY = 30f
    val boxSize = 32f // Adjusted square size for smaller chart
    val spacingBetweenBoxAndText = 8f
    val spacingBetweenItems = 32f

    // Calculate total width needed for legend
    val incomeTextWidth = legendPaint.measureText("Income")
    val expenseTextWidth = legendPaint.measureText("Expense")
    val totalWidth = boxSize + spacingBetweenBoxAndText + incomeTextWidth +
            spacingBetweenItems +
            boxSize + spacingBetweenBoxAndText + expenseTextWidth

    // Center the legend horizontally
    val startX = (width - totalWidth) / 2

    // Income legend - full square
    legendPaint.color = incomeLinePaint.color
    val incomeBoxLeft = startX
    val incomeBoxTop = legendY - boxSize
    canvas.drawRect(incomeBoxLeft, incomeBoxTop, incomeBoxLeft + boxSize, legendY, legendPaint)

    val incomeTextX = incomeBoxLeft + boxSize + spacingBetweenBoxAndText
    val textY = legendY - boxSize/2 + legendPaint.textSize/3

    labelPaint.textAlign = Paint.Align.LEFT
    canvas.drawText("Income", incomeTextX, textY, labelPaint)

    // Expense legend - full square
    val expenseBoxLeft = incomeTextX + incomeTextWidth + spacingBetweenItems
    legendPaint.color = expenseLinePaint.color
    canvas.drawRect(expenseBoxLeft, incomeBoxTop, expenseBoxLeft + boxSize, legendY, legendPaint)

    val expenseTextX = expenseBoxLeft + boxSize + spacingBetweenBoxAndText
    canvas.drawText("Expense", expenseTextX, textY, labelPaint)

    // Reset text align
    labelPaint.textAlign = Paint.Align.CENTER

    return bitmap
}
