package com.example.uiexplorations.activity

import android.animation.LayoutTransition
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.view.isNotEmpty
import androidx.transition.AutoTransition
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.example.uiexplorations.R
import com.example.uiexplorations.dto.Percent
import com.example.uiexplorations.ui.StatsView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        partOne()
        partTwo()
        partThree()
    }

    private fun partThree() {
        val groot = findViewById<ViewGroup>(R.id.moreOneRoot)
        findViewById<MaterialButton>(R.id.bounceButton)
            .setOnClickListener {
                groot.layoutTransition = LayoutTransition().apply {
                    if (groot.isNotEmpty()) {
                        setDuration(1500)
                        setInterpolator(LayoutTransition.CHANGE_APPEARING, BounceInterpolator())
                    }
                    else {
                        setDuration(500)
                        setInterpolator(LayoutTransition.CHANGING, LinearInterpolator())
                    }
                }
                val statsView = layoutInflater.inflate(
                    R.layout.stats_view,
                    groot,
                    false
                )
                groot.addView(statsView, 0)
        }
    }

    private fun partTwo() {
        var goBack = true
        findViewById<MaterialButton>(R.id.transitionButton).setOnClickListener {
            TransitionManager.go(
                Scene.getSceneForLayout(
                    findViewById(R.id.transitionRoot),
                    if (goBack)
                        R.layout.end_scene
                    else
                        R.layout.start_scene
                    ,
                    this@MainActivity
                ),
                AutoTransition().apply {
                    duration = 1500
                }
            )
            goBack = !goBack
        }
    }

    private fun partOne() {
        val letsFirstButton = findViewById<MaterialButton>(R.id.firstActionButton)
        val letsSecondButton = findViewById<MaterialButton>(R.id.secondActionButton)
        val firstStatsView = findViewById<StatsView>(R.id.firstRadialFiller)
        val secondStatsView = findViewById<StatsView>(R.id.secondRadialFiller)
        val data = listOf(
            200F,
            300F,
            400F,
            500F
        )
        letsFirstButton.setOnClickListener {
            firstStatsView.listData = data
        }
        letsSecondButton.setOnClickListener {
            secondStatsView.listData = data
        }
    }

    private fun lectureExample(view: StatsView) {
        view.percent = Percent(79)
        // Для доступа к анимации из кода существует метод startAnimation()
        view.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.animation)
                .apply {
                    // Для отслеживания процесса анимации существует метод
                    // setAnimationListener()
                    setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {
                            showToast(R.string.animation_start)
                        }
                        override fun onAnimationEnd(p0: Animation?) {
                            showToast(R.string.animation_stop)
                        }
                        override fun onAnimationRepeat(p0: Animation?) {
                            showToast(R.string.animation_repeat)
                        }
                    })
                }
        )
    }

    private fun showToast(stringResId: Int) {
        Toast.makeText(
            this,
            getString(stringResId),
            Toast.LENGTH_SHORT
        ).show()
    }
}