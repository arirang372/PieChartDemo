package com.sung.circlegraphdisplay

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.Paint.Align
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import java.text.DecimalFormat

class CircleDisplay : View, GestureDetector.OnGestureListener {
    private val LOG_TAG = "CircleDisplay"

    /** the unit that is represented by the circle-display  */
    private var mUnit = "%"

    /** startangle of the view  */
    private var mStartAngle = 270f

    /**
     * field representing the minimum selectable value in the display - the
     * minimum interval
     */
    private var mStepSize = 1f

    /** angle that represents the displayed value  */
    private var mAngle = 0f

    /** current state of the animation  */
    private var mPhase = 0f

    /** the currently displayed value, can be percent or actual value  */
    private var mValue = 0f

    /** the maximum displayable value, depends on the set value  */
    private var mMaxValue = 0f

    /** percent of the maximum width the arc takes  */
    private var mValueWidthPercent = 50f

    /** if enabled, the inner circle is drawn  */
    private var mDrawInner = true

    /** if enabled, the center text is drawn  */
    private var mDrawText = true

    /** if enabled, touching and therefore selecting values is enabled  */
    private var mTouchEnabled = true

    /** represents the alpha value used for the remainder bar  */
    private var mDimAlpha = 80

    /** the decimalformat responsible for formatting the values in the view  */
    private var mFormatValue = DecimalFormat("###,###,###,##0.0")

    /** array that contains values for the custom-text  */
    private var mCustomText: Array<String>? = null

    /**
     * rect object that represents the bounds of the view, needed for drawing
     * the circle
     */
    private var mCircleBox = RectF()

    private var mArcPaint: Paint? = null
    private var mInnerCirclePaint: Paint? = null
    private var mTextPaint: Paint? = null

