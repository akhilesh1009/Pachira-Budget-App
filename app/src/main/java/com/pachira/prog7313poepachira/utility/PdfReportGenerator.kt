package com.pachira.prog7313poepachira.utility

import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import android.view.View
import com.pachira.prog7313poepachira.data.Category
import com.pachira.prog7313poepachira.data.Transaction
import com.pachira.prog7313poepachira.data.Wallet
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PdfReportGenerator() {

    companion object {
        private const val TAG = "PdfReportGenerator"
        private const val PAGE_WIDTH = 595 // A4 width in points
        private const val PAGE_HEIGHT = 842 // A4 height in points
        private const val MARGIN = 50
        private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)
        private const val MAX_TRANSACTIONS_PER_PAGE = 25
    }

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun generateFinancialReport(
        chartView: View,
        pieChartView: View?,
        transactions: List<Transaction>,
        wallet: Wallet?,
        startDate: Calendar,
        endDate: Calendar,
        totalIncome: Double,
        totalExpenses: Double,
        incomeCategoryAmounts: Map<String, Double>,
        expenseCategoryAmounts: Map<String, Double>,
        categoriesMap: Map<String, Any> // For category names
    ): File? {
        try {
            // Create PDF document
            val pdfDocument = PdfDocument()
            var pageNumber = 1

            // Create first page with header and summary
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            var currentY = drawHeader(canvas, wallet, startDate, endDate)
            currentY = drawSummarySection(canvas, currentY, totalIncome, totalExpenses)

            // Draw line chart on first page
            val lineChartHeight = drawLineChart(canvas, currentY, chartView)
            currentY += lineChartHeight

            // Check if there's enough space for pie chart on first page
            val pieChartHeight = if (pieChartView != null) 250f else 0f

            // If pie chart won't fit on first page, create a new page for it
            if (pieChartView != null && currentY + pieChartHeight > PAGE_HEIGHT - MARGIN - 50f) {
                // Finish first page
                pdfDocument.finishPage(page)

                // Create new page for pie chart
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN + 40f

                // Draw pie chart on new page
                // Draw income pie chart
                if (incomeCategoryAmounts.isNotEmpty()) {
                    if (currentY + 300f > PAGE_HEIGHT - MARGIN - 50f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = MARGIN + 40f
                    }
                    currentY = drawCategoryPieChart(canvas, currentY, "Income Distribution", incomeCategoryAmounts, categoriesMap, "#4CAF50")
                }

                // Draw expense pie chart
                if (expenseCategoryAmounts.isNotEmpty()) {
                    if (currentY + 300f > PAGE_HEIGHT - MARGIN - 50f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = MARGIN + 40f
                    }
                    currentY = drawCategoryPieChart(canvas, currentY, "Expense Distribution", expenseCategoryAmounts, categoriesMap, "#F44336")
                }
            } else if (pieChartView != null) {
                // Draw income pie chart
                if (incomeCategoryAmounts.isNotEmpty()) {
                    if (currentY + 300f > PAGE_HEIGHT - MARGIN - 50f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = MARGIN + 40f
                    }
                    currentY = drawCategoryPieChart(canvas, currentY, "Income Distribution", incomeCategoryAmounts, categoriesMap, "#4CAF50")
                }

                // Draw expense pie chart
                if (expenseCategoryAmounts.isNotEmpty()) {
                    if (currentY + 300f > PAGE_HEIGHT - MARGIN - 50f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = MARGIN + 40f
                    }
                    currentY = drawCategoryPieChart(canvas, currentY, "Expense Distribution", expenseCategoryAmounts, categoriesMap, "#F44336")
                }
            }

            // Finish current page
            pdfDocument.finishPage(page)

            // Create page for transactions table if needed
            if (transactions.isNotEmpty()) {
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawTransactionsTable(canvas, transactions, categoriesMap)
                pdfDocument.finishPage(page)
            }

            // Save PDF to file
            val file = savePdfToFile(pdfDocument, wallet?.name ?: "All Wallets", startDate, endDate)
            pdfDocument.close()

            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF report", e)
            return null
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        wallet: Wallet?,
        startDate: Calendar,
        endDate: Calendar
    ): Float {
        val titlePaint = Paint().apply {
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#6200EE")
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint().apply {
            textSize = 16f
            color = Color.parseColor("#333333")
            textAlign = Paint.Align.CENTER
        }

        val datePaint = Paint().apply {
            textSize = 14f
            color = Color.parseColor("#666666")
            textAlign = Paint.Align.CENTER
        }

        var y = MARGIN + 40f

        // App title
        canvas.drawText("Pachira Financial Report", PAGE_WIDTH / 2f, y, titlePaint)
        y += 40f

        // Wallet name
        val walletName = wallet?.name ?: "All Wallets"
        canvas.drawText("Wallet: $walletName", PAGE_WIDTH / 2f, y, subtitlePaint)
        y += 30f

        // Date range
        val dateRange = "${dateFormatter.format(startDate.time)} - ${dateFormatter.format(endDate.time)}"
        canvas.drawText("Period: $dateRange", PAGE_WIDTH / 2f, y, datePaint)
        y += 40f

        // Draw separator line
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, linePaint)
        y += 30f

        return y
    }

    private fun drawSummarySection(
        canvas: Canvas,
        startY: Float,
        totalIncome: Double,
        totalExpenses: Double
    ): Float {
        val sectionTitlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#333333")
        }

        val labelPaint = Paint().apply {
            textSize = 14f
            color = Color.parseColor("#666666")
        }

        val incomeValuePaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#4CAF50")
            textAlign = Paint.Align.RIGHT
        }

        val expenseValuePaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#F44336")
            textAlign = Paint.Align.RIGHT
        }

        val backgroundPaint = Paint().apply {
            color = Color.parseColor("#F8F9FA")
            style = Paint.Style.FILL
        }

        var y = startY

        // Section title
        canvas.drawText("Financial Summary", MARGIN.toFloat(), y, sectionTitlePaint)
        y += 40f

        // Background rectangle for summary
        val summaryRect = RectF(
            MARGIN.toFloat(),
            y - 20f,
            (PAGE_WIDTH - MARGIN).toFloat(),
            y + 80f
        )
        canvas.drawRoundRect(summaryRect, 8f, 8f, backgroundPaint)

        // Income
        canvas.drawText("Total Income:", MARGIN + 20f, y, labelPaint)
        canvas.drawText(
            currencyFormatter.format(totalIncome),
            PAGE_WIDTH - MARGIN - 20f,
            y,
            incomeValuePaint
        )
        y += 25f

        // Expenses
        canvas.drawText("Total Expenses:", MARGIN + 20f, y, labelPaint)
        canvas.drawText(
            currencyFormatter.format(totalExpenses),
            PAGE_WIDTH - MARGIN - 20f,
            y,
            expenseValuePaint
        )
        y += 25f

        // Net Balance
        val netBalance = totalIncome - totalExpenses
        val balanceValuePaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            color = if (netBalance >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("Net Balance:", MARGIN + 20f, y, labelPaint)
        canvas.drawText(
            currencyFormatter.format(netBalance),
            PAGE_WIDTH - MARGIN - 20f,
            y,
            balanceValuePaint
        )
        y += 50f

        return y
    }

    private fun drawLineChart(canvas: Canvas, startY: Float, chartView: View): Float {
        val sectionTitlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#333333")
        }

        var y = startY

        // Section title
        canvas.drawText("Income vs Expenses Trend", MARGIN.toFloat(), y, sectionTitlePaint)
        y += 30f

        // Capture and draw line chart
        val lineChartBitmap = captureViewAsBitmap(chartView)
        if (lineChartBitmap != null) {
            val chartWidth = CONTENT_WIDTH * 0.8f
            val chartHeight = chartWidth * 0.6f
            val chartX = MARGIN + (CONTENT_WIDTH - chartWidth) / 2f

            val destRect = RectF(chartX, y, chartX + chartWidth, y + chartHeight)
            canvas.drawBitmap(lineChartBitmap, null, destRect, null)
            y += chartHeight + 20f

            // Return total height used
            return chartHeight + 50f
        }

        return 0f
    }

    private fun drawCategoryPieChart(
        canvas: Canvas,
        startY: Float,
        title: String,
        categoryAmounts: Map<String, Double>,
        categoriesMap: Map<String, Any>,
        accentColor: String
    ): Float {
        val sectionTitlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#333333")
        }

        var y = startY

        // Section title
        canvas.drawText(title, MARGIN.toFloat(), y, sectionTitlePaint)
        y += 30f

        // Calculate pie chart dimensions
        val pieSize = minOf(CONTENT_WIDTH * 0.4f, 200f)
        val pieX = MARGIN + (CONTENT_WIDTH - pieSize) / 2f
        val centerX = pieX + pieSize / 2f
        val centerY = y + pieSize / 2f
        val radius = pieSize / 2f - 10f

        // Create RectF for drawing arcs
        val pieRect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Calculate total and prepare data
        val total = categoryAmounts.values.sum()
        val sortedCategories = categoryAmounts.toList().sortedByDescending { it.second }

        // Draw pie chart segments
        if (total > 0) {
            var startAngle = 0f

            // Paint for pie slices
            val slicePaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // Paint for slice borders
            val borderPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.WHITE
                isAntiAlias = true
            }

            sortedCategories.forEach { (categoryId, amount) ->
                val sweepAngle = (amount / total * 360).toFloat()
                val categoryName = getCategoryName(categoryId, categoriesMap)
                val color = getCategoryColorForPdf(categoryName)

                // Set slice color
                slicePaint.color = Color.parseColor(color)

                // Draw the slice
                canvas.drawArc(pieRect, startAngle, sweepAngle, true, slicePaint)

                // Draw border around slice
                canvas.drawArc(pieRect, startAngle, sweepAngle, true, borderPaint)

                startAngle += sweepAngle
            }

            // Draw center circle for donut effect (optional)
            val centerPaint = Paint().apply {
                style = Paint.Style.FILL
                color = Color.WHITE
                isAntiAlias = true
            }
            val centerRadius = radius * 0.4f
            canvas.drawCircle(centerX, centerY, centerRadius, centerPaint)

            // Draw border around center circle
            val centerBorderPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.parseColor("#E0E0E0")
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY, centerRadius, centerBorderPaint)

            // Draw total amount in center
            val totalTextPaint = Paint().apply {
                textSize = 14f
                color = Color.parseColor("#333333")
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(currencyFormatter.format(total), centerX, centerY, totalTextPaint)

        } else {
            // Draw empty state
            val emptyPaint = Paint().apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#F0F0F0")
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY, radius, emptyPaint)

            val emptyTextPaint = Paint().apply {
                textSize = 14f
                color = Color.parseColor("#999999")
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("No Data", centerX, centerY + 5f, emptyTextPaint)
        }

        y += pieSize + 20f

        // Draw legend with real data
        y = drawRealDataLegend(canvas, y, categoryAmounts, categoriesMap)

        return y + 40f
    }

    private fun drawRealDataLegend(
        canvas: Canvas,
        startY: Float,
        categoryAmounts: Map<String, Double>,
        categoriesMap: Map<String, Any>
    ): Float {
        // Paint objects for legend
        val legendTitlePaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#333333")
            textAlign = Paint.Align.LEFT
        }

        val legendTextPaint = Paint().apply {
            textSize = 11f
            typeface = Typeface.DEFAULT
            color = Color.parseColor("#444444")
            textAlign = Paint.Align.LEFT
        }

        val legendValuePaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#666666")
            textAlign = Paint.Align.LEFT
        }

        val legendSmallValuePaint = Paint().apply {
            textSize = 9f
            typeface = Typeface.DEFAULT
            color = Color.parseColor("#888888")
            textAlign = Paint.Align.LEFT
        }

        val colorPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val backgroundPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FAFAFA")
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.parseColor("#DDDDDD")
            isAntiAlias = true
        }

        val indicatorBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.parseColor("#CCCCCC")
            isAntiAlias = true
        }

        val total = categoryAmounts.values.sum()
        val sortedCategories = categoryAmounts.toList().sortedByDescending { it.second }

        var y = startY
        val legendWidth = CONTENT_WIDTH * 0.8f
        val legendHeight = (sortedCategories.size * 28f) + 40f

        // Calculate the centered X position for the legend
        val startX = (PAGE_WIDTH - legendWidth) / 2f

        // Draw legend background
        val legendRect = RectF(startX, y - 15f, startX + legendWidth, y + legendHeight)
        canvas.drawRoundRect(legendRect, 8f, 8f, backgroundPaint)
        canvas.drawRoundRect(legendRect, 8f, 8f, borderPaint)

        // Draw legend title
        canvas.drawText("Category Breakdown", startX + 10f, y + 5f, legendTitlePaint)
        y += 25f

        // Draw separator line
        val separatorPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }
        canvas.drawLine(startX, y, startX + legendWidth - 20f, y, separatorPaint)
        y += 15f

        // First pass: calculate raw percentages
        val rawPercentages = sortedCategories.map { (_, amount) ->
            ((amount / total) * 100).toInt()
        }

        // Calculate how much we need to adjust to reach 100%
        val percentageSum = rawPercentages.sum()
        val adjustment = 100 - percentageSum

        // Distribute the adjustment among the categories
        val adjustedPercentages = mutableListOf<Int>()
        var remainingAdjustment = adjustment

        // Second pass: adjust percentages
        for (i in rawPercentages.indices) {
            if (i == rawPercentages.size - 1) {
                // Last item gets whatever adjustment is left to ensure exactly 100%
                adjustedPercentages.add(rawPercentages[i] + remainingAdjustment)
            } else if (remainingAdjustment > 0 && rawPercentages[i] > 0) {
                // Add 1 to percentages until adjustment is distributed
                adjustedPercentages.add(rawPercentages[i] + 1)
                remainingAdjustment--
            } else if (remainingAdjustment < 0 && rawPercentages[i] > 1) {
                // Subtract 1 from percentages until adjustment is distributed
                adjustedPercentages.add(rawPercentages[i] - 1)
                remainingAdjustment++
            } else {
                // No adjustment needed or possible for this item
                adjustedPercentages.add(rawPercentages[i])
            }
        }

        // Draw legend items with adjusted percentages
        sortedCategories.forEachIndexed { index, (categoryId, amount) ->
            val percentage = adjustedPercentages[index]
            val categoryName = getCategoryName(categoryId, categoriesMap)
            val color = getCategoryColorForPdf(categoryName)

            // Draw colored indicator
            colorPaint.color = Color.parseColor(color)
            val indicatorRect = RectF(startX + 10f, y - 8f, startX + 22f, y + 4f)
            canvas.drawRoundRect(indicatorRect, 3f, 3f, colorPaint)
            canvas.drawRoundRect(indicatorRect, 3f, 3f, indicatorBorderPaint)

            // Draw category name
            canvas.drawText(categoryName, startX + 30f, y, legendTextPaint)

            // Draw percentage and value
            val percentageText = "$percentage%"
            val valueText = currencyFormatter.format(amount)
            val valueWidth = legendSmallValuePaint.measureText(valueText)

            canvas.drawText(percentageText, startX + legendWidth - 80f, y - 2f, legendValuePaint)
            canvas.drawText(valueText, startX + legendWidth - 20f - valueWidth, y + 10f, legendSmallValuePaint)

            y += 28f
        }

        // Draw total
        y += 5f
        val totalLinePaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            strokeWidth = 1f
        }
        canvas.drawLine(startX + 10f, y, startX + legendWidth - 20f, y, totalLinePaint)

        y += 15f
        val totalLabelPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#333333")
        }

        val totalValuePaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#6200EE")
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("Total:", startX + 10f, y, totalLabelPaint)
        canvas.drawText(currencyFormatter.format(total), startX + legendWidth - 20f, y, totalValuePaint)

        return y
    }

    private fun getCategoryName(categoryId: String, categoriesMap: Map<String, Any>): String {
        // Try to get the category from the map
        val category = categoriesMap[categoryId]

        // If the category exists and has a name property, return it
        if (category is Category) {
            return category.name
        }

        // If it's a Map with a name key, return that
        if (category is Map<*, *> && category.containsKey("name")) {
            return category["name"].toString()
        }

        // Fallback to the category ID if we can't find a proper name
        return categoryId
    }

    private fun getCategoryColorForPdf(categoryName: String): String {
        return when (categoryName.lowercase()) {
            "bills", "utilities", "electricity", "water" -> "#00B894"
            "health", "healthcare", "medical", "pharmacy", "doctor" -> "#E17055"
            "car", "vehicle", "automobile" -> "#0984E3"
            "transport", "transportation", "fuel", "uber" -> "#FF9800"
            "education", "books", "courses", "school", "university" -> "#6C5CE7"
            "home", "house", "rent", "mortgage" -> "#A29BFE"
            "travel", "vacation", "holiday", "trip" -> "#74B9FF"
            "entertainment", "movies", "games", "fun" -> "#FD79A8"
            "groceries", "supermarket" -> "#FDCB6E"
            "food", "restaurant", "dining" -> "#4CAF50"
            "salary", "income", "wages", "bonus" -> "#36D1DC"
            "gift", "donation", "charity" -> "#FFDD00"
            "investment", "stocks", "crypto", "savings" -> "#FF5733"
            "shopping", "clothes", "retail", "amazon" -> "#2196F3"
            else -> "#6366F1" // Default color
        }
    }

    private fun drawTransactionsTable(canvas: Canvas, transactions: List<Transaction>, categoriesMap: Map<String, Any>) {
        val titlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#333333")
        }

        val headerPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#FFFFFF")
        }

        val cellPaint = Paint().apply {
            textSize = 10f
            color = Color.parseColor("#333333")
        }

        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#6200EE")
            style = Paint.Style.FILL
        }

        val rowBgPaint = Paint().apply {
            color = Color.parseColor("#F8F9FA")
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        var y = MARGIN + 40f

        // Table title
        canvas.drawText("Transaction Details", MARGIN.toFloat(), y, titlePaint)
        y += 40f

        // Table dimensions
        val tableWidth = CONTENT_WIDTH.toFloat()
        val rowHeight = 30f
        val colWidths = floatArrayOf(
            tableWidth * 0.15f, // Date
            tableWidth * 0.25f, // Description
            tableWidth * 0.20f, // Category
            tableWidth * 0.15f, // Type
            tableWidth * 0.25f  // Amount
        )

        // Draw header
        var x = MARGIN.toFloat()
        val headerRect = RectF(x, y, x + tableWidth, y + rowHeight)
        canvas.drawRect(headerRect, headerBgPaint)
        canvas.drawRect(headerRect, borderPaint)

        val headers = arrayOf("Date", "Description", "Category", "Type", "Amount")
        var currentX = x
        headers.forEachIndexed { index, header ->
            canvas.drawText(
                header,
                currentX + colWidths[index] / 2f,
                y + rowHeight / 2f + 5f,
                headerPaint.apply { textAlign = Paint.Align.CENTER }
            )
            currentX += colWidths[index]
        }
        y += rowHeight

        // Draw transaction rows
        transactions.take(MAX_TRANSACTIONS_PER_PAGE).forEachIndexed { index, transaction -> // Limit to 25 transactions per page
            val isEvenRow = index % 2 == 0
            currentX = x

            // Row background
            val rowRect = RectF(currentX, y, currentX + tableWidth, y + rowHeight)
            if (isEvenRow) {
                canvas.drawRect(rowRect, rowBgPaint)
            }
            canvas.drawRect(rowRect, borderPaint)

            // Create separate paint objects for different alignments and colors
            val centerAlignPaint = Paint().apply {
                textSize = 10f
                color = Color.parseColor("#333333")
                textAlign = Paint.Align.CENTER
            }

            val leftAlignPaint = Paint().apply {
                textSize = 10f
                color = Color.parseColor("#333333")
                textAlign = Paint.Align.LEFT
            }

            val rightAlignPaint = Paint().apply {
                textSize = 10f
                textAlign = Paint.Align.RIGHT
            }

            val incomeTypePaint = Paint().apply {
                textSize = 10f
                color = Color.parseColor("#4CAF50")
                textAlign = Paint.Align.CENTER
            }

            val expenseTypePaint = Paint().apply {
                textSize = 10f
                color = Color.parseColor("#F44336")
                textAlign = Paint.Align.CENTER
            }

            val incomeAmountPaint = Paint().apply {
                textSize = 10f
                color = Color.parseColor("#4CAF50")
                textAlign = Paint.Align.RIGHT
            }

            val expenseAmountPaint = Paint().apply {
                textSize = 10f
                color = Color.parseColor("#F44336")
                textAlign = Paint.Align.RIGHT
            }

            // Date
            val dateText = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(transaction.date))
            canvas.drawText(
                dateText,
                currentX + colWidths[0] / 2f,
                y + rowHeight / 2f + 5f,
                centerAlignPaint
            )
            currentX += colWidths[0]

            // Description
            val description = transaction.description.take(15) + if (transaction.description.length > 15) "..." else ""
            canvas.drawText(
                description,
                currentX + 5f,
                y + rowHeight / 2f + 5f,
                leftAlignPaint
            )
            currentX += colWidths[1]

            // Category - Use the category name instead of ID
            val categoryName = getCategoryName(transaction.categoryId, categoriesMap)
            canvas.drawText(
                categoryName,
                currentX + colWidths[2] / 2f,
                y + rowHeight / 2f + 5f,
                centerAlignPaint
            )
            currentX += colWidths[2]

            // Type
            val typePaint = if (transaction.type == "income") incomeTypePaint else expenseTypePaint
            canvas.drawText(
                transaction.type.capitalize(Locale.getDefault()),
                currentX + colWidths[3] / 2f,
                y + rowHeight / 2f + 5f,
                typePaint
            )
            currentX += colWidths[3]

            // Amount
            val amountPaint = if (transaction.type == "income") incomeAmountPaint else expenseAmountPaint
            canvas.drawText(
                currencyFormatter.format(transaction.amount),
                currentX + colWidths[4] - 5f,
                y + rowHeight / 2f + 5f,
                amountPaint
            )

            y += rowHeight
        }

        // Add note if more transactions exist
        if (transactions.size > MAX_TRANSACTIONS_PER_PAGE) {
            y += 20f
            val notePaint = Paint().apply {
                textSize = 10f
                color = Color.parseColor("#666666")
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "Showing first 25 transactions of ${transactions.size} total",
                PAGE_WIDTH / 2f,
                y,
                notePaint
            )
        }
    }

    private fun captureViewAsBitmap(view: View): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing view as bitmap", e)
            null
        }
    }

    private fun savePdfToFile(
        pdfDocument: PdfDocument,
        walletName: String,
        startDate: Calendar,
        endDate: Calendar
    ): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "Pachira_Report_${walletName.replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(startDate.time)}.pdf"
            val file = File(downloadsDir, fileName)

            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()

            Log.d(TAG, "PDF saved to: ${file.absolutePath}")
            file
        } catch (e: IOException) {
            Log.e(TAG, "Error saving PDF file", e)
            null
        }
    }

}
