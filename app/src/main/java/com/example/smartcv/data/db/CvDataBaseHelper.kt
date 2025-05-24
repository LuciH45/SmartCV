package com.example.smartcv.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CvDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE cvs (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                personal_info TEXT NOT NULL,
                education TEXT NOT NULL,
                experience TEXT NOT NULL,
                abilities TEXT NOT NULL,
                source_images TEXT NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS cvs")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "smartcv.db"
        private const val DATABASE_VERSION = 1
    }
}