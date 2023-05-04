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
import com.example.uiexplorations.dto.DirectionTypeChooser
import com.example.uiexplorations.dto.FillingTypeChooser
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
) : View(context, attributeSet, defStyleAttr, defStyleRes) {
    // Радиус индикатора прогресса
    private var radius = 0F
    // Центр окружности (индикатора)
    private var center = PointF()
    private var oval = RectF()
    // Ширина линии
    private var lineWidth = AndroidUtils.dp(context = context, dp = 12)
    private val randomColor = { Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt()) }
    private var colors = emptyList<Int>()
    private val emptyColor = 0xECECECEC.toInt()
    private var arcPaint: Paint
    private var arcs = emptyList<Arc>()
    // Для положения текста по оси Y придется ввести поправочный коэффициент
    private var yAxisTextCorrection = 0F
    private var textSize = AndroidUtils.dp(context = context, dp = 20)
    private var textColor = 0xFF000000.toInt()
    private val textPaint: Paint
    private var _fillingType = 0
    private val fillingType =
        mapOf(
            0 to FillingTypeChooser.CONCURRENTLY,
            1 to FillingTypeChooser.SEQUENTIALLY
        )
    var chosenFillingType: FillingTypeChooser
    private var _directionType = 0
    private val directionType =
        mapOf(
            0 to DirectionTypeChooser.UNIDIRECTIONAL,
            1 to DirectionTypeChooser.BIDIRECTIONAL
        )
    var chosenDirectionType: DirectionTypeChooser
    private var progress = 0F
        set(value) {
            field = value
            computeArcs()
        }
    private var angleShift = 0F
        set(value) {
            field = value
            computeArcs()
        }
    private var animators: List<Animator> = emptyList()
    var listData: List<Float> = emptyList()
        set(value) {
            field = value
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
            computeArcs()
        }
    private val sum: Float
        get() =
            if (listData.isNotEmpty())
                (progress / if (chosenFillingType == FillingTypeChooser.SEQUENTIALLY)
                                listData.size
                            else
                                1
                ) * listData.map {
                        (it / listData.max()) * 100 / listData.size
                    }
                            .sum()
            else
                percent.percent.toFloat()
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
            _fillingType = getInteger(R.styleable.StatsView_fillingType, 0)
            _directionType = getInteger(R.styleable.StatsView_directionType, 0)
        }
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG /*Флаг сглаживания*/)
            .apply {
                textSize = this@StatsView.textSize
                color = this@StatsView.textColor
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
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
        chosenFillingType = fillingType.getOrDefault(_fillingType, FillingTypeChooser.CONCURRENTLY)
        chosenDirectionType = directionType.getOrDefault(_directionType, DirectionTypeChooser.UNIDIRECTIONAL)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        center = PointF(w / 2F, h / 2F)
        yAxisTextCorrection = center.y + textPaint.textSize / 4
        // Добавим отступ от краев окружности, чтобы она смогла уместиться
        // во время отрисовки
        radius = (min(w, h) - lineWidth) / 2F
        // Установка пропорциональной зависимости размера текста от размера
        // custom view
        textPaint.textSize = 2 * radius / 6
        // Чтобы использовать область отрисовки, необходимо создать
        // прямоугольник типа RectF
        oval = RectF(
            /* left = */ center.x - radius,
            /* top = */ center.y - radius,
            /* right = */ center.x + radius,
            /* bottom = */ center.y + radius
        )
    }

    private fun computeArcs() {
        arcs = emptyList()
        val data = when {
            listData.isNotEmpty() -> listData
            percent.percent != 0 -> percent.data()
            else -> return
        }
        // Стартовый угол положения кисти
        var startAngle = -90F +
                if (chosenDirectionType == DirectionTypeChooser.UNIDIRECTIONAL)
                    angleShift
                else
                    0F
        // Сектор, выделяемый для одного элемента
        val sector = 360F / data.size
        data.forEachIndexed { index, datum ->
            var staticSweepAngle = sector * datum / data.max()
            if (chosenDirectionType == DirectionTypeChooser.BIDIRECTIONAL) {
                staticSweepAngle /= 2
                startAngle += staticSweepAngle
            }
            // Угол поворота (начертания дуги)
            val sweepAngle = (
                    if (listData.isNotEmpty()) {
                        if (chosenFillingType == FillingTypeChooser.SEQUENTIALLY)
                            when {
                                progress >= index &&
                                        progress < 1 + index -> progress - index
                                progress >= 1 + index -> 1F
                                else -> 0F
                            }
                        else
                            progress
                    }
                    else
                        1F
                    ) * staticSweepAngle
            // Для каждого элемента задается свой цвет.
            // При этом, если отсутствует цвет для элемента
            // из списка data, то цвет сгенерируется
            // по функции randomColor()
            val color = colors.getOrElse(index) { randomColor() }
            arcs = arcs.plus(Arc(
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                color = color
            ))
            if (chosenDirectionType == DirectionTypeChooser.BIDIRECTIONAL)
                arcs = arcs.plus(Arc(
                    startAngle = startAngle,
                    sweepAngle = -sweepAngle,
                    color = color
                ))
            // Добавим отступ к стартовому углу
            startAngle += sector
            if (chosenDirectionType == DirectionTypeChooser.BIDIRECTIONAL)
                startAngle -= staticSweepAngle
        }
        invalidate()
    }

    private fun update() {
        computeArcs()
        animators = animators.plus(
            ObjectAnimator.ofFloat(
                0F,
                ( if (chosenFillingType == FillingTypeChooser.SEQUENTIALLY)
                      listData.size
                  else
                      1
                ).toFloat()
            ).apply {
                addUpdateListener { anim ->
                    progress = anim.animatedValue as Float
                    invalidate()
                }
            }
        )
        // Вращение отключается при двунаправленном заполнении, чтобы было видно,
        // что заполнение происходит именно в двух направлениях, поскольку при
        // вращении создается иллюзия, что заполение однонаправленное
        if (chosenDirectionType == DirectionTypeChooser.UNIDIRECTIONAL)
            animators = animators.plus(
                ObjectAnimator.ofFloat(0F, 360F).apply {
                    addUpdateListener {
                        angleShift = it.animatedValue as Float
                        invalidate()
                    }
                }
            )
        AnimatorSet().apply {
            addListener(
                object : DefaultAnimatorListener() {
                    override fun onAnimationCancel(p0: Animator) {
                        animators.map {
                            it.removeAllListeners()
                            it.cancel()
                        }
                        animators = emptyList()
                    }
                }
            )
            duration =
                if (chosenDirectionType == DirectionTypeChooser.BIDIRECTIONAL &&
                    chosenFillingType == FillingTypeChooser.CONCURRENTLY)
                    2000
                else
                    3000
            interpolator = LinearInterpolator()
            animators.map(::play)
        }
            .start()
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
            arcs.first().let {
                if (chosenDirectionType == DirectionTypeChooser.BIDIRECTIONAL)
                    it.startAngle - it.sweepAngle
                else
                    it.startAngle
            },
            0.1F,
            false,
            arcPaint.apply {
                color = arcs.first().color
            }
        )
    }

    private fun drawText(canvas: Canvas) {
        canvas.drawText(
            /* text = */ totalPercent,
            /* x = */ center.x,
            /* y = */ yAxisTextCorrection,
            /* paint = */ textPaint
        )
    }
}