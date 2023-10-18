package com.example.uiexplorations.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.uiexplorations.R
import com.example.uiexplorations.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val data = listOf(
        200F,
        300F,
        400F,
        500F
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initViews()
        subscribe()
        setupListeners()
    }

    private fun initViews() {
        binding.apply {
            fillingSequence.text = getString(
                if (fillingSequence.isChecked)
                    R.string.concurrent_filling
                else
                    R.string.sequential_filling
            )
            fillingDirection.text = getString(
                if (fillingDirection.isChecked)
                    R.string.unidirectional_filling
                else
                    R.string.bidirectional_filling
            )
        }
    }

    private fun subscribe() {
        binding.apply {
            radialFiller.isOnDraw.observe(this@MainActivity) {
                fillingSequence.isClickable = !it
                fillingDirection.isClickable = !it
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            fillingSequence.setOnClickListener {
                initViews()
                radialFiller.setFillingType(fillingSequence.isChecked)
            }
            fillingDirection.setOnClickListener {
                initViews()
                radialFiller.setDirectionType(fillingDirection.isChecked)
            }
            checkboxRotation.setOnCheckedChangeListener { _, isChecked ->
                radialFiller.setShouldRotate(isChecked)
            }
            actionButton.setOnClickListener {
                radialFiller.listData = data
            }
        }
    }
}