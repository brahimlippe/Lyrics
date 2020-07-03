package com.example.android.lyrics

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.SearchView
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        Log.i("MainActivity", "Add search menu to action bar")
        // Associate searchable configuration with the SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        if (menu != null) {
            Log.i("MainActivity", "Setting searchable info")
            (menu.findItem(R.id.search).actionView as SearchView).apply {
                setSearchableInfo(searchManager.getSearchableInfo(componentName))
            }
        }
        return super.onCreateOptionsMenu(menu)
    }
}