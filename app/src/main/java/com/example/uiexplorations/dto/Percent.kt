package com.example.uiexplorations.dto

data class Percent(
    val percent: Int
) {
    fun data(): List<Float> =
        buildList {
            var innerPercent = percent
            for (i in 1..4) {
                val item = innerPercent - 25
                if (item >= 0)
                    add(25F)
                else {
                    if (innerPercent >= 0)
                        add(innerPercent.toFloat())
                    else
                        add(0F)
                }
                innerPercent = item
            }
        }
}
