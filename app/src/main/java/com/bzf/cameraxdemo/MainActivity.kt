package com.bzf.cameraxdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bzf.cameraxdemo.databinding.ActivityMainBinding

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"

class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //视图绑定
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
    }
}