package com.example.portfoliomanager.data.remote

import android.util.Log

object AppLogger {
    fun d(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (e: Throwable) {
            println("[$tag] $msg")
        }
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, msg, throwable)
        } catch (e: Throwable) {
            println("[$tag] $msg ${throwable?.message ?: ""}")
        }
    }
}
