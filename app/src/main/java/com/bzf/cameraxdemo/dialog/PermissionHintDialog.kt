package com.bzf.cameraxdemo.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class PermissionHintDialog(val title: String?= null, val content: String, val listener: DialogInterface.OnClickListener): DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val dialog = builder
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("确认",listener)
                .create()
            return dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}