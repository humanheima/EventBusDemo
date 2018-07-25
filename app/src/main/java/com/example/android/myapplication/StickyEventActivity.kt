package com.example.android.myapplication

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class StickyEventActivity : AppCompatActivity() {

    val TAG: String = "StickyEventActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticky_event)
        EventBus.getDefault().register(this)
    }

    @Subscribe(sticky = true)
    fun onStickEvent(message: MessageEvent) {
        val stickyEvent = EventBus.getDefault().removeStickyEvent(MessageEvent::class.java)
        // Better check that an event was actually posted before
        if (stickyEvent != null) {
            Toast.makeText(this, "stick:" + message.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}
