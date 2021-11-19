package com.bzf.cameraxdemo.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.text.toUpperCase
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.window.layout.WindowMetricsCalculator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bzf.cameraxdemo.EXTENSION_WHITELIST
import com.bzf.cameraxdemo.KEY_EVENT_ACTION
import com.bzf.cameraxdemo.R
import com.bzf.cameraxdemo.analyzer.LuminosityAnalyzer
import com.bzf.cameraxdemo.databinding.CameraUiContainerBinding
import com.bzf.cameraxdemo.databinding.FragmentCameraBinding
import com.bzf.cameraxdemo.utils.AppFileUtils
import com.bzf.cameraxdemo.utils.simulateClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {


    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private var cameraUiContainerBinding: CameraUiContainerBinding? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var displayId: Int = -1

    //使用的摄像头。前置还是后置
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var windowMetricsCalculator: WindowMetricsCalculator
    private var preview: Preview? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager


    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    //使用音量键－进行拍照
    private val volumeDownReceiver = object: BroadcastReceiver(){

        override fun onReceive(context: Context?, intent: Intent) {
            when(intent.getIntExtra(KEY_EVENT_ACTION, KeyEvent.KEYCODE_UNKNOWN)){
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    cameraUiContainerBinding?.cameraCaptureButton?.simulateClick()
                }
            }
        }
    }

    private val displayListener = object: DisplayManager.DisplayListener{
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            Log.d(TAG, "屏幕旋转：${view.display.rotation}")
            imageCapture?.targetRotation = view.display.rotation
            imageAnalyzer?.targetRotation = view.display.rotation
        }?: Unit
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        windowMetricsCalculator = WindowMetricsCalculator.getOrCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = AppFileUtils.getOutputDirectory(requireContext())

        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        val intentFilter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        broadcastManager.registerReceiver(volumeDownReceiver,intentFilter)

        displayManager.registerDisplayListener(displayListener,null)


        //等待布局初始化后，再初始化拍照预览窗口
        fragmentCameraBinding.viewFinder.post {
            displayId = fragmentCameraBinding.viewFinder.display.displayId

            updateCameraUi()
            setupCamera()
        }

    }


    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(CameraFragmentDirections.actionCameraFragmentToPermissionsFragment())
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        cameraExecutor.shutdown()
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bindCameraUseCases()
        updateCameraSwitchButton()
    }

    private fun updateCameraUi() {

        //删除以前的布局，如果有的话。
        cameraUiContainerBinding?.root.let {
            fragmentCameraBinding.root.removeView(it)
        }

        cameraUiContainerBinding = CameraUiContainerBinding.inflate(
            LayoutInflater.from(requireContext()),
            fragmentCameraBinding.root,
            true
        )


        //加载最新的缩略图(如果有)
        lifecycleScope.launch(Dispatchers.IO) {
            outputDirectory.listFiles { file ->
                //获取指定扩展名的文件
                EXTENSION_WHITELIST.contains(file.extension.uppercase(Locale.ROOT))
            }?.maxOrNull()?.let {
                setGalleryThumbnail(Uri.fromFile(it))
            }
        }

        cameraUiContainerBinding?.cameraCaptureButton?.setOnClickListener {

            imageCapture?.let { imageCapture ->
                val photoFile = AppFileUtils.createFile(
                    outputDirectory,
                    AppFileUtils.FILENAME,
                    AppFileUtils.PHOTO_EXTENSION
                )

                val metadata = ImageCapture.Metadata().apply {
                    //前置摄像头进行拍摄时，使用镜像拍摄，不设置镜像的话，拍出来的和取景画面左右是相反的。
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }
                val outputOptions =
                    ImageCapture.OutputFileOptions.Builder(photoFile).setMetadata(metadata).build()

                imageCapture.takePicture(outputOptions, cameraExecutor, object: ImageCapture.OnImageSavedCallback{
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val saveUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(TAG, "照片拍摄成功：$saveUri")

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){ //判断设备android版本是否大于等于6.0
                            setGalleryThumbnail(saveUri)
                        }

                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N){//判断设备android版本是否小于7.0
                            requireActivity().sendBroadcast(Intent(android.hardware.Camera.ACTION_NEW_PICTURE, saveUri))
                        }

                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(saveUri.toFile().extension)
                        //扫描指定路径和MIME(媒体)类型的文件
                        MediaScannerConnection.scanFile(context, arrayOf(saveUri.toFile().absolutePath), arrayOf(mimeType)){ path, uri ->
                            Log.d(TAG, "拍摄的照片保存的媒体目录： $uri")
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "拍照失败：${exception.message}", exception)
                    }
                })
            }
        }

        cameraUiContainerBinding?.cameraSwitchButton?.let {
            //关闭按钮，知道摄像机设置好
            it.isEnabled = false

            it.setOnClickListener {
                lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing){
                    CameraSelector.LENS_FACING_BACK
                }else{
                    CameraSelector.LENS_FACING_FRONT
                }
                bindCameraUseCases()
            }
        }

        cameraUiContainerBinding?.photoViewButton?.let {

        }
    }

    /*
    * 设置Camera
    * */
    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            //when 也可以用来取代 if-else if链。 如果不提供参数，所有的分支条件都是简单的布尔表达式，而当一个分支的条件为真时则执行该分支
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }
            updateCameraSwitchButton()

            bindCameraUseCases()

        }, ContextCompat.getMainExecutor(requireContext()))
    }


    /*
    * @describe: 设置图库的缩略图
    * @date: 2021/11/19
    */
    private fun setGalleryThumbnail(uri: Uri){
        cameraUiContainerBinding?.photoViewButton?.let { photoViewButton ->
            photoViewButton.post {
                photoViewButton.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

                Glide.with(photoViewButton)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(photoViewButton)
            }
        }
    }

    /*
    * @describe:选择相机并绑定生命周期和用例
    * @date: 2021/11/12
    */
    private fun bindCameraUseCases() {
        //获取屏幕分辨率，用于设置全屏分辨率的摄像头
        val metrics = windowMetricsCalculator.computeCurrentWindowMetrics(requireActivity()).bounds

        Log.d(TAG, "屏幕分辨率：${metrics.width()} x ${metrics.height()}")

        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())

        val rotation = fragmentCameraBinding.viewFinder.display.rotation

        val cameraProvider: ProcessCameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed")

        //选择摄像头(前置或者后置)
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()


        //创建图像预览
        imageCapture = ImageCapture.Builder()
            //拍摄速度优先，图片质量可能会差点
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        //创建图像分析
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                    Log.d(TAG, "平均亮度：$luma")
                })
            }

        cameraProvider.unbindAll()
        try {
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (e: Exception) {
            Log.e(TAG, "绑定失败")
        }
    }

    //启用或禁用切换相机按钮
    private fun updateCameraSwitchButton(){
        try {
            cameraUiContainerBinding?.cameraSwitchButton?.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException){
            cameraUiContainerBinding?.cameraSwitchButton?.isEnabled = false
        }
    }




    /*计算最合适的预览比例 4:3, 16:9*/
    private fun aspectRatio(width: Int, height: Int): Int {
        return 4 / 3
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }


    companion object {

        private const val TAG = "CameraXDemo"
    }
}