    /** object animator for doing the drawing animations  */
    private var mDrawAnimator: ObjectAnimator? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context,
        attrs,
        defStyleAttr) {
        init()
    }

    private fun init() {
        mBoxSetup = false
        mArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mArcPaint!!.style = Paint.Style.FILL
        mArcPaint!!.color = Color.rgb(192, 255, 140)
        mInnerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mInnerCirclePaint!!.style = Paint.Style.FILL
        mInnerCirclePaint!!.color = Color.WHITE
        mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint!!.style = Paint.Style.STROKE
        mTextPaint!!.textAlign = Align.CENTER
        mTextPaint!!.color = Color.BLACK
        mTextPaint!!.textSize = Utils.convertDpToPixel(resources, 24f)
        mDrawAnimator = ObjectAnimator.ofFloat(this, "phase", mPhase, 1.0f).setDuration(3000)
        mDrawAnimator!!.interpolator = AccelerateDecelerateInterpolator()
        mGestureDetector = GestureDetector(context, this)
    }

    /** boolean flag that indicates if the box has been setup  */
    private var mBoxSetup = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!mBoxSetup) {
            mBoxSetup = true
            setupBox()
        }
        drawWholeCircle(canvas)
        drawValue(canvas)
        if (mDrawInner) drawInnerCircle(canvas)
        if (mDrawText) {
            if (mCustomText != null) drawCustomText(canvas) else drawText(canvas)
        }
    }

    /**
     * draws the text in the center of the view
     *
     * @param c
     */
    private fun drawText(c: Canvas) {
        c.drawText(mFormatValue.format((mValue * mPhase).toDouble()) + " " + mUnit,
            (width / 2).toFloat(),
            height / 2 + mTextPaint!!.descent(),
            mTextPaint!!)
    }

    /**
     * draws the custom text in the center of the view
     *
     * @param c
     */
    private fun drawCustomText(c: Canvas) {
        val index = (mValue * mPhase / mStepSize).toInt()
        if (index < mCustomText!!.size) {
            c.drawText(mCustomText!![index], (width / 2).toFloat(),
                height / 2 + mTextPaint!!.descent(), mTextPaint!!)
        } else {
            Log.e(LOG_TAG, "Custom text array not long enough.")
        }
    }

    /**
     * draws the background circle with less alpha
     *
     * @param c
     */
    private fun drawWholeCircle(c: Canvas) {
        mArcPaint!!.alpha = mDimAlpha
        val r = getRadius()
        c.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), r, mArcPaint!!)
    }

    /**
     * draws the inner circle of the view
     *
     * @param c
     */
    private fun drawInnerCircle(c: Canvas) {
        c.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), getRadius() / 100f
                * (100f - mValueWidthPercent), mInnerCirclePaint!!)
    }

    /**
     * draws the actual value slice/arc
     *
     * @param c
     */
    private fun drawValue(c: Canvas) {
        mArcPaint!!.alpha = 255
        val angle = mAngle * mPhase
        c.drawArc(mCircleBox, mStartAngle, angle, true, mArcPaint!!)

        // Log.i(LOG_TAG, "CircleBox bounds: " + mCircleBox.toString() +
        // ", Angle: " + angle + ", StartAngle: " + mStartAngle);
    }

    /**
     * sets up the bounds of the view
     */
    private fun setupBox() {
        val width = width
        val height = height
        val diameter = getDiameter()
        mCircleBox = RectF(width / 2 - diameter / 2, height / 2 - diameter / 2, width / 2
                + diameter / 2, height / 2 + diameter / 2)
    }

    /**
     * shows the given value in the circle view
     *
     * @param toShow
     * @param total
     * @param animated
     */
    fun showValue(toShow: Float, total: Float, animated: Boolean) {
        mAngle = calcAngle(toShow / total * 100f)
        mValue = toShow
        mMaxValue = total
        if (animated) startAnim() else {
            mPhase = 1f
            invalidate()
        }
    }

    /**
     * Sets the unit that is displayed next to the value in the center of the
     * view. Default "%". Could be "€" or "$" or left blank or whatever it is
     * you display.
     *
     * @param unit
     */
    fun setUnit(unit: String) {
        mUnit = unit
    }

    /**
     * Returns the currently displayed value from the view. Depending on the
     * used method to show the value, this value can be percent or actual value.
     *
     * @return
     */
    fun getValue(): Float {
        return mValue
    }

    fun startAnim() {
        mPhase = 0f
        mDrawAnimator!!.start()
    }

    /**
     * set the duration of the drawing animation in milliseconds
     *
     * @param durationmillis
     */
    fun setAnimDuration(durationmillis: Int) {
        mDrawAnimator!!.duration = durationmillis.toLong()
    }

    /**
     * returns the diameter of the drawn circle/arc
     *
     * @return
     */
    fun getDiameter(): Float {
        return Math.min(width, height).toFloat()
    }

    /**
     * returns the radius of the drawn circle
     *
     * @return
     */
    fun getRadius(): Float {
        return getDiameter() / 2f
    }

    /**
     * calculates the needed angle for a given value
     *
     * @param percent
     * @return
     */
    private fun calcAngle(percent: Float): Float {
        return percent / 100f * 360f
    }

    /**
     * set the starting angle for the view
     *
     * @param angle
     */
    fun setStartAngle(angle: Float) {
        mStartAngle = angle
    }

    /**
     * returns the current animation status of the view
     *
     * @return
     */
    fun getPhase(): Float {
        return mPhase
    }

    /**
     * DONT USE THIS METHOD
     *
     * @param phase
     */
    fun setPhase(phase: Float) {
        mPhase = phase
        invalidate()
    }

    /**
     * set this to true to draw the inner circle, default: true
     *
     * @param enabled
     */
    fun setDrawInnerCircle(enabled: Boolean) {
        mDrawInner = enabled
    }

    /**
     * returns true if drawing the inner circle is enabled, false if not
     *
     * @return
     */
    fun isDrawInnerCircleEnabled(): Boolean {
        return mDrawInner
    }

    /**
     * set the drawing of the center text to be enabled or not
     *
     * @param enabled
     */
    fun setDrawText(enabled: Boolean) {
        mDrawText = enabled
    }

    /**
     * returns true if drawing the text in the center is enabled
     *
     * @return
     */
    fun isDrawTextEnabled(): Boolean {
        return mDrawText
    }

    /**
     * set the color of the arc
     *
     * @param color
     */
    fun setColor(color: Int) {
        mArcPaint!!.color = color
    }

    /**
     * set the size of the center text in dp
     *
     * @param size
     */
    fun setTextSize(size: Float) {
        mTextPaint!!.textSize = Utils.convertDpToPixel(resources, size)
    }

    /**
     * set the thickness of the value bar, default 50%
     *
     * @param percentFromTotalWidth
     */
    fun setValueWidthPercent(percentFromTotalWidth: Float) {
        mValueWidthPercent = percentFromTotalWidth
    }

    /**
     * Set an array of custom texts to be drawn instead of the value in the
     * center of the CircleDisplay. If set to null, the custom text will be
     * reset and the value will be drawn. Make sure the length of the array corresponds with the maximum number of steps (set with setStepSize(float stepsize).
     *
     * @param custom
     */
    fun setCustomText(custom: Array<String>?) {
        mCustomText = custom
    }

    /**
     * sets the number of digits used to format values
     *
     * @param digits
     */
    fun setFormatDigits(digits: Int) {
        val b = StringBuffer()
        for (i in 0 until digits) {
            if (i == 0) b.append(".")
            b.append("0")
        }
        mFormatValue = DecimalFormat("###,###,###,##0$b")
    }

    /**
     * set the aplha value to be used for the remainder of the arc, default 80
     * (use value between 0 and 255)
     *
     * @param alpha
     */
    fun setDimAlpha(alpha: Int) {
        mDimAlpha = alpha
    }

    /** paint used for drawing the text  */
    val PAINT_TEXT = 1

    /** paint representing the value bar  */
    val PAINT_ARC = 2

    /** paint representing the inner (by default white) area  */
    val PAINT_INNER = 3

    /**
     * sets the given paint object to be used instead of the original/default
     * one
     *
     * @param which, e.g. CircleDisplay.PAINT_TEXT to set a new text paint
     * @param p
     */
    fun setPaint(which: Int, p: Paint?) {
        when (which) {
            PAINT_ARC -> mArcPaint = p
            PAINT_INNER -> mInnerCirclePaint = p
            PAINT_TEXT -> mTextPaint = p
        }
    }

    /**
     * Sets the stepsize (minimum selection interval) of the circle display,
     * default 1f. It is recommended to make this value not higher than 1/5 of
     * the maximum selectable value, and not lower than 1/200 of the maximum
     * selectable value. For a maximum value of 100 for example, a stepsize
     * between 0.5 and 20 is recommended.
     *
     * @param stepsize
     */
    fun setStepSize(stepsize: Float) {
        mStepSize = stepsize
    }

    /**
     * returns the current stepsize of the display, default 1f
     *
     * @return
     */
    fun getStepSize(): Float {
        return mStepSize
    }

    /**
     * returns the center point of the view in pixels
     *
     * @return
     */
    fun getCenter(): PointF {
        return PointF((width / 2).toFloat(), (height / 2).toFloat())
    }

    /**
     * Enable touch gestures on the circle-display. If enabled, selecting values
     * onTouch() is possible. Set a SelectionListener to retrieve selected
     * values. Do not forget to set a value before selecting values. By default
     * the maxvalue is 0f and therefore nothing can be selected.
     *
     * @param enabled
     */
    fun setTouchEnabled(enabled: Boolean) {
        mTouchEnabled = enabled
    }

    /**
     * returns true if touch-gestures are enabled, false if not
     *
     * @return
     */
    fun isTouchEnabled(): Boolean {
        return mTouchEnabled
    }

    /**
     * set a selection listener for the circle-display that is called whenever a
     * value is selected onTouch()
     *
     * @param l
     */
    fun setSelectionListener(l: SelectionListener?) {
        mListener = l
    }

    /** listener called when a value has been selected on touch  */
    private var mListener: SelectionListener? = null

    /** gesturedetector for recognizing single-taps  */
    private var mGestureDetector: GestureDetector? = null

    override fun onTouchEvent(e: MotionEvent): Boolean {
        return if (mTouchEnabled) {
            if (mListener == null) Log.w(LOG_TAG,
                "No SelectionListener specified. Use setSelectionListener(...) to set a listener for callbacks when selecting values.")

            // if the detector recognized a gesture, consume it
            if (mGestureDetector!!.onTouchEvent(e)) return true
            val x = e.x
            val y = e.y

            // get the distance from the touch to the center of the view
            val distance = distanceToCenter(x, y)
            val r = getRadius()

            // touch gestures only work when touches are made exactly on the
            // bar/arc
            if (distance >= r - r * mValueWidthPercent / 100f && distance < r) {
                when (e.action) {
                    MotionEvent.ACTION_MOVE -> {
                        updateValue(x, y)
                        invalidate()
                        if (mListener != null) mListener!!.onSelectionUpdate(mValue, mMaxValue)
                    }
                    MotionEvent.ACTION_UP -> if (mListener != null) mListener!!.onValueSelected(
                        mValue,
                        mMaxValue)
                }
            }
            true
        } else super.onTouchEvent(e)
    }

    /**
     * updates the display with the given touch position, takes stepsize into
     * consideration
     *
     * @param x
     * @param y
     */
    private fun updateValue(x: Float, y: Float) {

        // calculate the touch-angle
        val angle = getAngleForPoint(x, y)

        // calculate the new value depending on angle
        var newVal = mMaxValue * angle / 360f

        // if no stepsize
        if (mStepSize == 0f) {
            mValue = newVal
            mAngle = angle
            return
        }
        val remainder = newVal % mStepSize

        // check if the new value is closer to the next, or the previous
        newVal = if (remainder <= mStepSize / 2f) {
            newVal - remainder
        } else {
            newVal - remainder + mStepSize
        }

        // set the new values
        mAngle = getAngleForValue(newVal)
        mValue = newVal
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {

        // get the distance from the touch to the center of the view
        val distance = distanceToCenter(e.x, e.y)
        val r = getRadius()

        // touch gestures only work when touches are made exactly on the
        // bar/arc
        if (distance >= r - r * mValueWidthPercent / 100f && distance < r) {
            updateValue(e.x, e.y)
            invalidate()
            if (mListener != null) mListener!!.onValueSelected(mValue, mMaxValue)
        }
        return true
    }

    /**
     * returns the angle relative to the view center for the given point on the
     * chart in degrees. The angle is always between 0 and 360°, 0° is NORTH
     *
     * @param x
     * @param y
     * @return
     */
    fun getAngleForPoint(x: Float, y: Float): Float {
        val c = getCenter()
        val tx = (x - c.x).toDouble()
        val ty = (y - c.y).toDouble()
        val length = Math.sqrt(tx * tx + ty * ty)
        val r = Math.acos(ty / length)
        var angle = Math.toDegrees(r).toFloat()
        if (x > c.x) angle = 360f - angle
        angle = angle + 180

        // neutralize overflow
        if (angle > 360f) angle = angle - 360f
        return angle
    }

    /**
     * returns the angle representing the given value
     *
     * @param value
     * @return
     */
    fun getAngleForValue(value: Float): Float {
        return value / mMaxValue * 360f
    }

    /**
     * returns the value representing the given angle
     *
     * @param angle
     * @return
     */
    fun getValueForAngle(angle: Float): Float {
        return angle / 360f * mMaxValue
    }

    /**
     * returns the distance of a certain point on the view to the center of the
     * view
     *
     * @param x
     * @param y
     * @return
     */
    fun distanceToCenter(x: Float, y: Float): Float {
        val c = getCenter()
        var dist = 0f
        var xDist = 0f
        var yDist = 0f
        xDist = if (x > c.x) {
            x - c.x
        } else {
            c.x - x
        }
        yDist = if (y > c.y) {
            y - c.y
        } else {
            c.y - y
        }

        // pythagoras
        dist = Math.sqrt(Math.pow(xDist.toDouble(), 2.0) + Math.pow(yDist.toDouble(), 2.0))
            .toFloat()
        return dist
    }

    /**
     * listener for callbacks when selecting values ontouch
     *
     * @author Philipp Jahoda
     */
    interface SelectionListener {
        /**
         * called everytime the user moves the finger on the circle-display
         *
         * @param val
         * @param maxval
         */
        fun onSelectionUpdate(`val`: Float, maxval: Float)

        /**
         * called when the user releases his finger fromt he circle-display
         *
         * @param val
         * @param maxval
         */
        fun onValueSelected(`val`: Float, maxval: Float)
    }

    object Utils {
        /**
         * This method converts dp unit to equivalent pixels, depending on
         * device density.
         *
         * @param dp A value in dp (density independent pixels) unit. Which we
         * need to convert into pixels
         * @return A float value to represent px equivalent to dp depending on
         * device density
         */
        fun convertDpToPixel(r: Resources, dp: Float): Float {
            val metrics = r.displayMetrics
            return dp * (metrics.densityDpi / 160f)
        }
    }

    override fun onDown(e: MotionEvent?): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
        // TODO Auto-generated method stub
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun onShowPress(e: MotionEvent?) {
        // TODO Auto-generated method stub
    }
}