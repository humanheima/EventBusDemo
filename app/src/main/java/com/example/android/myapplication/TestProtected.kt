package com.example.android.myapplication

/**
 * @date: 2019/2/27
 * @author: dumingwei
 * @description:
 */
class TestProtected {

    private val TAG = "TestProtected"

    protected fun onMessageEvent(event: MessageEvent) {
        println(TAG)
    }
}