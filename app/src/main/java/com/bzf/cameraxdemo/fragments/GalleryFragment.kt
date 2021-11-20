package com.bzf.cameraxdemo.fragments

import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bzf.cameraxdemo.BuildConfig
import com.bzf.cameraxdemo.R
import com.bzf.cameraxdemo.databinding.FragmentGalleryBinding
import com.bzf.cameraxdemo.utils.padWidthDisplayCutout
import com.bzf.cameraxdemo.utils.showImmersive
import java.io.File
import java.util.*


val EXTENSION_WHITELIST = arrayOf("JPG")

class GalleryFragment : Fragment() {


    private val args: GalleryFragmentArgs by navArgs()

    private lateinit var viewModel: GalleryViewModel

    private lateinit var mediaList: MutableList<File>

    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
    private val fragmentGalleryBinding get() = _fragmentGalleryBinding!!


    inner class MediaPagerAdapter(fm: FragmentManager): FragmentStateAdapter(fm,this.lifecycle){
        override fun getItemCount() = mediaList.size
        override fun createFragment(position: Int) =  PhotoFragment.create(mediaList[position])
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding= FragmentGalleryBinding.inflate(inflater, container, false)
        return fragmentGalleryBinding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootDirectory = File(args.rootDirectory)
        mediaList = rootDirectory.listFiles{ file ->
            EXTENSION_WHITELIST.contains(file.extension.uppercase(Locale.ROOT))
        }?.sortedDescending()?.toMutableList() ?: mutableListOf()

    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(mediaList.isEmpty()){
            fragmentGalleryBinding.deleteButton.isEnabled = false
            fragmentGalleryBinding.shareButton.isEnabled = false
        }

        fragmentGalleryBinding.photoViewPager.apply {
            offscreenPageLimit = 2
            adapter = MediaPagerAdapter(childFragmentManager)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }

        //适配刘海屏
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            fragmentGalleryBinding.cutoutSafeArea.padWidthDisplayCutout()
        }

        fragmentGalleryBinding.backButton.setOnClickListener {
            //返回上一个页面
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp()
        }

        fragmentGalleryBinding.shareButton.setOnClickListener {

            mediaList.getOrNull(fragmentGalleryBinding.photoViewPager.currentItem)?.let { mediaFile ->

                //使用Android Sharesheet分享图片
                //https://developer.android.com/training/sharing/send
                val shareIntent = Intent().apply {
                    val mediaType =
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(mediaFile.extension)

                    val uri = FileProvider.getUriForFile(
                        view.context,
                        BuildConfig.APPLICATION_ID + ".provider",
                        mediaFile
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.my_share_photo))
                    type = mediaType
                    action = Intent.ACTION_SEND
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(shareIntent,getString(R.string.share_hint)))
            }
        }

        fragmentGalleryBinding.deleteButton.setOnClickListener {
            mediaList.getOrNull(fragmentGalleryBinding.photoViewPager.currentItem)?.let { mediaFile ->
                AlertDialog.Builder(view.context, android.R.style.Theme_Material_Dialog)
                    .setTitle(getString(R.string.confirm))
                    .setMessage(getString(R.string.delete_curent_photo))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(getString(R.string.yes)){ _, _ ->
                        mediaFile.delete()

                        //通知其他应用，这张照片已经删除
                        MediaScannerConnection.scanFile(view.context, arrayOf(mediaFile.absolutePath), null,null)

                        mediaList.removeAt(fragmentGalleryBinding.photoViewPager.currentItem)
                        fragmentGalleryBinding.photoViewPager.adapter?.notifyItemRemoved(fragmentGalleryBinding.photoViewPager.currentItem)
                        //没有照片就返回拍照页面
                        if(mediaList.isEmpty()){
                            Navigation.findNavController(requireActivity(),R.id.nav_host_fragment).navigateUp()
                        }
                    }
                    .setNegativeButton(getString(R.string.no),null)
                    .create().showImmersive()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)
    }

    override fun onDestroyView() {
        _fragmentGalleryBinding = null
        super.onDestroyView()
    }

}