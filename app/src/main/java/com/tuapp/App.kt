package com.tuapp

import android.app.Application
import com.tuapp.data.ChatRepository
import com.tuapp.data.ChatWebSocket
import com.tuapp.data.SessionStore
import com.tuapp.data.TemaPrefs
import com.tuapp.data.UploadRepository
import com.tuapp.data.local.AppDatabase

class App : Application() {
    lateinit var sessionStore: SessionStore
        private set
    lateinit var ws: ChatWebSocket
        private set
    lateinit var db: AppDatabase
        private set
    lateinit var repo: ChatRepository
        private set
    lateinit var uploader: UploadRepository
        private set
    lateinit var temaPrefs: TemaPrefs
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        ws = ChatWebSocket()
        db = AppDatabase.get(this)
        repo = ChatRepository(db)
        uploader = UploadRepository()
        temaPrefs = TemaPrefs(this)
    }
}
