package com.example.uiexplorations.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.example.uiexplorations.R
import com.example.uiexplorations.dto.Arc
import com.example.uiexplorations.dto.Percent
import com.example.uiexplorations.util.AndroidUtils
import kotlin.math.min
import kotlin.random.Random

class StatsViewSimplified @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attributeSet, defStyleAttr, defStyleRes) {
    private var radius = 0F
    private var center = PointF()
    private var oval = RectF()
    private var lineWidth = AndroidUtils.dp(context = context, dp = 12)
    private val randomColor = { Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt()) }
    private var colors = emptyList<Int>()
    private val emptyColor = 0xECECECEC.toInt()
    private var arcPaint: Paint
    private var arcs = emptyList<Arc>()
    private var yAxisTextCorrection = 0F
    private var textSize = AndroidUtils.dp(context = context, dp = 20)
    private var textColor = 0xFF000000.toInt()
    private val textPaint: Paint
    var percent: Percent = Percent(0)
        set(value) {
            val setValue = when {
                value.percent < 0 -> Percent(0)
                value.percent > 100 -> Percent(100)
                else -> value
            }
            field = setValue
            computeArcs()
        }
    private val sum: Float
        get() = percent.percent.toFloat()
    private val totalPercent: String
        get() = "%.2f%%".format(sum)

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StatsView) {
            textColor = getColor(R.styleable.StatsView_textColor, textColor)
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth)
            colors = listOf(
                getColor(R.styleable.StatsView_firstColor, randomColor()),
                getColor(R.styleable.StatsView_secondColor, randomColor()),
                getColor(R.styleable.StatsView_thirdColor, randomColor()),
                getColor(R.styleable.StatsView_fourthColor, randomColor())
            )
        }
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                textSize = this@StatsViewSimplified.textSize
                color = this@StatsViewSimplified.textColor
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
            }
        arcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                strokeWidth = lineWidth
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
        percent = Percent(79)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        center = PointF(w / 2F, h / 2F)
        yAxisTextCorrection = center.y + textPaint.textSize / 4
        radius = (min(w, h) - lineWidth) / 2F
        // Установка пропорциональной зависимости размера текста от размера
        // custom view
        textPaint.textSize = 2 * radius / 6
        oval = RectF(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius
        )
    }

    private fun computeArcs() {
        arcs = emptyList()
        val data = when {
            percent.percent != 0 -> percent.data()
            else -> return
        }
        var startAngle = -90F
        val sector = 360F / data.size
        data.forEachIndexed { index, datum ->
            val sweepAngle = sector * datum / data.max()
            val color = colors.getOrElse(index) { randomColor() }
            arcs = arcs.plus(Arc(
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                color = color
            ))
            startAngle += sector
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        drawBackgroundCircle(canvas)
        arcs.apply {
            if (isNotEmpty()) {
                forEach { drawArcs(canvas = canvas, arc = it) }
                drawStartPoint(canvas)
            }
        }
        drawText(canvas)
    }

    private fun drawBackgroundCircle(canvas: Canvas) {
        canvas.drawCircle(
            center.x,
            center.y,
            radius,
            arcPaint.apply {
                color = emptyColor
            }
        )
    }

    private fun drawArcs(canvas: Canvas, arc: Arc) {
        canvas.drawArc(
            oval,
            arc.startAngle,
            arc.sweepAngle,
            false,
            arcPaint.apply {
                color = arc.color
            }
        )
    }

    private fun drawStartPoint(canvas: Canvas) {
        canvas.drawArc(
            oval,
            arcs.first().startAngle,
            0.1F,
            false,
            arcPaint.apply {
                color = arcs.first().color
            }
        )
    }

    private fun drawText(canvas: Canvas) {
        canvas.drawText(
            totalPercent,
            center.x,
            yAxisTextCorrection,
            textPaint
        )
    }
}