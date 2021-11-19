package com.bzf.cameraxdemo.analyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.util.ArrayDeque


typealias LumaListener = (luma: Double) -> Unit

/*
* @describe: 图像亮度分析器 (通过查看YUV框架的Y平面来计算图像的平均亮度)
* @author: bzf
* @date: 2021/11/13
*/
class LuminosityAnalyzer(listener: LumaListener? = null): ImageAnalysis.Analyzer {

    private val frameRateWindow = 8
    //帧的时间戳队列
    private val frameTimestamps = ArrayDeque<Long>(5)
    private var listeners = ArrayList<LumaListener>().apply {
        listener?.let { add(it) }
    }
    private var lastAnalyzedTimestamp = 0L
    var framesPerSecond: Double = -1.0
        private set

    //对于来自相机的每个图像调用一次此方法，并以相机的帧速率调用。每个分析调用都是按顺序执行的。
    override fun analyze(image: ImageProxy) {
        if(listeners.isEmpty()){
            image.close()
            return
        }

//        // Keep track of frames analyzed
//        val currentTime = System.currentTimeMillis()
//        frameTimestamps.push(currentTime)
//
//        // Compute the FPS using a moving average
//        while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
//        val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
//        val timestampLast = frameTimestamps.peekLast() ?: currentTime
//        framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
//                frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0
//
//        // Analysis could take an arbitrarily long amount of time
//        // Since we are running in a different thread, it won't stall other use cases
//
//        lastAnalyzedTimestamp = frameTimestamps.first

        /*因为摄像头获得的图像是YUV格式，Y表示亮度，image.planes[0]获得Y的数据， image.planes[1]取得U的数据， image.planes[2]取得V的数据*/
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map {
            //and是位运算符& 按位与。当两位同时为 1 时才返回 1
            //将数据转换为0-255范围内的像素值数组
            it.toInt() and 0xFF
        }
        val luma = pixels.average()
        listeners.forEach { it(luma) }
        image.close()
    }


    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    /**
     * Used to add listeners that will be called with each luma computed
     */
    fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

}