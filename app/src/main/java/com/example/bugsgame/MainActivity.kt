package com.example.bugsgame

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private var selectedDay = 1
    private var selectedMonth = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editTextFullName = findViewById<EditText>(R.id.editTextFullName)
        val radioGroupGender = findViewById<RadioGroup>(R.id.radioGroupGender)
        val spinnerCourse = findViewById<Spinner>(R.id.spinnerCourse)
        val seekBarDifficulty = findViewById<SeekBar>(R.id.seekBarDifficulty)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val textViewResult = findViewById<TextView>(R.id.textViewResult)
        val imageViewZodiac = findViewById<ImageView>(R.id.imageViewZodiac)

        val calendar = Calendar.getInstance()
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectedMonth = calendar.get(Calendar.MONTH) + 1

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDay = dayOfMonth
            selectedMonth = month + 1
        }

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
            val zodiac = getZodiac(selectedMonth, selectedDay)

            val zodiacImageRes = getZodiacImage(selectedMonth, selectedDay)
            imageViewZodiac.setImageResource(zodiacImageRes)

            val resultMessage = "Игрок: $fullName\nПол: $gender\nКурс: $course\nСложность: $difficulty\nЗнак: $zodiac"

            textViewResult.text = resultMessage

            Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
        }
    }

    fun getZodiac(month: Int, day: Int): String {
        return when (month) {
            1 -> if (day < 20) "Козерог" else "Водолей"
            2 -> if (day < 19) "Водолей" else "Рыбы"
            3 -> if (day < 21) "Рыбы" else "Овен"
            4 -> if (day < 20) "Овен" else "Телец"
            5 -> if (day < 21) "Телец" else "Близнецы"
            6 -> if (day < 22) "Близнецы" else "Рак"
            7 -> if (day < 23) "Рак" else "Лев"
            8 -> if (day < 23) "Лев" else "Дева"
            9 -> if (day < 23) "Дева" else "Весы"
            10 -> if (day < 23) "Весы" else "Скорпион"
            11 -> if (day < 22) "Скорпион" else "Стрелец"
            12 -> if (day < 22) "Стрелец" else "Козерог"
            else -> "Неизвестно"
        }
    }

    fun getZodiacImage(month: Int, day: Int): Int {
        val zodiacName = getZodiac(month, day)
        return when (zodiacName) {
            "Козерог" -> R.drawable.zodiac_1
            "Водолей" -> R.drawable.zodiac_2
            "Рыбы" -> R.drawable.zodiac_3
            "Овен" -> R.drawable.zodiac_4
            "Телец" -> R.drawable.zodiac_5
            "Близнецы" -> R.drawable.zodiac_6
            "Рак" -> R.drawable.zodiac_7
            "Лев" -> R.drawable.zodiac_8
            "Дева" -> R.drawable.zodiac_9
            "Весы" -> R.drawable.zodiac_10
            "Скорпион" -> R.drawable.zodiac_11
            "Стрелец" -> R.drawable.zodiac_12
            else -> R.drawable.zodiac_1
        }
    }
}