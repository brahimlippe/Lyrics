package com.example.android.lyrics

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.android.lyrics.databinding.LyricsFragmentBinding
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import kotlin.concurrent.thread

class LyricsFragment : Fragment() {
    private lateinit var binding: LyricsFragmentBinding
    private lateinit var database: Database
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
        database =
            Database("${activity!!.filesDir.absolutePath}${File.separatorChar}${getString(R.string.remote_database_name)}")
        viewModel = ViewModelProvider(this).get(LyricsViewModel::class.java)
        binding = DataBindingUtil.setContentView(activity as Activity, R.layout.lyrics_fragment)
        binding.lyricsViewModel = viewModel
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.lifecycleOwner = this
        viewModel.lyrics.observe(viewLifecycleOwner, Observer {
            Log.d(
                "LyricsFragment",
                "Lyrics changed to ${viewModel.lyrics.value?.substring(0, 50)}...etc"
            )
            if (context != null) {
                binding.lyricsScrollView.background =
                    AppCompatResources.getDrawable(context!!, R.drawable.lyrics_box)
            }
        })
        viewModel.listOfSongs.observe(viewLifecycleOwner, Observer {
            updateListOfSongs()
        })
    }

    private fun updateListOfSongs() {
        binding.resultList.apply {
            setHasFixedSize(true)
            adapter = RecyclerViewAdapter(viewModel) {
                Companion.hideKeyboard(activity as Activity)
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
                if (query != null) this@LyricsFragment.handleSearch(query)
                Companion.hideKeyboard(activity as Activity)
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
            if (downloadDatabase() || fallbackDownloadDatabase()) {
                searchView.visibility = View.VISIBLE
            }
        }
        return super.onCreateOptionsMenu(menu, inflater)
    }

    private fun downloadDatabase(): Boolean {
        Log.i("LyricsFragment", "Querying songs")
        val url = URL(getString(R.string.remote_server))
        val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
        urlConnection.useCaches = false
        try {
            val inputStream = BufferedInputStream(urlConnection.inputStream)
            val buffer = ByteArray(1024)
            var nBytesRead: Int
            var total = 0
            val outputStream = ByteArrayOutputStream()
            while (inputStream.read(buffer, 0, 1024).also { nBytesRead = it } > 0) {
                total += nBytesRead
                outputStream.write(buffer)
            }
            Log.i("LyricsFragment", "Downloaded $total bytes from ${url.toString()}")
            val jsonArray = JSONObject(outputStream.toString()).getJSONArray("songs")
            for (i in 0 until jsonArray.length()) {
                val song = jsonArray.get(i) as JSONObject
                database.insertSong(song.getString("title"), song.getString("lyrics"))
            }
        } catch (e: Throwable) {
            Log.e("LyricsFragment", e.toString())
            return false
        }
        return true
    }

    private fun handleSearch(searchString: String) {
        try {
            viewModel.listOfSongs.value = database.filterSongs(searchString)
        } catch (e: Throwable) {
            showError(getString(R.string.database_connection_error))
            return
        }
    }

    private fun fallbackDownloadDatabase(): Boolean {
        Log.i("LyricsFragment", "Downloading fallback database")
        if (database.countSongs() > 0) {
            Log.i("MainActivity", "Database already exists no need to download fallback database")
            return true
        }
        if (!Companion.isInternetAvailable()) {
            Log.e("MainActivity", "No internet connection to download fallback database")
            return false
        }
        val url = URL(getString(R.string.fallback_remote_database))
        val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
        urlConnection.useCaches = false
        val inputStream = BufferedInputStream(urlConnection.inputStream)
        database.fallbackDownload(inputStream)
        Log.i("MainActivity", "Finished download")
        inputStream.close()
        urlConnection.disconnect()
        return true

    }

    private fun showError(error: String) {
        binding.lyricsTextView.text = error
    }

    companion object {
        private fun isInternetAvailable(): Boolean {
            try {
                val address = InetAddress.getByName("www.google.com")
                return !address.equals("")
            } catch (e: UnknownHostException) {
            }
            return false
        }

        private fun hideKeyboard(activity: Activity) {
            Log.i("LyricsFragment", "Hiding keyboard")
            val view: View = activity.currentFocus ?: View(activity.applicationContext)
            val imm =
                activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}