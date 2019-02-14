package net

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.android.myapplication.MessageEvent
import com.example.android.myapplication.R
import com.example.android.myapplication.temp.MainActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SecondActivity : MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
    }

    fun postEvent(v: View) {
        Log.d("TAG", "post event")
        EventBus.getDefault().post(MessageEvent("hello eventbus"))
        //EventBus.getDefault().postSticky(MessageEvent("hello eventbus"))
        finish()
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//     protected fun onMessageEvent(event: MessageEvent) {
//        Log.d("TAG", "receive event")
//        Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
//    }

}
