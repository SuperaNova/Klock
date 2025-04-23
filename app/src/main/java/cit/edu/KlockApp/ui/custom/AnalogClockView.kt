package cit.edu.KlockApp.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var timeZone: TimeZone = TimeZone.getDefault()
    private val calendar: Calendar = Calendar.getInstance()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var radius: Float = 0f
    private var centerX: Float = 0f
    private var centerY: Float = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            postInvalidateOnAnimation()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(tickRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(tickRunnable)
    }

    fun setTimeZone(timeZoneId: String) {
        this.timeZone = TimeZone.getTimeZone(timeZoneId)
        postInvalidate()
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
        calendar.timeInMillis = System.currentTimeMillis()

        drawClockFace(canvas)
        drawHands(canvas)
    }

    private fun drawClockFace(canvas: Canvas) {
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Basic hour markers
        paint.strokeWidth = 4f
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
        paint.color = Color.BLACK
        paint.strokeWidth = 12f
        drawHand(canvas, (hour % 12 + minute / 60f) * 5f, true)

        // Minute hand
        paint.color = Color.BLACK
        paint.strokeWidth = 8f
        drawHand(canvas, minute.toFloat(), false)

        // Second hand
        paint.color = Color.RED
        paint.strokeWidth = 4f
        drawHand(canvas, second.toFloat(), false)
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