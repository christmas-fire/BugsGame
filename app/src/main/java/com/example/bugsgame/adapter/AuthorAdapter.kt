package com.example.bugsgame.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.bugsgame.R
import com.example.bugsgame.model.Author

class AuthorAdapter(context: Context, authors: List<Author>) : ArrayAdapter<Author>(context, R.layout.item_author, authors) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_author, parent, false)
        val author = getItem(position)
        view.findViewById<ImageView>(R.id.imageViewAuthorPhoto).setImageResource(author?.photoResId ?: 0)
        view.findViewById<TextView>(R.id.textViewAuthorName).text = author?.name
        return view
    }
}