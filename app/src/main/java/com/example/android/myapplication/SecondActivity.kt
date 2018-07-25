package com.example.android.myapplication

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import org.greenrobot.eventbus.EventBus

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
    }

    fun postEvent(v: View) {
        Log.d("TAG", "post event")
        EventBus.getDefault().postSticky(MessageEvent("hello eventbus"))
        finish()
    }

}
