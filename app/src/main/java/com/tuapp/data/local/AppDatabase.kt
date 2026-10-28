package com.tuapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MsgEntity::class, UsuarioEntity::class, PendienteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mensajes(): MensajeDao
    abstract fun usuarios(): UsuarioDao
    abstract fun pendientes(): PendienteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                ctx.applicationContext, AppDatabase::class.java, "seend.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
