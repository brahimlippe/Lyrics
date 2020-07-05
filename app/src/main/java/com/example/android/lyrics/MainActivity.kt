package com.example.android.lyrics

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private lateinit var databasePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("MainActivity", "onCreate called")
        databasePath =
            "${filesDir.absolutePath}${File.separatorChar}${getString(R.string.remote_database_name)}"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        if (toolbar == null) {
            Log.w("MainActivity", "Cannot find toolbar")
        }
        setSupportActionBar(toolbar)
        Log.i("MainActivity", "onCreate call finished")
    }

    private fun isInternetAvailable(): Boolean {
        try {
            val address = InetAddress.getByName("www.google.com")
            return !address.equals("")
        } catch (e: UnknownHostException) {
        }
        return false
    }

    private fun downloadDatabase(searchView: SearchView) {
        val databaseFile = File(databasePath)
        if (databaseFile.exists()) Log.i("MainActivity", "Database already exists")
        val internetAvailable = isInternetAvailable()
        Log.i("MainActivity", "Internet availability: $internetAvailable")
        if (databaseFile.exists() && internetAvailable && !databaseFile.delete()) {
            Log.e("MainActivity", "Cannot delete old database $databaseFile")
            return
        }
        val url = URL(getString(R.string.remote_database))
        val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
        urlConnection.useCaches = false
        try {
            val inputStream = BufferedInputStream(urlConnection.inputStream)
            val buffer = ByteArray(1024)
            val output = FileOutputStream(databaseFile)
            var nBytesRead: Int
            var total = 0
            while (inputStream.read(buffer, 0, 1024).also { nBytesRead = it } > 0) {
                output.write(buffer, 0, nBytesRead)
                total += nBytesRead
            }
            Log.i("MainActivity", "Writing $total bytes in ${databaseFile.absolutePath}")
            output.flush()
            output.close()
            inputStream.close()
            Log.i("MainActivity", "Finished download")
            searchView.visibility = View.VISIBLE
        } catch (e: Throwable) {
            Log.w("MainActivity", e.toString())
        } finally {
            urlConnection.disconnect()
        }
    }

    override fun onNewIntent(intent: Intent) {
        Log.i("MainActivity", "onNewIntent called")
        super.onNewIntent(intent)
        TODO("Not yet implemented")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        Log.i("MainActivity", "Add search menu to action bar")
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        if (menu != null) {
            val searchView = menu.findItem(R.id.search).actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query != null) {
                        this@MainActivity.handleSearch(query)
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText != null && newText.compareTo("") != 0) {
                        this@MainActivity.handleSearch(newText)
                    }
                    return true
                }
            })
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchView.visibility = View.GONE
            thread {
                downloadDatabase(menu.findItem(R.id.search).actionView as SearchView)
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun handleSearch(searchString: String) {
        val lyricsTextView = findViewById<TextView>(R.id.lyricsTextView)
        val list = getListOfSongs(searchString)
        findViewById<RecyclerView>(R.id.result_list).apply {
            setHasFixedSize(true)
            adapter = RecyclerViewAdapter(lyricsTextView, list) {
                Log.i("MainActivity", "Hiding keyboard")
                val view: View = currentFocus ?: View(applicationContext)
                val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    private fun getListOfSongs(searchString: String): List<Song> {
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
            showError(getString(R.string.database_connection_error))
        }
        return result
    }

    private fun showError(error: String) {
        findViewById<TextView>(R.id.lyricsTextView).text = error
    }
}