package com.example.android.lyrics

import android.content.ContentValues
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class Database(databasePath: String) {
    private var database: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(
        databasePath,
        null,
        DatabaseErrorHandler {
            Log.e("Database", "Error while opening database")
        }
    )

    init {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS Song(" +
                    "  title TEXT PRIMARY KEY NOT NULL," +
                    "  lyrics TEXT NOT NULL)"
        )
    }

    fun insertSong(title: String, lyrics: String) {
        val values = ContentValues()
        values.put("title", title)
        values.put("lyrics", lyrics)
        database.replace("Song", null, values)
    }

    fun fallbackDownload(inputStream: InputStream) {
        database.close()
        val buffer = ByteArray(1024)
        val output = FileOutputStream(File(database.path))
        var nBytesRead: Int
        var total = 0
        while (inputStream.read(buffer, 0, 1024).also { nBytesRead = it } > 0) {
            output.write(buffer, 0, nBytesRead)
            total += nBytesRead
        }
        Log.i("Database", "Writing $total bytes in ${database.path}")
        output.flush()
        output.close()
        database = SQLiteDatabase.openOrCreateDatabase(
            database.path,
            null,
            DatabaseErrorHandler {
                Log.e("Database", "Error while opening database")
            })
    }

    fun filterSongs(searchString: String): List<Song> {
        Log.i("Database", "Updating song list")
        val result = mutableListOf<Song>()
        val cursor = database.query(
            "Song",
            null,
            "title LIKE ?",
            arrayOf("%$searchString%"),
            null,
            null,
            null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val titleIndex = cursor.getColumnIndex("title")
            if (titleIndex < 0) break
            val lyricsIndex = cursor.getColumnIndex("lyrics")
            if (lyricsIndex < 0) break
            val title = cursor.getString(titleIndex)
            val lyrics = cursor.getString(lyricsIndex)
            val song = Song(title, lyrics)
            result.add(song)
            cursor.moveToNext()
        }
        cursor.close()
        return result
    }

    fun countSongs(): Int {
        val cursor = database.query(
            "Song",
            arrayOf("count(*) as c"),
            null,
            null,
            null,
            null,
            null
        )
        cursor.moveToFirst()
        if (!cursor.isAfterLast) {
            val countIndex = cursor.getColumnIndex("c")
            if (countIndex >= 0) {
                val result = cursor.getInt(countIndex)
                Log.i("Database", "$result song(s) in database")
                return result
            }
        }
        cursor.close()
        return -1
    }
}