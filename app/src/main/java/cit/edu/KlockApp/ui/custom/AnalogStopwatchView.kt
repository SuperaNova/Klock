package cit.edu.KlockApp.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnalogStopwatchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Main hand properties
    private var handColor = Color.RED
    private var handWidth = 6f

    // Lap hand properties
    private var lapHandColor = Color.BLUE // Different color for lap hand
    private var lapHandWidth = 4f // Slightly thinner

    // Clock face properties
    private var markerColor = Color.DKGRAY
    private var borderColor = Color.DKGRAY
    private var markerWidth = 4f
    private var borderWidth = 8f

    private var elapsedTimeMillis: Long = 0L
    private var lastLapResetElapsedTime: Long = -1L // Time when lap hand was last reset

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var radius: Float = 0f
    private var centerX: Float = 0f
    private var centerY: Float = 0f

    // No internal timer needed, time is set externally

    fun setElapsedTime(milliseconds: Long) {
        this.elapsedTimeMillis = milliseconds
        postInvalidate() // Redraw when time changes
    }

    // Method to call when a lap is recorded
    fun recordLap() {
        this.lastLapResetElapsedTime = this.elapsedTimeMillis
        postInvalidate()
    }

    // Method to call when stopwatch is reset
    fun resetLaps() {
        this.lastLapResetElapsedTime = -1L // Reset lap state
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) / 2f * 0.85f // Slightly larger radius for stopwatch feel
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawClockFace(canvas)
        drawLapHand(canvas) // Draw lap hand first (behind main)
        drawHand(canvas)
        drawCenterPivot(canvas)
    }

    private fun drawClockFace(canvas: Canvas) {
        // Border
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidth
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Markers (every 5 seconds, smaller ticks in between)
        paint.color = markerColor
        for (i in 0 until 60) {
            val angle = Math.PI / 30 * i - Math.PI / 2
            val isFiveSecondMark = i % 5 == 0
            val markerLengthRatio = if (isFiveSecondMark) 0.9f else 0.95f
            paint.strokeWidth = if (isFiveSecondMark) markerWidth else markerWidth / 2
            
            val startX = centerX + cos(angle).toFloat() * radius * markerLengthRatio
            val startY = centerY + sin(angle).toFloat() * radius * markerLengthRatio
            val endX = centerX + cos(angle).toFloat() * radius
            val endY = centerY + sin(angle).toFloat() * radius
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }

    private fun drawHand(canvas: Canvas) {
        // Calculate seconds from elapsed time
        val totalSeconds = elapsedTimeMillis / 1000f
        val displaySeconds = totalSeconds % 60 // Only show 0-59 seconds on main dial

        paint.color = handColor
        paint.strokeWidth = handWidth
        
        // Angle based on seconds
        val angle = Math.PI * displaySeconds / 30 - Math.PI / 2
        val handRadius = radius * 0.8f // Single hand representing seconds

        canvas.drawLine(
            centerX,
            centerY,
            centerX + cos(angle).toFloat() * handRadius,
            centerY + sin(angle).toFloat() * handRadius,
            paint
        )
        
        // Optional: Center pivot point
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, handWidth, paint)
    }

    // Function to draw the lap hand
    private fun drawLapHand(canvas: Canvas) {
        val lapTimeToShowMillis: Long = if (lastLapResetElapsedTime < 0L) {
            // Before first lap or after reset: Mirror the main hand
            elapsedTimeMillis
        } else {
            // After a lap: Show time elapsed since that lap
            elapsedTimeMillis - lastLapResetElapsedTime
        }

        val totalSeconds = lapTimeToShowMillis / 1000f
        val displaySeconds = totalSeconds % 60

        paint.color = lapHandColor
        paint.strokeWidth = lapHandWidth
        paint.style = Paint.Style.STROKE

        val angle = Math.PI * displaySeconds / 30 - Math.PI / 2
        val handRadius = radius * 0.8f // Same length as main hand

        canvas.drawLine(
            centerX,
            centerY,
            centerX + cos(angle).toFloat() * handRadius,
            centerY + sin(angle).toFloat() * handRadius,
            paint
        )
    }

    // Function to draw the center pivot
    private fun drawCenterPivot(canvas: Canvas) {
         // Draw main pivot slightly larger than main hand width
        paint.color = handColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, handWidth * 1.2f, paint)
        // Overlay lap pivot only if a lap has been recorded and not reset
        if (lastLapResetElapsedTime >= 0L) {
             paint.color = lapHandColor
             canvas.drawCircle(centerX, centerY, lapHandWidth * 1.2f, paint)
        }
    }
} 