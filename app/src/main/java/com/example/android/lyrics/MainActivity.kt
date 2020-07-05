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

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("MainActivity", "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("MainActivity", "onCreate call finished")
    }
}