package com.bzf.cameraxdemo.utils

import android.annotation.SuppressLint
import android.os.Build
import android.view.DisplayCutout
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat


/** Combination of all flags required to put activity into immersive mode */
const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

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


/*
* @describe: 用设备切口提供的插入填充此视图
* @date: 2021/11/20
*/
@RequiresApi(Build.VERSION_CODES.P)
fun View.padWidthDisplayCutout(){

    fun doPadding(cutout: DisplayCutout) = setPadding(cutout.safeInsetLeft,cutout.safeInsetTop,cutout.safeInsetRight,cutout.safeInsetBottom)

    //测量刘海屏凹槽的Api: DisplayCutout
    rootWindowInsets?.displayCutout?.let {
        doPadding(it)
    }

    setOnApplyWindowInsetsListener { v, insets ->
        insets.displayCutout?.let {
            doPadding(it)
        }
        insets
    }

}

/*
* @describe: 给对话框设置沉浸式模式
* @date: 2021/11/20
*/
@SuppressLint("WrongConstant")
fun AlertDialog.showImmersive() {
    // Set the dialog to not focusable
    window?.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window?.decorView?.let { view ->
            val windowInsetsController =
                ViewCompat.getWindowInsetsController(view)?.let { controller ->

                    controller.hide(WindowInsets.Type.statusBars())
                }
        }
    }


    // Make sure that the dialog's window is in full screen
    window?.decorView?.systemUiVisibility = FLAGS_FULLSCREEN

    // Show the dialog while still in immersive mode
    show()

    // Set the dialog to focusable again
    window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
}