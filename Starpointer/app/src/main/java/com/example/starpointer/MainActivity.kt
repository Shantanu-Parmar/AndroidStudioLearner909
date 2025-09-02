package com.example.starpointer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starpointer.ui.theme.StarpointerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StarpointerTheme {
                MainSelectionScreen { selectedObject ->
                    val intent = Intent(this, CameraActivity::class.java)
                    intent.putExtra("SELECTED_OBJECT", selectedObject)
                    startActivity(intent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSelectionScreen(onStartCapture: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedObject by remember { mutableStateOf("Pleiades") }
    val objects = listOf("Pleiades", "Jupiter", "Betelgeuse", "Aldebaran", "Zeta Tauri", "Elnath", "Hassaleh", "Bellatrix")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Object to Track",
            fontSize = 24.sp,
            color = androidx.compose.ui.graphics.Color.White
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.padding(16.dp)
        ) {
            TextField(
                readOnly = true,
                value = selectedObject,
                onValueChange = { },
                label = { Text("Object") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                objects.forEach { obj ->
                    DropdownMenuItem(
                        text = { Text(obj) },
                        onClick = {
                            selectedObject = obj
                            expanded = false
                        }
                    )
                }
            }
        }
        Button(onClick = { onStartCapture(selectedObject) }) {
            Text("Start Capture")
        }
    }
}