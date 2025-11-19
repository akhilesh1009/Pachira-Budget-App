package com.pachira.prog7313poepachira.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// Enum representing different confetti shapes
enum class ConfettiShape { RECTANGLE, CIRCLE, STAR }

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val confettiPieces = mutableListOf<ConfettiPiece>() // List of confetti particles
    private val paint = Paint() // Paint object for drawing
    private val random = Random() // Random generator for properties
    //(Android Developer 2025)
    private var animator: ValueAnimator? = null // Animator for driving updates

    // Color palette for confetti
    private val colors = intArrayOf(
        Color.parseColor("#FFC107"), // Yellow
        Color.parseColor("#FF5722"), // Orange
        Color.parseColor("#E91E63"), // Pink
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#3F51B5"), // Indigo
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#00BCD4"), // Cyan
        Color.parseColor("#FFEB3B")  // Light Yellow
    )

    init {
        // Initially create 150 confetti pieces
        for (i in 0 until 150) {
            confettiPieces.add(createConfettiPiece())
        }
    }

    // Generates a new confetti piece with random properties
    private fun createConfettiPiece(): ConfettiPiece {
        val shape = when (random.nextInt(3)) {
            0 -> ConfettiShape.RECTANGLE
            1 -> ConfettiShape.CIRCLE
            else -> ConfettiShape.STAR
        }

        return ConfettiPiece(
            x = random.nextFloat() * width, // May be 0 initially; recommend initializing later
            y = -50f - random.nextFloat() * 500, // Start above view
            size = 8f + random.nextFloat() * 20f, // Varying sizes
            color = colors[random.nextInt(colors.size)], // Random color
            speed = 200f + random.nextFloat() * 400f, // Falling speed
            angle = random.nextFloat() * 2 * Math.PI.toFloat(), // Horizontal movement direction
            rotationSpeed = random.nextFloat() * 15f - 7.5f, // Rotation velocity
            shape = shape,
            alpha = 200 + random.nextInt(56) // Slight transparency
        )
    }

    // Starts the confetti animation
    fun start() {
        // Reset and recreate confetti pieces
        confettiPieces.clear()
        for (i in 0 until 150) {
            confettiPieces.add(createConfettiPiece())
        }

        // Start the animator
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 5000 // 5 seconds total duration
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateConfetti(it.animatedFraction)
                invalidate() // Redraw view
            }
            start()
        }
    }

    // Updates positions and state of confetti pieces
    private fun updateConfetti(deltaTime: Float) {
        val screenHeight = height.toFloat()

        for (i in confettiPieces.indices) {
            val piece = confettiPieces[i]

            // Move down
            piece.y += piece.speed * deltaTime * 0.1f

            // Move slightly side-to-side
            piece.x += 20f * sin(piece.angle) * deltaTime

            // Rotate the piece
            piece.rotation += piece.rotationSpeed * deltaTime * 50

            // Reset if the piece goes off screen
            if (piece.y > screenHeight) {
                piece.y = -50f - random.nextFloat() * 100
                piece.x = random.nextFloat() * width
            }
        }
    }

    // Draws all confetti pieces on the canvas
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (piece in confettiPieces) {
            paint.color = piece.color
            paint.alpha = piece.alpha

            canvas.save()
            canvas.translate(piece.x, piece.y)
            canvas.rotate(piece.rotation)

            // Draw shape based on the type
            when (piece.shape) {
                ConfettiShape.RECTANGLE -> {
                    canvas.drawRect(-piece.size/2, -piece.size/2, piece.size/2, piece.size/2, paint)
                }
                ConfettiShape.CIRCLE -> {
                    canvas.drawCircle(0f, 0f, piece.size/2, paint)
                }
                ConfettiShape.STAR -> {
                    drawStar(canvas, 0f, 0f, piece.size/2, paint)
                }
            }

            canvas.restore()
        }
    }

    // Draws a 5-pointed star shape
    private fun drawStar(canvas: Canvas, x: Float, y: Float, radius: Float, paint: Paint) {
        val path = Path()
        val outerRadius = radius
        val innerRadius = radius * 0.4f

        for (i in 0 until 10) {
            val angle = Math.PI * i / 5
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val pointX = (x + r * cos(angle)).toFloat()
            val pointY = (y + r * sin(angle)).toFloat()

            if (i == 0) {
                path.moveTo(pointX, pointY)
            } else {
                path.lineTo(pointX, pointY)
            }
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    // Stops the animation
    fun stop() {
        animator?.removeAllUpdateListeners()
        animator?.cancel()
        animator = null
    }

    // Stop animation when view is removed from window
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    // Data class representing a confetti particle
    private data class ConfettiPiece(
        var x: Float,
        var y: Float,
        val size: Float,
        val color: Int,
        val speed: Float,
        val angle: Float,
        val rotationSpeed: Float,
        val shape: ConfettiShape,
        val alpha: Int,
        var rotation: Float = 0f
    )
}

//REFERENCES
// Android Developer. 2025. “ValueAnimator”.
// <https://developer.android.com/reference/android/animation/ValueAnimator>
// [accessed 1 May 2025].

