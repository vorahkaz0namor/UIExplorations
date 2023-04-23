package com.example.uiexplorations.util

import android.content.Context
import kotlin.math.ceil

object AndroidUtils {
    fun dp(context: Context, dp: Int): Float =
        ceil(context.resources.displayMetrics.density * dp)
}