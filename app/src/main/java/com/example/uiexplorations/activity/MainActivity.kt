package com.example.uiexplorations.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.example.uiexplorations.R
import com.example.uiexplorations.dto.Percent
import com.example.uiexplorations.ui.StatsView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val letsFirstButton = findViewById<MaterialButton>(R.id.firstActionButton)
        val letsSecondButton = findViewById<MaterialButton>(R.id.secondActionButton)
        val firstStatsView = findViewById<StatsView>(R.id.firstRadialFiller)
        val secondStatsView = findViewById<StatsView>(R.id.secondRadialFiller)
        firstStatsView.percent = Percent(79)
        letsFirstButton.setOnClickListener {
            // Для доступа к анимации из кода существует метод startAnimation()
            firstStatsView.startAnimation(
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
        letsSecondButton.setOnClickListener {
            secondStatsView.listData = listOf(
                200F,
                300F,
                400F,
                500F
            )
        }
    }

    private fun showToast(stringResId: Int) {
        Toast.makeText(
            this,
            getString(stringResId),
            Toast.LENGTH_SHORT
        ).show()
    }
}