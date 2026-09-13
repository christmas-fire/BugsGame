package com.example.bugsgame

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.SeekBar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editTextFullName = findViewById<EditText>(R.id.editTextFullName)
        val radioGroupGender = findViewById<RadioGroup>(R.id.radioGroupGender)
        val spinnerCourse = findViewById<Spinner>(R.id.spinnerCourse)
        val seekBarDifficulty = findViewById<SeekBar>(R.id.seekBarDifficulty)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)

        buttonRegister.setOnClickListener {
            val fullName = editTextFullName.text.toString().trim()

            if (fullName.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите ФИО", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedGenderId = radioGroupGender.checkedRadioButtonId
            val gender = if (selectedGenderId == R.id.radioMale) "Мужской" else "Женский"

            val course = spinnerCourse.selectedItem.toString()

            val difficulty = seekBarDifficulty.progress + 1
            val resultMessage = "Игрок зарегистрирован:\nФИО: $fullName\nПол: $gender\nКурс: $course\nСложность: $difficulty"

            Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show()
        }
    }
}
