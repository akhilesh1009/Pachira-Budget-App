package com.pachira.prog7313poepachira.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val TAG = "LineChartView"

    // Data points for income and expenses
    private var incomePoints: List<DataPoint> = emptyList()
    private var expensePoints: List<DataPoint> = emptyList()

    // Animation properties
    private var animationProgress = 0f
    private var animator: ValueAnimator? = null
    private val animationDuration = 1500L

    // Touch and tooltip properties
    private var selectedPoint: DataPoint? = null
    private var selectedPointType: String? = null // "income" or "expense"
    private var touchX = 0f
    private var touchY = 0f
    private var showTooltip = false
    private val touchRadius = 50f // Radius for touch detection
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    // Paint objects
    private var incomeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#4CAF50")  // Purple for income
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var expenseLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#F44336")  // Red for expenses
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val incomeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Shader will be set in onSizeChanged
    }

    private val expenseFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Shader will be set in onSizeChanged
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E0E0E0") // Light grey vertical grid
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666") // Dark grey for text
        textSize = 28f // Slightly smaller to fit more labels
        textAlign = Paint.Align.CENTER
    }

    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Data point paints
    private val incomePointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4CAF50")
    }

    private val expensePointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F44336")
    }

    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 3f
    }

    private val selectedPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFD700") // Gold color for selected point
    }

    // Tooltip paints
    private val tooltipBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        setShadowLayer(8f, 0f, 4f, Color.parseColor("#40000000"))
    }

    private val tooltipStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 2f
    }

    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 32f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
    }

    private val tooltipSubTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textSize = 28f
        textAlign = Paint.Align.LEFT
    }

    // Paths for drawing
    private val incomePath = Path()
    private val expensePath = Path()
    private val incomeFillPath = Path()
    private val expenseFillPath = Path()

    // Chart dimensions
    private var paddingTop = 50f
    private var paddingBottom = 80f
    private var paddingLeft = 40f  // Reduced left padding since no y-axis
    private var paddingRight = 40f
    private var chartWidth = 0f
    private var chartHeight = 0f

    // Value range for scaling
    private var minValue = 0f
    private var maxValue = 0f

    // Data class for chart points
    data class DataPoint(
        val date: Long,
        val value: Float,
        val label: String
    )

    // Set data for the chart
    fun setData(incomeData: List<DataPoint>, expenseData: List<DataPoint>) {
        animator?.cancel()

        this.incomePoints = incomeData
        this.expensePoints = expenseData

        // Clear selection when data changes
        selectedPoint = null
        selectedPointType = null
        showTooltip = false

        Log.d(TAG, "Setting data - Income points: ${incomeData.size}, Expense points: ${expenseData.size}")
        incomeData.forEachIndexed { index, point ->
            Log.d(TAG, "Income point $index: ${point.label} = ${point.value}")
        }
        expenseData.forEachIndexed { index, point ->
            Log.d(TAG, "Expense point $index: ${point.label} = ${point.value}")
        }

        val allValues = incomeData.map { it.value } + expenseData.map { it.value }
        if (allValues.isNotEmpty()) {
            minValue = allValues.minOrNull() ?: 0f
            maxValue = allValues.maxOrNull() ?: 0f

            val range = maxValue - minValue
            minValue -= range * 0.1f
            maxValue += range * 0.1f

            if (minValue < 0) minValue = 0f
            if (maxValue <= minValue) maxValue = minValue + 1f
        } else {
            minValue = 0f
            maxValue = 100f
        }

        startAnimation()
        invalidate()
    }

    fun clearData() {
        animator?.cancel()

        incomePoints = emptyList()
        expensePoints = emptyList()
        selectedPoint = null
        selectedPointType = null
        showTooltip = false
        animationProgress = 0f
        invalidate()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate chart area with padding
        chartWidth = w - paddingLeft - paddingRight
        chartHeight = h - paddingTop - paddingBottom

        // Update gradient shaders
        updateShaders(h)
    }

    private fun updateShaders(height: Int) {
        incomeFillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, height - paddingBottom,
            intArrayOf(
                Color.argb(128, 76, 175, 80), // Semi-transparent green
                Color.argb(8, 76, 175, 80)    // Very transparent green
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        expenseFillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, height - paddingBottom,
            intArrayOf(
                Color.parseColor("#80F44336"), // Semi-transparent red (#F44336 with 50% alpha)
                Color.parseColor("#10F44336")  // Very transparent red
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y

                // Find the closest data point
                val closestPoint = findClosestDataPoint(touchX, touchY)
                if (closestPoint != null) {
                    selectedPoint = closestPoint.first
                    selectedPointType = closestPoint.second
                    showTooltip = true
                    invalidate()
                    return true
                } else {
                    // Clear selection if no point is close enough
                    if (showTooltip) {
                        selectedPoint = null
                        selectedPointType = null
                        showTooltip = false
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                // Keep tooltip visible after touch up
                return showTooltip
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findClosestDataPoint(x: Float, y: Float): Pair<DataPoint, String>? {
        var closestPoint: DataPoint? = null
        var closestType: String? = null
        var minDistance = Float.MAX_VALUE

        // Check income points - only consider non-zero values
        incomePoints.forEachIndexed { index, point ->
            if (point.value > 0) { // Only check non-zero points
                val pointX = paddingLeft + (index * chartWidth / (incomePoints.size - 1).coerceAtLeast(1))
                val normalizedValue = (point.value - minValue) / (maxValue - minValue)
                val pointY = height - paddingBottom - (normalizedValue * chartHeight)

                val distance = sqrt((x - pointX) * (x - pointX) + (y - pointY) * (y - pointY))
                if (distance < touchRadius && distance < minDistance) {
                    minDistance = distance
                    closestPoint = point
                    closestType = "income"
                }
            }
        }

        // Check expense points - only consider non-zero values
        expensePoints.forEachIndexed { index, point ->
            if (point.value > 0) { // Only check non-zero points
                val pointX = paddingLeft + (index * chartWidth / (expensePoints.size - 1).coerceAtLeast(1))
                val normalizedValue = (point.value - minValue) / (maxValue - minValue)
                val pointY = height - paddingBottom - (normalizedValue * chartHeight)

                val distance = sqrt((x - pointX) * (x - pointX) + (y - pointY) * (y - pointY))
                if (distance < touchRadius && distance < minDistance) {
                    minDistance = distance
                    closestPoint = point
                    closestType = "expense"
                }
            }
        }

        return closestPoint?.let { point ->
            closestType?.let { type ->
                point to type
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (incomePoints.isEmpty() && expensePoints.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        // Draw grid lines
        drawGrid(canvas)

        // Draw income line and fill
        if (incomePoints.isNotEmpty()) {
            drawLineWithFill(canvas, incomePoints, incomePath, incomeFillPath, incomeLinePaint, incomeFillPaint)
        }

        // Draw expense line and fill
        if (expensePoints.isNotEmpty()) {
            drawLineWithFill(canvas, expensePoints, expensePath, expenseFillPath, expenseLinePaint, expenseFillPaint)
        }

        // Draw data points
        drawDataPoints(canvas)

        // Draw x-axis labels
        drawXLabels(canvas)

        // Draw legend
        drawLegend(canvas)

        // Draw tooltip if a point is selected
        if (showTooltip && selectedPoint != null && selectedPointType != null) {
            drawTooltip(canvas)
        }
    }

    private fun drawDataPoints(canvas: Canvas) {
        val pointRadius = 6f
        val selectedPointRadius = 10f

        // Draw income points
        incomePoints.forEachIndexed { index, point ->
            val x = paddingLeft + (index * chartWidth / (incomePoints.size - 1).coerceAtLeast(1))
            val normalizedValue = (point.value - minValue) / (maxValue - minValue)
            val y = height - paddingBottom - (normalizedValue * chartHeight)

            // Only draw points with non-zero values
            if (point.value > 0) {
                val isSelected = selectedPoint == point && selectedPointType == "income"
                val radius = if (isSelected) selectedPointRadius else pointRadius
                val fillPaint = if (isSelected) selectedPointPaint else incomePointPaint

                // Draw point with white stroke
                canvas.drawCircle(x, y, radius + 2f, pointStrokePaint)
                canvas.drawCircle(x, y, radius, fillPaint)
            }
        }

        // Draw expense points
        expensePoints.forEachIndexed { index, point ->
            val x = paddingLeft + (index * chartWidth / (expensePoints.size - 1).coerceAtLeast(1))
            val normalizedValue = (point.value - minValue) / (maxValue - minValue)
            val y = height - paddingBottom - (normalizedValue * chartHeight)

            // Only draw points with non-zero values
            if (point.value > 0) {
                val isSelected = selectedPoint == point && selectedPointType == "expense"
                val radius = if (isSelected) selectedPointRadius else pointRadius
                val fillPaint = if (isSelected) selectedPointPaint else expensePointPaint

                // Draw point with white stroke
                canvas.drawCircle(x, y, radius + 2f, pointStrokePaint)
                canvas.drawCircle(x, y, radius, fillPaint)
            }
        }
    }

    private fun drawTooltip(canvas: Canvas) {
        val point = selectedPoint ?: return
        val type = selectedPointType ?: return

        // Find the position of the selected point
        val pointIndex = if (type == "income") {
            incomePoints.indexOf(point)
        } else {
            expensePoints.indexOf(point)
        }

        if (pointIndex == -1) return

        val points = if (type == "income") incomePoints else expensePoints
        val pointX = paddingLeft + (pointIndex * chartWidth / (points.size - 1).coerceAtLeast(1))
        val normalizedValue = (point.value - minValue) / (maxValue - minValue)
        val pointY = height - paddingBottom - (normalizedValue * chartHeight)

        // Prepare tooltip text
        val typeText = if (type == "income") "Income" else "Expense"
        val amountText = currencyFormatter.format(point.value.toDouble())
        val periodText = point.label

        // Measure text dimensions
        val typeTextWidth = tooltipTextPaint.measureText(typeText)
        val amountTextWidth = tooltipTextPaint.measureText(amountText)
        val periodTextWidth = tooltipSubTextPaint.measureText(periodText)

        val maxTextWidth = maxOf(typeTextWidth, amountTextWidth, periodTextWidth)
        val tooltipWidth = maxTextWidth + 32f // 16dp padding on each side
        val tooltipHeight = 120f // Height for 3 lines of text + padding

        // Calculate tooltip position
        var tooltipX = pointX - tooltipWidth / 2
        var tooltipY = pointY - tooltipHeight - 20f // 20f offset above the point

        // Adjust if tooltip goes outside bounds
        if (tooltipX < 10f) tooltipX = 10f
        if (tooltipX + tooltipWidth > width - 10f) tooltipX = width - tooltipWidth - 10f
        if (tooltipY < 10f) tooltipY = pointY + 30f // Show below point if no space above

        // Draw tooltip background with rounded corners
        val rect = RectF(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight)
        canvas.drawRoundRect(rect, 12f, 12f, tooltipBackgroundPaint)
        canvas.drawRoundRect(rect, 12f, 12f, tooltipStrokePaint)

        // Draw tooltip text
        val textX = tooltipX + 16f
        var textY = tooltipY + 30f

        // Type text
        tooltipTextPaint.color = if (type == "income") Color.parseColor("#8b5cf6") else Color.parseColor("#F44336")
        canvas.drawText(typeText, textX, textY, tooltipTextPaint)

        // Amount text
        textY += 35f
        tooltipTextPaint.color = Color.BLACK
        canvas.drawText(amountText, textX, textY, tooltipTextPaint)

        // Period text
        textY += 30f
        canvas.drawText(periodText, textX, textY, tooltipSubTextPaint)

        // Draw small triangle pointing to the data point
        drawTooltipPointer(canvas, pointX, pointY, tooltipX + tooltipWidth / 2, tooltipY + tooltipHeight)
    }

    private fun drawTooltipPointer(canvas: Canvas, pointX: Float, pointY: Float, tooltipCenterX: Float, tooltipBottomY: Float) {
        val pointerPath = Path()
        val pointerSize = 12f

        if (pointY > tooltipBottomY) {
            // Point is below tooltip, draw pointer pointing down
            pointerPath.moveTo(tooltipCenterX, tooltipBottomY)
            pointerPath.lineTo(tooltipCenterX - pointerSize, tooltipBottomY + pointerSize)
            pointerPath.lineTo(tooltipCenterX + pointerSize, tooltipBottomY + pointerSize)
            pointerPath.close()
        } else {
            // Point is above tooltip, draw pointer pointing up
            pointerPath.moveTo(tooltipCenterX, tooltipBottomY - 120f) // Top of tooltip
            pointerPath.lineTo(tooltipCenterX - pointerSize, tooltipBottomY - 120f - pointerSize)
            pointerPath.lineTo(tooltipCenterX + pointerSize, tooltipBottomY - 120f - pointerSize)
            pointerPath.close()
        }

        canvas.drawPath(pointerPath, tooltipBackgroundPaint)
        canvas.drawPath(pointerPath, tooltipStrokePaint)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.parseColor("#999999")
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            "No data available",
            width / 2f,
            height / 2f,
            paint
        )
    }

    private fun drawGrid(canvas: Canvas) {
        // Draw vertical grid lines for all data points
        val points = if (incomePoints.isNotEmpty()) incomePoints else expensePoints
        if (points.isEmpty()) return

        // Draw grid lines for all data points, but limit visual grid lines to avoid clutter
        val maxGridLines = 12 // Maximum grid lines to show
        val step = if (points.size <= maxGridLines) 1 else points.size / maxGridLines

        for (i in points.indices step step) {
            val x = paddingLeft + (i * chartWidth / (points.size - 1).coerceAtLeast(1))
            canvas.drawLine(x, paddingTop, x, height - paddingBottom, gridPaint)
        }
    }

    private fun drawLineWithFill(
        canvas: Canvas,
        points: List<DataPoint>,
        path: Path,
        fillPath: Path,
        linePaint: Paint,
        fillPaint: Paint
    ) {
        if (points.size < 2) return

        path.reset()
        fillPath.reset()

        val displayedPointCount = (points.size * animationProgress).toInt().coerceAtLeast(2)

        val mappedPoints = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until displayedPointCount) {
            val x = paddingLeft + (i * chartWidth / (points.size - 1))
            val normalizedValue = (points[i].value - minValue) / (maxValue - minValue)
            val y = height - paddingBottom - (normalizedValue * chartHeight)
            mappedPoints.add(Pair(x, y))
        }

        // Start fill path at the bottom left
        fillPath.moveTo(mappedPoints.first().first, height - paddingBottom)

        mappedPoints.forEachIndexed { i, (x, y) ->
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                val prevX = mappedPoints[i - 1].first
                val prevY = mappedPoints[i - 1].second
                val controlX = (prevX + x) / 2
                path.quadTo(controlX, prevY, x, y)
                fillPath.quadTo(controlX, prevY, x, y)
            }
        }

        // Complete fill path
        val lastX = mappedPoints.last().first
        fillPath.lineTo(lastX, height - paddingBottom)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }

    // Fixed drawXLabels method to show all relevant labels
    private fun drawXLabels(canvas: Canvas) {
        val points = if (incomePoints.isNotEmpty()) incomePoints else expensePoints
        if (points.isEmpty()) return

        Log.d(TAG, "Drawing X labels for ${points.size} points")

        // Determine how many labels to show based on data type
        val maxLabels = when {
            points.size <= 7 -> points.size // Show all if 7 or fewer (days of week)
            points.size <= 12 -> points.size // Show all months
            points.size <= 31 -> 8 // For daily data, show every 4th day approximately
            else -> 6 // For other cases, show 6 labels
        }

        val step = if (points.size <= maxLabels) 1 else points.size / maxLabels

        Log.d(TAG, "Showing $maxLabels labels with step $step")

        for (i in 0 until points.size step step) {
            val x = paddingLeft + (i * chartWidth / (points.size - 1).coerceAtLeast(1))
            val label = points[i].label

            Log.d(TAG, "Drawing label '$label' at position $i, x=$x")
            canvas.drawText(label, x, height - paddingBottom + 40f, labelPaint)
        }

        // Always show the last label if it's not already shown
        val lastIndex = points.size - 1
        if (lastIndex > 0 && (lastIndex % step) != 0) {
            val x = paddingLeft + (lastIndex * chartWidth / (points.size - 1))
            val label = points[lastIndex].label
            Log.d(TAG, "Drawing final label '$label' at position $lastIndex, x=$x")
            canvas.drawText(label, x, height - paddingBottom + 40f, labelPaint)
        }
    }

    private fun drawLegend(canvas: Canvas) {
        val legendY = paddingTop - 15f

        val boxSize = 24f
        val spacingBetweenBoxAndText = 10f
        val spacingBetweenItems = 40f

        // Calculate total width needed for legend
        val incomeTextWidth = labelPaint.measureText("Income")
        val expenseTextWidth = labelPaint.measureText("Expense")
        val totalWidth = boxSize + spacingBetweenBoxAndText + incomeTextWidth +
                spacingBetweenItems +
                boxSize + spacingBetweenBoxAndText + expenseTextWidth

        // Center the legend horizontally
        val startX = (width - totalWidth) / 2

        // Income legend
        legendPaint.color = incomeLinePaint.color
        val incomeBoxLeft = startX
        val incomeBoxTop = legendY - boxSize
        canvas.drawRect(incomeBoxLeft, incomeBoxTop, incomeBoxLeft + boxSize, legendY, legendPaint)

        val incomeTextX = incomeBoxLeft + boxSize + spacingBetweenBoxAndText
        val textY = legendY - boxSize/2 + labelPaint.textSize/3

        labelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Income", incomeTextX, textY, labelPaint)

        // Expense legend
        val expenseBoxLeft = incomeTextX + incomeTextWidth + spacingBetweenItems
        legendPaint.color = expenseLinePaint.color
        canvas.drawRect(expenseBoxLeft, incomeBoxTop, expenseBoxLeft + boxSize, legendY, legendPaint)

        val expenseTextX = expenseBoxLeft + boxSize + spacingBetweenBoxAndText
        canvas.drawText("Expense", expenseTextX, textY, labelPaint)

        // Reset text align
        labelPaint.textAlign = Paint.Align.CENTER
    }

    fun clearTouch() {
        selectedPoint = null
        selectedPointType = null
        showTooltip = false
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    // Add these methods to provide data access for PDF generation
    fun getIncomeData(): List<Float>? {
        return if (incomePoints.isNotEmpty()) {
            incomePoints.map { it.value }
        } else null
    }

    fun getExpenseData(): List<Float>? {
        return if (expensePoints.isNotEmpty()) {
            expensePoints.map { it.value }
        } else null
    }
}