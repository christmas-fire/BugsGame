package com.example.bugsgame

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editTextFullName = findViewById<EditText>(R.id.editTextFullName)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)

        buttonRegister.setOnClickListener {
            val fullName = editTextFullName.text.toString().trim()

            if (fullName.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите ФИО", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultMessage = "Игрок зарегистрирован:\nФИО: $fullName"
            Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show()
        }
    }
}
