package com.example.android.lyrics

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class RecyclerViewAdapter(
    private val lyricsTextView: TextView,
    private val data: List<Song> = listOf(),
    private val hideKeyboard : () -> Unit
) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.i("RecyclerViewAdapter", "onCreateViewAdapter called")
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.one_result, parent, false) as TextView
        return ViewHolder(textView)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.i("RecyclerViewAdapter", "onBindViewAdapter called")
        holder.textView.text = data[position].title
        holder.textView.setOnClickListener {
            lyricsTextView.text = data[position].lyrics
            hideKeyboard()
        }
    }
}