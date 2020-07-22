package tn.malouf.lyrics

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.json.JSONObject
import tn.malouf.lyrics.databinding.LyricsFragmentBinding
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.overflow_menu, menu)
        menu.findItem(R.id.about).setOnMenuItemClickListener {
            val dialogFragment = AboutDialog()
            dialogFragment.showNow(
                requireActivity().supportFragmentManager,
                "About Dialog"
            )
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i("LyricsFragment", "onActivityCreated Called")
        super.onActivityCreated(savedInstanceState)
        val listener =
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector?): Boolean {
                    if (detector == null) return false
                    viewModel.scaleLyricsText(detector.scaleFactor)
                    return true
                }
            }

        val detector = ScaleGestureDetector(context, listener)
        database =
            Database("${activity!!.filesDir.absolutePath}${File.separatorChar}${getString(R.string.remote_database_name)}")

        // Observers
        viewModel = ViewModelProvider(this).get(LyricsViewModel::class.java)
        viewModel.apply {
            scale.observe(viewLifecycleOwner, Observer {
                binding.lyricsTextView.textSize = 16 * it
            })
            lyrics.observe(viewLifecycleOwner, Observer { showLyrics() })
            listOfSongs.observe(
                viewLifecycleOwner,
                Observer { binding.resultList.adapter?.notifyDataSetChanged() })
        }

        // Bindings
        binding = DataBindingUtil.setContentView(activity as Activity, R.layout.lyrics_fragment)
        binding.apply {
            lyricsViewModel = viewModel
            resultList.apply {
                setHasFixedSize(true)
                adapter = RecyclerViewAdapter(viewModel)
            }
            (activity as AppCompatActivity).setSupportActionBar(toolbar)
            lifecycleOwner = this@LyricsFragment
        }
        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        binding.search.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    onQueryTextSubmit()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    this@LyricsFragment.onQueryTextChange(newText)
                    return true
                }
            })
            setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))
            setOnQueryTextFocusChangeListener { _, hasFocus -> if (hasFocus) showSuggestionsList() }
        }
        thread {
            if (downloadDatabase() || fallbackDownloadDatabase()) {
                activity?.runOnUiThread { binding.search.visibility = View.VISIBLE }
            }
        }
        binding.lyricsScrollView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
        }
    }

    private fun showSuggestionsList() {
        TransitionManager.beginDelayedTransition(binding.lyricsScrollView as ViewGroup)
        TransitionManager.beginDelayedTransition(
            binding.resultList as ViewGroup,
            ChangeBounds()
        )
        viewModel.showSuggestionsList(binding.resultList.background)
    }

    private fun onQueryTextSubmit() {
        hideKeyboard(activity as Activity)
    }

    private fun onQueryTextChange(newText: String?) {
        if (newText != null && newText.compareTo("") != 0) {
            this@LyricsFragment.handleSearch(newText)
        }
        showSuggestionsList()
    }

    private fun showLyrics() {
        TransitionManager.beginDelayedTransition(binding.lyricsScrollView as ViewGroup)
        TransitionManager.beginDelayedTransition(binding.resultList as ViewGroup)
        var background: Drawable? = null
        if (context != null) {
            background = AppCompatResources.getDrawable(context!!, R.drawable.lyrics_box)
        }
        viewModel.showLyrics(background)
        binding.search.clearFocus()
    }

    private fun downloadDatabase(): Boolean {
        val url = URL(getString(R.string.remote_server) + database.getGreatestSongID())
        Log.i("LyricsFragment", "Querying songs from $url")
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
            Log.i("LyricsFragment", "Downloaded $total bytes from $url")
            val jsonArray = JSONObject(outputStream.toString()).getJSONArray("songs")
            for (i in 0 until jsonArray.length()) {
                val song = jsonArray.get(i) as JSONObject
                database.insertSong(
                    song.getLong("songID"),
                    song.getString("title"),
                    song.getString("lyrics")
                )
            }
            activity?.runOnUiThread {
                if (jsonArray.length() < 1) return@runOnUiThread
                var text = getString(R.string.saved_songs_1)
                if (jsonArray.length() > 1) text += " " + jsonArray.length().toString()
                text += """ ${getString(R.string.saved_songs_2)}"""
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Throwable) {
            Log.e("LyricsFragment", e.toString())
            return false
        }
        return true
    }

    private fun handleSearch(searchString: String) {
        try {
            val listOfSongs = database.filterSongs(searchString)
            if (viewModel.listOfSongs.value != listOfSongs) {
                viewModel.listOfSongs.value = listOfSongs
            }
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
        if (!isInternetAvailable()) {
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
            val view: View = activity.currentFocus ?: View(activity.applicationContext)
            val imm =
                activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}