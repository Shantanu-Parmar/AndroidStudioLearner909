package com.example.buygud

import android.media.Image
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
//import coil.compose.rememberImagePainter
import com.example.buygud.ui.theme.BuyGudTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()
        setContent {
            BuyGudTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun fetchData(): MutableState<String> {
    val data = remember { mutableStateOf("Loading...") }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(true) {
        db.collection("buygud").document("things_to_do")
            .get()
            .addOnSuccessListener { document ->
                data.value = document.getString("name") ?: "No data found"
            }
            .addOnFailureListener { exception ->
                data.value = "Error: ${exception.message}"
            }
    }

    return data
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val data = fetchData()

    if (data.value == "Loading...") {
        CircularProgressIndicator(modifier = modifier)
    } else {
        Text(
            text = "Category: ${data.value}",
            modifier = modifier
        )
    }
}

@Composable
fun CategoryList() {
    val db = FirebaseFirestore.getInstance()
    val categories = remember { mutableStateOf<List<String>>(listOf()) }

    LaunchedEffect(true) {
        db.collection("categories").get()
            .addOnSuccessListener { result ->
                val categoryList = result.mapNotNull { it.getString("name") }
                categories.value = categoryList
            }
    }

    LazyColumn {
        items(categories.value) { category ->
            CategoryCard(category)
        }
    }
}

@Composable
fun CategoryCard(category: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Add navigation logic here
            }
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(16.dp),
           // style = MaterialTheme.typography.h6
        )
    }
}

@Composable
fun SubcategoryList(category: String) {
    val db = FirebaseFirestore.getInstance()
    val subcategories = remember { mutableStateOf<List<String>>(listOf()) }

    LaunchedEffect(category) {
        db.collection("categories").document(category)
            .collection("subcategories")
            .get()
            .addOnSuccessListener { result ->
                val subcategoryList = result.mapNotNull { it.getString("name") }
                subcategories.value = subcategoryList
            }
    }

    LazyColumn {
        items(subcategories.value) { subcategory ->
            SubcategoryCard(subcategory)
        }
    }
}

@Composable
fun SubcategoryCard(subcategory: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Add navigation logic here
            }
    ) {
        Text(
            text = subcategory,
            modifier = Modifier.padding(16.dp),
            //style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun ItemList(subcategory: String) {
    val db = FirebaseFirestore.getInstance()
    val items = remember { mutableStateOf<List<Item>>(listOf()) }

    LaunchedEffect(subcategory) {
        db.collection("Subcategories").document("things_to_do")
            .collection(subcategory)
            .get()
            .addOnSuccessListener { result ->
                val itemList = result.mapNotNull { document ->
                    Item(
                        name = document.getString("name") ?: "No Name",
                        description = document.getString("description"),
                        reminderTime = document.getString("reminder_time"),
                        imageUrl = document.getString("image_url")
                    )
                }
                items.value = itemList
            }
    }

    LazyColumn {
        items(items.value) { item ->
            ItemCard(item)
        }
    }
}

data class Item(val name: String, val description: String?, val reminderTime: String?, val imageUrl: String?)

@Composable
fun ItemCard(item: Item) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = item.name)
        Text(text = item.description ?: "No description")
        Text(text = "Reminder: ${item.reminderTime ?: "No Reminder"}")
        item.imageUrl?.let {

           // Image(painter = rememberImagePainter(it), contentDescription = "Item Image")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BuyGudTheme {
        Greeting()
    }
}
