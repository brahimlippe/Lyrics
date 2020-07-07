package com.example.android.lyrics

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LyricsViewModel : ViewModel() {
    val lyrics = MutableLiveData<String>()
    val listOfSongs = MutableLiveData<List<Song>>()
}