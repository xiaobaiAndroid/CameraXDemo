package com.bzf.cameraxdemo

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig

class MyApplication: Application(), CameraXConfig.Provider {

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                /*设置后置摄像头可用，避免不需要的摄像头初始化，优化相机启动时间*/
//            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
                //只打印错误日志
            .setMinimumLoggingLevel(Log.ERROR).build()
    }
}