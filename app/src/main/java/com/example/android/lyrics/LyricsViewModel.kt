package com.example.android.lyrics

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LyricsViewModel : ViewModel() {
    val lyrics = MutableLiveData<String>()
    val listOfSongs = MutableLiveData<List<Song>>()
    val searchString = MutableLiveData<String>()

    fun filterSongs(databasePath: String, searchString: String): Boolean {
        Log.i("LyricsViewModel", "Updating song list")
        val result = mutableListOf<Song>()
        try {
            val database = SQLiteDatabase.openDatabase(
                databasePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
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
            database.close()
        } catch (e: Throwable) {
            return false
        }
        listOfSongs.value = result
        return true
    }

}