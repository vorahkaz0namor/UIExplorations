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
        setupListeners()
    }

    private fun initViews() {
        binding.apply {
            fillingSequence.text = getString(
                if (fillingSequence.isChecked)
                    R.string.concurrently_filling
                else
                    R.string.sequentially_filling
            )
            fillingDirection.text = getString(
                if (fillingDirection.isChecked)
                    R.string.unidirectional_filling
                else
                    R.string.bidirectional_filling
            )
        }
    }

    private fun setupListeners() {
        binding.apply {
            fillingSequence.setOnClickListener { initViews() }
            fillingDirection.setOnClickListener { initViews() }
            actionButton.setOnClickListener {
                radialFiller.listData = data
            }
        }
    }
}