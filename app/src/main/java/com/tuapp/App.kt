package com.tuapp

import android.app.Application
import com.tuapp.data.ChatWebSocket
import com.tuapp.data.SessionStore

class App : Application() {
    lateinit var sessionStore: SessionStore
        private set
    lateinit var ws: ChatWebSocket
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        ws = ChatWebSocket()
    }
}
