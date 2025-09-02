package com.example.quotesapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

class QuoteListActivity : AppCompatActivity() {
    private val TAG = "QuoteListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_quote_list)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting content view: ${e.message}", e)
            Toast.makeText(this, "UI error: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting toolbar: ${e.message}", e)
            Toast.makeText(this, "Toolbar error: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        val quotes = intent.getParcelableArrayListExtra<Quote>("QUOTES") ?: arrayListOf()
        try {
            val recyclerView = findViewById<RecyclerView>(R.id.quotesRecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = QuoteAdapter(quotes)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView: ${e.message}", e)
            Toast.makeText(this, "RecyclerView error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class QuoteAdapter(private val quotes: List<Quote>) : RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder>() {
    class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val quoteTextView: TextView = itemView.findViewById(R.id.quoteTextView)
        val authorTextView: TextView = itemView.findViewById(R.id.authorTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quote, parent, false)
        return QuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val quote = quotes[position]
        holder.quoteTextView.text = quote.q
        holder.authorTextView.text = "~ ${quote.a}"
    }

    override fun getItemCount(): Int = quotes.size
}