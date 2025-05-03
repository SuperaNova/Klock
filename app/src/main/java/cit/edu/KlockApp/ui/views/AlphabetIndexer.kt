package cit.edu.KlockApp.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class AlphabetIndexer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f // Adjust size as needed
        color = Color.DKGRAY
        textAlign = Paint.Align.CENTER
    }
    private val selectedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 35f // Slightly larger when selected
        color = Color.BLUE // Highlight color
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private var letterHeight: Float = 0f
    private var textBounds = Rect()
    private var currentSelectedIndex = -1

    var onLetterSelectedListener: ((Char) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (h > 0 && letters.isNotEmpty()) {
            letterHeight = (h - paddingTop - paddingBottom).toFloat() / letters.size
            // Adjust text size based on available height per letter, if needed
            val potentialTextSize = letterHeight * 0.7f // Example: 70% of the slot height
            textPaint.textSize = min(textPaint.textSize, potentialTextSize)
            selectedTextPaint.textSize = min(selectedTextPaint.textSize, potentialTextSize * 1.1f) // Slightly larger selected
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (letterHeight <= 0) return

        val viewWidth = width.toFloat()
        var currentY = paddingTop.toFloat()

        for (i in letters.indices) {
            val letter = letters[i]
            val paint = if (i == currentSelectedIndex) selectedTextPaint else textPaint
            // Get text bounds to center vertically within the letter's slot
            paint.getTextBounds(letter.toString(), 0, 1, textBounds)
            val textX = viewWidth / 2
            // Center vertically within the allocated slot for the letter
            val textY = currentY + (letterHeight / 2) + (textBounds.height() / 2f) - textBounds.bottom

            canvas.drawText(letter.toString(), textX, textY, paint)
            currentY += letterHeight
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (letterHeight <= 0) return super.onTouchEvent(event)

        val y = event.y
        val action = event.action

        // Calculate index based on touch position relative to padding
        val effectiveHeight = height - paddingTop - paddingBottom
        val touchIndex = ((y - paddingTop) / effectiveHeight * letters.size).toInt()
        val validIndex = max(0, min(letters.size - 1, touchIndex))

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (validIndex != currentSelectedIndex) {
                    currentSelectedIndex = validIndex
                    onLetterSelectedListener?.invoke(letters[currentSelectedIndex])
                    invalidate() // Redraw to show selection highlight
                }
                return true // Consume the event
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentSelectedIndex = -1 // Clear selection
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
} 