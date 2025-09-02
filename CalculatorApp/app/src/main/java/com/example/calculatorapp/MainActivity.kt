package com.example.calculatorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.Button
import android.widget.TextView
import com.example.calculatorapp.R

class MainActivity : ComponentActivity() {
    private lateinit var displayTextView: TextView
    private var currentInput = ""
    private var firstOperand = 0.0
    private var operator = ""
    private var isNewOperation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        displayTextView = findViewById(R.id.displayTextView)

        // Number buttons
        val numberButtons = listOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3,
            R.id.button4, R.id.button5, R.id.button6, R.id.button7,
            R.id.button8, R.id.button9
        )
        numberButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener { appendNumber((it as Button).text.toString()) }
        }

        // Operator buttons
        findViewById<Button>(R.id.buttonAdd).setOnClickListener { setOperator("+") }
        findViewById<Button>(R.id.buttonSubtract).setOnClickListener { setOperator("-") }
        findViewById<Button>(R.id.buttonMultiply).setOnClickListener { setOperator("*") }
        findViewById<Button>(R.id.buttonDivide).setOnClickListener { setOperator("/") }
        findViewById<Button>(R.id.buttonDecimal).setOnClickListener { appendDecimal() }
        findViewById<Button>(R.id.buttonEquals).setOnClickListener { calculate() }
        findViewById<Button>(R.id.buttonClear).setOnClickListener { clear() }
    }

    private fun appendNumber(number: String) {
        if (isNewOperation) {
            currentInput = ""
            isNewOperation = false
        }
        currentInput += number
        updateDisplay()
    }

    private fun appendDecimal() {
        if (isNewOperation) {
            currentInput = "0"
            isNewOperation = false
        }
        if (!currentInput.contains(".")) {
            currentInput += if (currentInput.isEmpty()) "0." else "."
        }
        updateDisplay()
    }

    private fun setOperator(op: String) {
        if (currentInput.isNotEmpty()) {
            firstOperand = currentInput.toDoubleOrNull() ?: 0.0
            operator = op
            isNewOperation = true
        }
    }

    private fun calculate() {
        if (currentInput.isNotEmpty() && operator.isNotEmpty()) {
            val secondOperand = currentInput.toDoubleOrNull() ?: 0.0
            val result = when (operator) {
                "+" -> firstOperand + secondOperand
                "-" -> firstOperand - secondOperand
                "*" -> firstOperand * secondOperand
                "/" -> if (secondOperand != 0.0) firstOperand / secondOperand else "Error"
                else -> 0.0
            }
            currentInput = if (result is Double) {
                if (result % 1 == 0.0) result.toInt().toString() else result.toString()
            } else {
                result.toString()
            }
            operator = ""
            isNewOperation = true
            updateDisplay()
        }
    }

    private fun clear() {
        currentInput = ""
        firstOperand = 0.0
        operator = ""
        isNewOperation = true
        displayTextView.text = "0"
    }

    private fun updateDisplay() {
        displayTextView.text = currentInput.ifEmpty { "0" }
    }
}