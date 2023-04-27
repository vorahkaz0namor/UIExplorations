package com.example.uiexplorations.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.withStyledAttributes
import com.example.uiexplorations.R
import com.example.uiexplorations.dto.Arc
import com.example.uiexplorations.dto.Percent
import com.example.uiexplorations.util.AndroidUtils
import kotlin.math.min
import kotlin.random.Random

class StatsView @JvmOverloads constructor(
    context: Context,
    // Набор атрибутов, которые можно передавать через xml
    attributeSet: AttributeSet? = null,
    // Стиль атрибута по умолчанию
    defStyleAttr: Int = 0,
    // Стиль по умолчанию (ресурса-?)
    defStyleRes: Int = 0
) : View(
    context,
    attributeSet,
    defStyleAttr,
    defStyleRes
) {
    // Центр окружности (индикатора)
    private var center = PointF()
        set(value) {
            field = value
            circularProgress.center = value
        }
    private var textSize = AndroidUtils.dp(context = context, dp = 20)
    private var textColor = 0xFF000000.toInt()
    private val textPaint: Paint
    private val circularProgress =
        CircularProgress(context, attributeSet, defStyleAttr, defStyleRes)
    private var progress = 0F
    private var animators: List<Animator> = emptyList()
    var listData: List<Float> = emptyList()
        set(value) {
            field = value
            circularProgress.listData = value
            update()
        }
    var percent: Percent = Percent(0)
        set(value) {
            val setValue = when {
                value.percent < 0 -> Percent(0)
                value.percent > 100 -> Percent(100)
                else -> value
            }
            field = setValue
            circularProgress.percent = value
            invalidate()
        }
    private val sum: Float
        get() =
            if (listData.isNotEmpty())
                (progress / listData.size) *
                        listData.map {
                            (it / listData.max()) * 100 / listData.size
                        }
                            .sum()
            else
                percent.percent.toFloat()
    private val totalPercent: String
        get() = "%.2f%%".format(sum)

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StatsView) {
            textSize = getDimension(
                /* index = */ R.styleable.StatsView_textSize,
                /* defValue = */ textSize
            )
            textColor = getColor(R.styleable.StatsView_textColor, textColor)
        }
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG /*Флаг сглаживания*/)
            .apply {
                textSize = this@StatsView.textSize
                color = this@StatsView.textColor
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
            }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        center = PointF(w / 2F, h / 2F)
        circularProgress.fromSizeChanged(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        circularProgress.draw(canvas)
        canvas.drawText(
            /* text = */ totalPercent,
            /* x = */ center.x,
            // Для положения текста по оси Y придется ввести поправочный коэффициент
            /* y = */ center.y + textPaint.textSize / 4,
            /* paint = */ textPaint
        )
    }

    private fun update() {
        animators = animators.plus(
            ObjectAnimator.ofFloat(0F, listData.size.toFloat()).apply {
                addUpdateListener { anim ->
                    (anim.animatedValue as Float).let {
                        progress = it
                        circularProgress.progress = it
                    }
                    invalidate()
                }
            }
        )
            .plus(
                ObjectAnimator.ofFloat(circularProgress, ROTATION, 0F, 360F).apply {
                    addUpdateListener {
                        circularProgress.angleShift = it.animatedValue as Float
                        invalidate()
                    }
                }
            )
        AnimatorSet().apply {
            animators.map {
                addListener(
                    object : DefaultAnimatorListener() {
                        override fun onAnimationCancel(p0: Animator) {
                            it.removeAllListeners()
                            it.cancel()
                            animators = emptyList()
                        }
                    }
                )
                duration = 3000
                interpolator = LinearInterpolator()
                play(it)
            }
        }
            .start()
    }
}

private class CircularProgress @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
): View(context, attributeSet, defStyleAttr, defStyleRes) {
    // Радиус индикатора прогресса
    private var radius = 0F
    // Центр окружности (индикатора)
    var center = PointF()
    private var oval = RectF()
    // Ширина линии
    private var lineWidth = AndroidUtils.dp(context = context, dp = 12)
    private val randomColor = { Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt()) }
    private var colors = emptyList<Int>()
    private val emptyColor = 0xECECECEC.toInt()
    private var arcPaint: Paint
    private var arcs = emptyList<Arc>()
    var progress = 0F
        set(value) {
            field = value
            computeArcs()
        }
    var angleShift = 0F
        set(value) {
            field = value
            computeArcs()
        }
    var listData: List<Float> = emptyList()
        set(value) {
            field = value
            computeArcs()
        }
    var percent: Percent = Percent(0)
        set(value) {
            field = value
            computeArcs()
        }

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StatsView) {
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth)
            colors = listOf(
                getColor(R.styleable.StatsView_firstColor, randomColor()),
                getColor(R.styleable.StatsView_secondColor, randomColor()),
                getColor(R.styleable.StatsView_thirdColor, randomColor()),
                getColor(R.styleable.StatsView_fourthColor, randomColor())
            )
        }
        // Создание кисти
        arcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                // Толщина кисти
                strokeWidth = lineWidth
                // Стиль отрисовки (в данном случае это строки)
                style = Paint.Style.STROKE
                // Скругление краев при отрисовке (для концов линий, а также
                // при их пересечении)
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
    }

    private fun computeArcs() {
        arcs = emptyList()
        val data = when {
            listData.isNotEmpty() -> listData
            percent.percent != 0 -> percent.data()
            else -> return
        }
        // Стартовый угол положения кисти
        var startAngle = -90F + angleShift
        // Сектор, выделяемый для одного элемента
        val sector = 360F / data.size
        data.forEachIndexed { index, datum ->
            // Угол поворота (начертания дуги)
            val sweepAngle = (
                    if (listData.isNotEmpty())
                        when {
                            progress >= index &&
                            progress < 1 + index -> progress - index
                            progress >= 1 + index -> 1F
                            else -> 0F
                        }
                    else
                        1F
                    ) * sector * datum / data.max()
            // Для каждого элемента задается свой цвет.
            // При этом, если отсутствует цвет для элемента
            // из списка data, то цвет сгенерируется
            // по функции randomColor()
            val color = colors.getOrElse(index) { randomColor() }
            arcs = arcs
                .plus(Arc(
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    color = color
                ))
            // Добавим отступ к стартовому углу
            startAngle += sector
        }
        invalidate()
    }

    fun fromSizeChanged(w: Int, h: Int) {
        // Добавим отступ от краев окружности, чтобы она смогла уместиться
        // во время отрисовки
        radius = (min(w, h) - lineWidth) / 2F
        // Чтобы использовать область отрисовки, необходимо создать
        // прямоугольник типа RectF
        oval = RectF(
            /* left = */ center.x - radius,
            /* top = */ center.y - radius,
            /* right = */ center.x + radius,
            /* bottom = */ center.y + radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        drawBackgroundCircle(canvas)
        arcs.apply {
            if (isNotEmpty()) {
                forEach { drawArcs(canvas = canvas, arc = it) }
                drawStartPoint(canvas)
            }
        }
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
            /* oval = */ oval,
            /* startAngle = */ arc.startAngle,
            /* sweepAngle = */ arc.sweepAngle,
            /* useCenter = */ false,
            /* paint = */ arcPaint.apply {
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
}