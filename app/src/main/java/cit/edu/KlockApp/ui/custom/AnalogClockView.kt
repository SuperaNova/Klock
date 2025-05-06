package cit.edu.KlockApp.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import cit.edu.KlockApp.R // Import R class
import java.util.Calendar
import java.util.TimeZone
import androidx.core.content.ContextCompat // Import ContextCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Customizable properties with defaults
    private var hourHandColor = Color.BLACK
    private var minuteHandColor = Color.BLACK
    private var secondHandColor = Color.RED
    private var borderColor = Color.DKGRAY
    private var markerColor = Color.DKGRAY
    private var clockBackgroundColor = Color.TRANSPARENT
    private var showSecondHand = true
    private var borderWidth = 8f
    private var markerWidth = 4f
    private var hourHandWidth = 12f
    private var minuteHandWidth = 8f
    private var secondHandWidth = 4f

    private var timeZone: TimeZone = TimeZone.getDefault()
    private val calendar: Calendar = Calendar.getInstance()
    private var currentTimeMillis: Long = System.currentTimeMillis() // Store time

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var radius: Float = 0f
    private var centerX: Float = 0f
    private var centerY: Float = 0f

    init {
        // Load attributes from XML
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AnalogClockView, defStyleAttr, 0)
        try {
            hourHandColor = typedArray.getColor(R.styleable.AnalogClockView_hourHandColor, hourHandColor)
            minuteHandColor = typedArray.getColor(R.styleable.AnalogClockView_minuteHandColor, minuteHandColor)
            secondHandColor = typedArray.getColor(R.styleable.AnalogClockView_secondHandColor, secondHandColor)
            borderColor = typedArray.getColor(R.styleable.AnalogClockView_borderColor, borderColor)
            markerColor = typedArray.getColor(R.styleable.AnalogClockView_markerColor, markerColor)
            clockBackgroundColor = typedArray.getColor(R.styleable.AnalogClockView_clockBackgroundColor, clockBackgroundColor)
            showSecondHand = typedArray.getBoolean(R.styleable.AnalogClockView_showSecondHand, showSecondHand)
            borderWidth = typedArray.getDimension(R.styleable.AnalogClockView_borderWidth, borderWidth)
            markerWidth = typedArray.getDimension(R.styleable.AnalogClockView_markerWidth, markerWidth)
            hourHandWidth = typedArray.getDimension(R.styleable.AnalogClockView_hourHandWidth, hourHandWidth)
            minuteHandWidth = typedArray.getDimension(R.styleable.AnalogClockView_minuteHandWidth, minuteHandWidth)
            secondHandWidth = typedArray.getDimension(R.styleable.AnalogClockView_secondHandWidth, secondHandWidth)
        } finally {
            typedArray.recycle()
        }
    }

    fun setTimeZone(timeZoneId: String) {
        this.timeZone = TimeZone.getTimeZone(timeZoneId)
        invalidate() // Redraw when timezone changes
    }

    fun setTimeMillis(timeMillis: Long) {
        this.currentTimeMillis = timeMillis
        if (visibility == View.VISIBLE) { // Only invalidate if visible
           invalidate() // Request redraw
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) / 2f * 0.8f // Use 80% of available radius
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        calendar.timeZone = timeZone
        calendar.timeInMillis = currentTimeMillis 

        drawBackground(canvas)
        drawClockFace(canvas)
        drawHands(canvas)
    }
    
    private fun drawBackground(canvas: Canvas) {
        if (clockBackgroundColor != Color.TRANSPARENT) {
            paint.color = clockBackgroundColor
            paint.style = Paint.Style.FILL
            canvas.drawCircle(centerX, centerY, radius + borderWidth / 2, paint) // Draw slightly larger for background
        }
    }

    private fun drawClockFace(canvas: Canvas) {
        // Border
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidth
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Markers
        paint.color = markerColor
        paint.strokeWidth = markerWidth
        for (i in 1..12) {
            val angle = Math.PI / 6 * (i - 3)
            val startX = centerX + cos(angle).toFloat() * radius * 0.9f
            val startY = centerY + sin(angle).toFloat() * radius * 0.9f
            val endX = centerX + cos(angle).toFloat() * radius
            val endY = centerY + sin(angle).toFloat() * radius
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }

    private fun drawHands(canvas: Canvas) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        // Hour hand
        paint.color = hourHandColor
        paint.strokeWidth = hourHandWidth
        drawHand(canvas, (hour % 12 + minute / 60f) * 5f, true)

        // Minute hand
        paint.color = minuteHandColor
        paint.strokeWidth = minuteHandWidth
        drawHand(canvas, minute.toFloat(), false)

        // Second hand (optional)
        if (showSecondHand) {
            paint.color = secondHandColor
            paint.strokeWidth = secondHandWidth
            drawHand(canvas, second.toFloat(), false)
        }
    }

    private fun drawHand(canvas: Canvas, value: Float, isHour: Boolean) {
        val angle = Math.PI * value / 30 - Math.PI / 2
        val handRadius = if (isHour) radius * 0.5f else radius * 0.7f
        canvas.drawLine(
            centerX,
            centerY,
            centerX + cos(angle).toFloat() * handRadius,
            centerY + sin(angle).toFloat() * handRadius,
            paint
        )
    }
} 