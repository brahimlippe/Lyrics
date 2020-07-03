package com.example.android.lyrics

import android.app.SearchManager
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class SearchableActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searchable)
        handleSearch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSearch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun handleSearch(intent: Intent) {
        if (intent.action == Intent.ACTION_SEARCH) {
            intent.getStringExtra(SearchManager.QUERY)?.also { searchString ->
                val list = getListOfSongs(searchString)
                val lyricsTextView = findViewById<TextView>(R.id.lyricsTextView)
                findViewById<RecyclerView>(R.id.result_list).apply {
                    setHasFixedSize(true)
                    adapter = RecyclerViewAdapter(lyricsTextView, list)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun getListOfSongs(searchString: String): List<Song> {
        val result = mutableListOf<Song>()
        try {
            val params = SQLiteDatabase.OpenParams.Builder()
            val database = SQLiteDatabase.openDatabase(getDatabaseFile(), params.build())
            val cursor = database.query(
                "Song",
                null,
                "title LIKE ?",
                arrayOf("%$searchString%"),
                null,
                null,
                null
            )
            if (!cursor.moveToFirst()) return result
            while (!cursor.isAfterLast) {
                val titleIndex = cursor.getColumnIndex("title")
                if (titleIndex < 0) {
                    break
                }
                val lyricsIndex = cursor.getColumnIndex("lyrics")
                if (lyricsIndex < 0) {
                    break
                }
                val title = cursor.getString(titleIndex)
                val lyrics = cursor.getString(lyricsIndex)
                val song = Song(title, lyrics)
                result.add(song)
                cursor.moveToNext()
            }
        } finally {
            return result
        }
    }

    private fun getDatabaseFile(): File {
        val fileName = "MySQLiteDB.db"
        val file = getDatabasePath(fileName)
        if (!file.exists()) {
            if (!file.parentFile.exists()) {
                file.parentFile.mkdir()
            }
            val inputStream: InputStream = assets.open("songs.sqlite")
            val outputStream: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(1024 * 8)
            var numOfBytesToRead: Int
            while (inputStream.read(buffer)
                    .also { numOfBytesToRead = it } > 0
            ) outputStream.write(buffer, 0, numOfBytesToRead)
            inputStream.close()
            outputStream.close()
        }
        return file
    }
}