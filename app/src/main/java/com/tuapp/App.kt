package com.tuapp

import android.app.Application
import com.tuapp.data.*
import com.tuapp.data.local.AppDatabase

class App : Application() {
    lateinit var sessionStore: SessionStore; private set
    lateinit var ws: ChatWebSocket; private set
    lateinit var db: AppDatabase; private set
    lateinit var repo: ChatRepository; private set
    lateinit var uploader: UploadRepository; private set
    lateinit var temaPrefs: TemaPrefs; private set
    lateinit var monitor: NetworkMonitor; private set
    lateinit var cola: ColaOfflineRepository; private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        ws = ChatWebSocket()
        db = AppDatabase.get(this)
        repo = ChatRepository(db)
        uploader = UploadRepository()
        temaPrefs = TemaPrefs(this)
        monitor = NetworkMonitor(this)
        cola = ColaOfflineRepository(db.pendientes())
    }
}
