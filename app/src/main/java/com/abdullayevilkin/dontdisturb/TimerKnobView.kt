package com.abdullayevilkin.dontdisturb

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.google.android.material.color.MaterialColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Rotary knob for picking the auto-off timer. One full turn covers
 * [minValue]..[maxValue] minutes in [step] minute steps. Drag around the ring
 * (or tap on it) to set the value; the label in the middle shows the time.
 */
class TimerKnobView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var minValue = 5
    var maxValue = 240
    var step = 5

    /** Selected minutes. Setting it from code does not call [onChange]. */
    var minutes: Int = 30
        set(value) {
            field = snap(value.toDouble())
            contentDescription = label()
            invalidate()
        }

    var onChange: ((Int) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val strokeWidth = 16f * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@TimerKnobView.strokeWidth
        color = MaterialColors.getColor(this@TimerKnobView, com.google.android.material.R.attr.colorSurfaceVariant)
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@TimerKnobView.strokeWidth
        color = MaterialColors.getColor(this@TimerKnobView, androidx.appcompat.R.attr.colorPrimary)
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = MaterialColors.getColor(this@TimerKnobView, androidx.appcompat.R.attr.colorPrimary)
    }
    private val thumbCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = MaterialColors.getColor(this@TimerKnobView, com.google.android.material.R.attr.colorOnPrimary)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 30f, resources.displayMetrics
        )
        isFakeBoldText = true
        color = MaterialColors.getColor(this@TimerKnobView, com.google.android.material.R.attr.colorOnSurface)
    }

    private val arcRect = RectF()

    init {
        contentDescription = label()
    }

    private fun label(): String {
        val m = minutes
        val h = m / 60
        val r = m % 60
        return when {
            h == 0 -> context.getString(R.string.knob_min, r)
            r == 0 -> context.getString(R.string.knob_h, h)
            else -> context.getString(R.string.knob_h_min, h, r)
        }
    }

    private fun snap(raw: Double): Int {
        val steps = ((raw - minValue) / step).roundToInt()
        return (minValue + steps * step).coerceIn(minValue, maxValue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - strokeWidth * 1.2f
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)

        val fraction = (minutes - minValue).toFloat() / (maxValue - minValue)
        val sweep = fraction * 360f
        if (sweep > 0f) canvas.drawArc(arcRect, -90f, sweep, false, progressPaint)

        val angle = Math.toRadians((sweep - 90f).toDouble())
        val tx = cx + radius * cos(angle).toFloat()
        val ty = cy + radius * sin(angle).toFloat()
        canvas.drawCircle(tx, ty, strokeWidth * 1.1f, thumbPaint)
        canvas.drawCircle(tx, ty, strokeWidth * 0.4f, thumbCorePaint)

        val baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label(), cx, baseline, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                update(event, isStart = true)
            }
            MotionEvent.ACTION_MOVE -> update(event, isStart = false)
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun update(event: MotionEvent, isStart: Boolean) {
        val cx = width / 2f
        val cy = height / 2f
        var deg = Math.toDegrees(atan2((event.y - cy).toDouble(), (event.x - cx).toDouble())) + 90.0
        if (deg < 0) deg += 360.0

        val range = maxValue - minValue
        val raw = minValue + deg / 360.0 * range
        val current = minutes

        val next = if (!isStart && abs(raw - current) > range / 2.0) {
            // Finger crossed the top: stop at the end instead of wrapping around.
            if (current > minValue + range / 2) maxValue else minValue
        } else {
            snap(raw)
        }

        if (next != current) {
            minutes = next
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onChange?.invoke(next)
        }
    }
}
