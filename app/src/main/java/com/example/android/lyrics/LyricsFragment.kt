package com.example.android.lyrics

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.android.lyrics.databinding.LyricsFragmentBinding
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import kotlin.concurrent.thread

class LyricsFragment : Fragment() {
    private lateinit var binding: LyricsFragmentBinding
    private lateinit var databasePath: String

    companion object {
        fun newInstance() = LyricsFragment()
    }

    private lateinit var viewModel: LyricsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i("LyricsFragment", "onCreateView Called")
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.lyrics_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i("LyricsFragment", "onActivityCreated Called")
        super.onActivityCreated(savedInstanceState)
        databasePath =
            "${activity!!.filesDir.absolutePath}${File.separatorChar}${getString(R.string.remote_database_name)}"
        viewModel = ViewModelProvider(this).get(LyricsViewModel::class.java)
        binding =
            DataBindingUtil.setContentView(activity as Activity, R.layout.lyrics_fragment)
        binding.lyricsViewModel = viewModel
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.lifecycleOwner = this
        viewModel.lyrics.observe(viewLifecycleOwner, Observer {
            binding.lyricsTextView.setBackgroundColor(Color.parseColor("#f5f5f5"))
        })
        updateListOfSongs()
    }

    private fun updateListOfSongs() {
        binding.resultList.apply {
            setHasFixedSize(true)
            adapter = RecyclerViewAdapter(viewModel) {
                Log.i("LyricsFragment", "Hiding keyboard")
                val view: View = activity!!.currentFocus ?: View(activity!!.applicationContext)
                val imm =
                    activity!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.i("LyricsFragment", "onCreateOptionsMenu Called")
        if (activity == null) {
            Log.e("LyricsFragment", "onCreateOptionsMenu called after onActivity created")
            return super.onCreateOptionsMenu(menu, inflater)
        }
        inflater.inflate(R.menu.options_menu, menu)
        Log.i("LyricsFragment", "Add search menu to action bar")
        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    this@LyricsFragment.handleSearch(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && newText.compareTo("") != 0) {
                    this@LyricsFragment.handleSearch(newText)
                }
                return true
            }
        })
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))
        searchView.visibility = View.GONE
        thread {
            downloadDatabase(menu.findItem(R.id.search).actionView as SearchView)
        }
        return super.onCreateOptionsMenu(menu, inflater)
    }


    private fun handleSearch(searchString: String) {
        if (!viewModel.filterSongs(databasePath, searchString)) {
            showError(getString(R.string.database_connection_error))
            return
        }
        updateListOfSongs()
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

    private fun showError(error: String) {
        binding.lyricsTextView.text = error
    }

}