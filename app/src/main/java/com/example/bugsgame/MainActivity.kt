package com.example.bugsgame

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.example.bugsgame.adapter.AuthorAdapter
import com.example.bugsgame.model.Author
import com.example.bugsgame.util.ZodiacHelper
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private var selectedDay = 1
    private var selectedMonth = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupTabs()
        setupContent()
        setupListeners()
    }

    private fun setupTabs() {
        val tabHost = findViewById<TabHost>(android.R.id.tabhost)
        tabHost.setup()
        val tabs = listOf("reg" to R.id.tabRegistration, "rules" to R.id.tabRules, "authors" to R.id.tabAuthors, "settings" to R.id.tabSettings)
        val titles = listOf("Регистрация", "Правила", "Авторы", "Настройки")

        tabs.forEachIndexed { i, tab ->
            val spec = tabHost.newTabSpec(tab.first).setIndicator(titles[i]).setContent(tab.second)
            tabHost.addTab(spec)
        }
    }

    private fun setupContent() {
        findViewById<TextView>(R.id.textViewRules).text = HtmlCompat.fromHtml(getString(R.string.game_rules), HtmlCompat.FROM_HTML_MODE_LEGACY)
        findViewById<ListView>(R.id.listViewAuthors).adapter = AuthorAdapter(this, listOf(
            Author("Бабешко А.В. ИП-314", R.drawable.cat1),
            Author("Брунилин С.Д. ИП-314", R.drawable.cat2)
        ))
    }

    private fun setupListeners() {
        findViewById<CalendarView>(R.id.calendarView).setOnDateChangeListener { _, _, month, dayOfMonth ->
            selectedDay = dayOfMonth
            selectedMonth = month + 1
        }

        val radioGroupGender = findViewById<RadioGroup>(R.id.radioGroupGender)
        val spinnerCourse = findViewById<Spinner>(R.id.spinnerCourse)
        val seekBarDifficulty = findViewById<SeekBar>(R.id.seekBarDifficulty)
        val textViewResult = findViewById<TextView>(R.id.textViewResult)

        findViewById<Button>(R.id.buttonRegister).setOnClickListener {
            val name = findViewById<EditText>(R.id.editTextFullName).text.toString()
            if (name.isEmpty()) return@setOnClickListener

            val zodiac = ZodiacHelper.getZodiac(selectedMonth, selectedDay)
            findViewById<ImageView>(R.id.imageViewZodiac).setImageResource(ZodiacHelper.getZodiacImage(selectedMonth, selectedDay))
            val selectedGenderId = radioGroupGender.checkedRadioButtonId
            val gender = if (selectedGenderId == R.id.radioMale) "Мужской" else "Женский"

            val course = spinnerCourse.selectedItem.toString()
            val difficulty = seekBarDifficulty.progress + 1


            val resultMessage = "Игрок: $name\nПол: $gender\nКурс: $course\nСложность: $difficulty\nЗнак: $zodiac"

            textViewResult.text = resultMessage

            Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
        }
    }
}