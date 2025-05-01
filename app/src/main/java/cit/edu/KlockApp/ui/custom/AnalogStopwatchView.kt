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

    // Basic appearance properties (could be made customizable later via attrs)
    private var handColor = Color.RED
    private var markerColor = Color.DKGRAY
    private var borderColor = Color.DKGRAY
    private var handWidth = 6f
    private var markerWidth = 4f
    private var borderWidth = 8f

    private var elapsedTimeMillis: Long = 0L

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var radius: Float = 0f
    private var centerX: Float = 0f
    private var centerY: Float = 0f

    // No internal timer needed, time is set externally

    fun setElapsedTime(milliseconds: Long) {
        this.elapsedTimeMillis = milliseconds
        postInvalidate() // Redraw when time changes
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
        drawHand(canvas)
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
} 