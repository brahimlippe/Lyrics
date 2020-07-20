package tn.malouf.lyrics

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class RecyclerViewAdapter(
    private val viewModel: LyricsViewModel
) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.one_result, parent, false) as TextView
        return ViewHolder(textView)
    }

    override fun getItemCount(): Int = viewModel.listOfSongs.value?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (viewModel.listOfSongs.value == null) {
            // This case seems impossible
            Log.e("RecyclerViewAdapter", "No songs found")
            return
        }
        holder.textView.text = viewModel.listOfSongs.value!![position].title
        holder.textView.setOnClickListener {
            viewModel.lyrics.value = viewModel.listOfSongs.value!![position].lyrics
        }
    }
}