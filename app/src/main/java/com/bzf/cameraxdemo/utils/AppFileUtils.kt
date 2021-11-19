package com.bzf.cameraxdemo.utils

import android.content.Context
import com.bzf.cameraxdemo.R
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/*
* @describe:
* @author: bzf
* @date: 2021/11/19
*/
class AppFileUtils{

    companion object{

         const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
         const val PHOTO_EXTENSION = ".jpg"

        fun getOutputDirectory(context: Context): File{
            val appContext = context.applicationContext
            val mediaDir = appContext.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply {
                    mkdirs()
                }
            }
            return if(mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }

        fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.CANADA).format(System.currentTimeMillis()) + extension)
    }

}