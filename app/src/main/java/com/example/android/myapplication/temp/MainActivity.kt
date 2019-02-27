package com.example.android.myapplication.temp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.android.myapplication.MessageEvent
import com.example.android.myapplication.R
import com.example.android.myapplication.StickyEventActivity
import com.example.android.myapplication.TestProtected
import net.SecondActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {


    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("tag", "oncreate")
        EventBus.getDefault().register(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    protected fun onMessageEvent(event: MessageEvent) {
        Log.d("TAG", "receive event")
        Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.tv_start_second_activity -> intent = Intent(this, SecondActivity::class.java)
            R.id.tv_start_third_activity -> intent = Intent(this, StickyEventActivity::class.java)
            R.id.tvGetMethods -> {
                //val method = MainActivity::class.java.getDeclaredMethod("onMessageEvent",MessageEvent::class.java)
                val method = TestProtected::class.java.getDeclaredMethod("onMessageEvent",MessageEvent::class.java)
                Log.e(TAG, method.toString())

            }
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}
