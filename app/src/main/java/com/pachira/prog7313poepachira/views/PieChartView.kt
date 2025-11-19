package com.pachira.prog7313poepachira.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd

/**
 * Custom view for rendering an animated pie chart showing financial category distribution
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Data class for pie chart slices with percentage value, color (as hex string), and label
    data class PieSlice(
        val value: Float,       // Percentage value (0-100)
        val color: String,      // Color in hex format (e.g., "#FF5733")
        val label: String,      // Category label
        val iconResId: Int = 0  // Icon resource ID for the category (optional)
    )

    // Paint objects for drawing
    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2f
    }

    // Rectangle for drawing arcs
    private val rect = RectF()

    // Data storage
    private val data = mutableListOf<PieSlice>()
    private val animatedValues = mutableListOf<Float>()

    // Animation properties
    private var animationProgress = 0f
    private var animator: ValueAnimator? = null
    private val animationDuration = 1000L
    private var isFirstDraw = true
    private var rotationAngle = 0f
    private var rotationAnimator: ValueAnimator? = null
    private val rotationDuration = 3000L
    private var isRotating = false

    /**
     * Set new data for the pie chart with animation
     * @param newData List of PieSlice objects
     */
    fun setData(newData: List<PieSlice>) {
        // Cancel any running animations
        animator?.cancel()
        rotationAnimator?.cancel()

        // Store the new data
        data.clear()
        data.addAll(newData)

        // Initialize animated values if needed
        if (animatedValues.isEmpty()) {
            animatedValues.addAll(List(newData.size) { 0f })
        } else {
            // Resize the list if needed
            animatedValues.clear()
            animatedValues.addAll(List(newData.size) { 0f })
        }

        // Start the animation
        startAnimation()

        // Invalidate to trigger redraw
        invalidate()
    }

    /**
     * Clear all data from the pie chart
     */
    fun clearData() {
        // Cancel any running animations
        animator?.cancel()
        rotationAnimator?.cancel()

        data.clear()
        animatedValues.clear()
        animationProgress = 0f
        isFirstDraw = true

        invalidate() // Trigger redraw
    }

    /**
     * Start the animation for the pie chart
     */
    private fun startAnimation() {
        // Cancel any running animation
        animator?.cancel()

        // Create a new animator
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float

                // Update animated values
                for (i in data.indices) {
                    animatedValues[i] = data[i].value * animationProgress
                }

                invalidate() // Trigger redraw
            }

            doOnEnd {
                // Start rotation animation after the initial animation completes
                if (isFirstDraw) {
                    isFirstDraw = false
                    startRotationAnimation()
                }
            }

            start()
        }
    }

    /**
     * Start a continuous gentle rotation animation
     */
    private fun startRotationAnimation() {
        if (data.isEmpty()) return

        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = rotationDuration
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener { animation ->
                rotationAngle = animation.animatedValue as Float
                invalidate()
            }

            start()
            isRotating = true
        }
    }

    /**
     * Stop the rotation animation
     */
    fun stopRotation() {
        rotationAnimator?.cancel()
        isRotating = false
        invalidate()
    }

    /**
     * Resume the rotation animation
     */
    fun resumeRotation() {
        if (!isRotating && data.isNotEmpty()) {
            startRotationAnimation()
        }
    }

    /**
     * Check if the chart is currently rotating
     */
    fun isRotating(): Boolean {
        return isRotating
    }

    /**
     * Set whether the chart should rotate
     */
    fun setRotating(shouldRotate: Boolean) {
        if (shouldRotate && !isRotating) {
            resumeRotation()
        } else if (!shouldRotate && isRotating) {
            stopRotation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            // Draw empty state
            slicePaint.color = Color.LTGRAY
            canvas.drawCircle(width / 2f, height / 2f, minOf(width, height) / 2f - 10f, slicePaint)
            return
        }

        // Calculate the bounds of the pie chart
        val padding = 10f
        val size = minOf(width, height).toFloat() - (padding * 2)
        val centerX = width / 2f
        val centerY = height / 2f

        rect.set(
            centerX - size / 2,
            centerY - size / 2,
            centerX + size / 2,
            centerY + size / 2
        )

        var startAngle = rotationAngle // Start from the rotation angle

        // Draw each slice with animation
        for (i in data.indices) {
            try {
                // Parse color from hex string
                slicePaint.color = Color.parseColor(data[i].color)
            } catch (e: IllegalArgumentException) {
                // Fallback to default color if parsing fails
                slicePaint.color = Color.parseColor("#6366F1") // Default indigo
            }

            val sweepAngle = 360f * animatedValues[i] / 100f
            canvas.drawArc(rect, startAngle, sweepAngle, true, slicePaint)

            // Draw a thin white stroke between slices for better visibility
            canvas.drawArc(rect, startAngle, sweepAngle, true, strokePaint)

            startAngle += sweepAngle
        }

        // Draw a white circle in the center for a donut chart effect
        val centerRadius = size / 3f
        canvas.drawCircle(centerX, centerY, centerRadius, centerPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Make the view square and slightly larger than before
        val size = minOf(measuredWidth, measuredHeight)
        // Increase the size by 20% to make it larger
        val enlargedSize = (size * 1.2f).toInt()
        setMeasuredDimension(enlargedSize, enlargedSize)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Clean up animations when the view is detached
        animator?.cancel()
        rotationAnimator?.cancel()
    }

    /**
     * Get the current pie chart data for external legend creation
     */
    fun getPieData(): List<PieSlice> {
        return data.toList()
    }

    /**
     * Get the animated values for external use
     */
    fun getAnimatedValues(): List<Float> {
        return animatedValues.toList()
    }
}
