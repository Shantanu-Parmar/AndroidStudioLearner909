
package com.example.quotesapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class Quote(val q: String, val a: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(q)
        parcel.writeString(a)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Quote> {
        override fun createFromParcel(parcel: Parcel): Quote = Quote(parcel)
        override fun newArray(size: Int): Array<Quote?> = arrayOfNulls(size)
    }
}

interface ZenQuotesApi {
    @GET("api/random")
    fun getRandomQuote(): Call<List<Quote>>
}

class MainActivity : AppCompatActivity() {
    private lateinit var quoteTextView: TextView
    private lateinit var authorTextView: TextView
    private lateinit var fetchButton: Button
    private lateinit var toolbar: Toolbar
    private val quoteList = mutableListOf<Quote>()
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting content view: ${e.message}", e)
            Toast.makeText(this, "UI error: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        try {
            quoteTextView = findViewById(R.id.quoteTextView)
            authorTextView = findViewById(R.id.authorTextView)
            fetchButton = findViewById(R.id.fetchButton)
            toolbar = findViewById(R.id.toolbar)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            Toast.makeText(this, "View initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        try {
            setSupportActionBar(toolbar)
            toolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_list) {
                    try {
                        val intent = Intent(this, QuoteListActivity::class.java)
                        intent.putParcelableArrayListExtra("QUOTES", ArrayList(quoteList))
                        startActivity(intent)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting QuoteListActivity: ${e.message}", e)
                        Toast.makeText(this, "Error opening quote list: ${e.message}", Toast.LENGTH_SHORT).show()
                        false
                    }
                } else false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting toolbar: ${e.message}", e)
            Toast.makeText(this, "Toolbar error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        fetchButton.setOnClickListener {
            fetchRandomQuote()
        }

        if (isNetworkAvailable()) {
            fetchRandomQuote()
        } else {
            Log.w(TAG, "No internet connection on launch")
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            quoteTextView.text = "No internet connection"
            authorTextView.text = "~ Please try again"
        }
    }

    private fun fetchRandomQuote() {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No internet connection")
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://zenquotes.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ZenQuotesApi::class.java)
            api.getRandomQuote().enqueue(object : Callback<List<Quote>> {
                override fun onResponse(call: Call<List<Quote>>, response: Response<List<Quote>>) {
                    if (response.isSuccessful) {
                        response.body()?.let { quotes ->
                            if (quotes.isNotEmpty()) {
                                val quote = quotes[0]
                                quoteTextView.text = quote.q
                                authorTextView.text = "~ ${quote.a}"
                                quoteList.add(quote)
                                Toast.makeText(this@MainActivity, "New quote fetched!", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.w(TAG, "Empty quote response")
                                Toast.makeText(this@MainActivity, "No quote received", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Log.w(TAG, "Null response body")
                            Toast.makeText(this@MainActivity, "Empty response", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w(TAG, "Failed to fetch quote: ${response.code()}")
                        Toast.makeText(this@MainActivity, "Failed to fetch quote: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Quote>>, t: Throwable) {
                    Log.e(TAG, "Network error: ${t.message}", t)
                    Toast.makeText(this@MainActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching quote: ${e.message}", e)
            Toast.makeText(this, "Error fetching quote: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.main_menu, menu)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error inflating menu: ${e.message}", e)
            Toast.makeText(this, "Menu error: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}
