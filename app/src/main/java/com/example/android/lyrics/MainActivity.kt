package com.example.android.lyrics

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.SearchView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("MainActivity", "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        if (toolbar == null) {
            Log.w("MainActivity", "Cannot find toolbar")
        }
        setSupportActionBar(toolbar)
        handleIntent(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onNewIntent(intent: Intent) {
        Log.i("MainActivity", "onNewIntent called")
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEARCH) {
            val searchString = intent.getStringExtra(SearchManager.QUERY)
            if (searchString != null && searchString.compareTo("") != 0) {
                handleSearch(searchString)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        Log.i("MainActivity", "Add search menu to action bar")
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        if (menu != null) {
            Log.i("MainActivity", "Setting searchable info")
            val searchView = menu.findItem(R.id.search).actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                @RequiresApi(Build.VERSION_CODES.O_MR1)
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query != null) {
                        this@MainActivity.handleSearch(query)
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText != null && newText.compareTo("") != 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            this@MainActivity.handleSearch(newText)
                        }
                    }
                    return true
                }
            })
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        }
        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun handleSearch(searchString: String) {
        val list = getListOfSongs(searchString)
        val lyricsTextView = findViewById<TextView>(R.id.lyricsTextView)
        findViewById<RecyclerView>(R.id.result_list).apply {
            setHasFixedSize(true)
            adapter = RecyclerViewAdapter(lyricsTextView, list)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun getListOfSongs(searchString: String): List<Song> {
        val result = mutableListOf<Song>()
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
        cursor.moveToFirst()
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
        database.close()
        return result
    }

    private fun getDatabaseFile(): File {
        val fileName = "MySQLiteDB.db"
        val file = getDatabasePath(fileName)
        if (!file.exists()) {
            val parentFile = file.parentFile
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdir()
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