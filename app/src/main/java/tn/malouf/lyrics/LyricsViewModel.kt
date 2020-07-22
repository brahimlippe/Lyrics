package tn.malouf.lyrics

import android.graphics.drawable.Drawable
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LyricsViewModel : ViewModel() {
    val lyrics = MutableLiveData<String>()
    val listOfSongs = MutableLiveData<List<Song>>()

    private val _resultListVisibility = MutableLiveData<Int>(View.GONE)
    val resultListVisibility: LiveData<Int>
        get() = _resultListVisibility

    private val _lyricsVisibility = MutableLiveData<Int>(View.GONE)
    val lyricsVisibility: LiveData<Int>
        get() = _lyricsVisibility

    private val _lyricsScrollBackground = MutableLiveData<Drawable>()
    val lyricsScrollBackground: LiveData<Drawable>
        get() = _lyricsScrollBackground

    private var _scale = MutableLiveData<Float>(1.0f)
    val scale : LiveData<Float>
    get() = _scale

    fun scaleLyricsText(newScale: Float) {
        _scale.value = newScale * _scale.value!!
    }
    fun showSuggestionsList(background: Drawable?) {
        _resultListVisibility.value = View.VISIBLE
        _lyricsVisibility.value = View.GONE
        _lyricsScrollBackground.value = background
    }

    fun showLyrics(background: Drawable?) {
        _lyricsVisibility.value = View.VISIBLE
        _resultListVisibility.value = View.GONE
        _lyricsScrollBackground.value = background
    }
}