package com.example.smartcv.utils

import android.content.Context
import android.util.Log
import java.io.File

object DatabaseUtils {
    /**
     * Obtiene la ruta de la base de datos SQLite
     */
    fun getDatabasePath(context: Context): String {
        val dbFile = context.getDatabasePath("smartcv.db")
        Log.d("DatabaseUtils", "Ruta de la base de datos: ${dbFile.absolutePath}")
        return dbFile.absolutePath
    }

    /**
     * Verifica si la base de datos existe
     */
    fun databaseExists(context: Context): Boolean {
        val dbFile = context.getDatabasePath("smartcv.db")
        val exists = dbFile.exists()
        Log.d("DatabaseUtils", "¿La base de datos existe? $exists")
        return exists
    }

    /**
     * Obtiene el tamaño de la base de datos
     */
    fun getDatabaseSize(context: Context): Long {
        val dbFile = context.getDatabasePath("smartcv.db")
        val size = if (dbFile.exists()) dbFile.length() else 0
        Log.d("DatabaseUtils", "Tamaño de la base de datos: $size bytes")
        return size
    }
}
