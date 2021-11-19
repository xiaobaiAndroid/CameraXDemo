package com.bzf.cameraxdemo.fragments

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bzf.cameraxdemo.R
import com.bzf.cameraxdemo.dialog.PermissionHintDialog


private val PERMISSIONS_REQUIRED = Manifest.permission.CAMERA

/**
 * A simple [Fragment] subclass.
 * Use the [PermissionsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PermissionsFragment : Fragment(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            hasPermissions(requireContext()) -> {
                navigationToCamera()
            }
            else -> {
                val registerForActivityResult =
                    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                        if (isGranted) {
                            navigationToCamera()
                        } else {
                            Toast.makeText(context, "请求许可被拒绝", Toast.LENGTH_LONG).show()
                        }
                    }
                //如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true。如果用户在过去拒绝了权限请求，并在权限请求系统对话框中选择了 Don't ask again 选项，此方法将返回 false。如果设备规范禁止应用具有该权限，此方法也会返回 false。
                if(shouldShowRequestPermissionRationale(PERMISSIONS_REQUIRED)){
                    val permissionHintDialog =
                        PermissionHintDialog("拍照需要调用摄像头", "调用摄像头需要该权限") { dialog, id ->
                            dialog.dismiss()
                            registerForActivityResult.launch(PERMISSIONS_REQUIRED)
                        }
                    permissionHintDialog.show(childFragmentManager, "permissionHint")
                }else{
                    //请求权限
                    registerForActivityResult.launch(PERMISSIONS_REQUIRED)
                }
            }
        }

    }

    private fun navigationToCamera() {
        lifecycleScope.launchWhenStarted {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment())
        }
    }



    companion object {

        /*权限数组里所有权限都已经被允许了，就返回true*/
        @JvmStatic
        fun hasPermissions(context: Context) = (ContextCompat.checkSelfPermission(
            context,
            PERMISSIONS_REQUIRED
        ) == PackageManager.PERMISSION_GRANTED)
    }
}