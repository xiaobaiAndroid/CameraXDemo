package com.bzf.cameraxdemo.utils

import android.widget.ImageButton

//用于执行动画的时间
const val ANIMATION_FAST_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L

/*
* @describe: 模拟一个按钮点击
* @date: 2021/11/19
*/
fun ImageButton.simulateClick(delay: Long = ANIMATION_FAST_MILLIS){
    performClick()
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    },delay)
}