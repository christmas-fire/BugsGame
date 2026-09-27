package com.example.bugsgame

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import com.example.bugsgame.adapter.AuthorAdapter
import com.example.bugsgame.engine.GameEngine
import com.example.bugsgame.model.Author
import com.example.bugsgame.util.ZodiacHelper
import com.example.bugsgame.view.GameView
import com.example.bugsgame.viewmodel.GameViewModel

class MainActivity : AppCompatActivity() {
    private var selectedDay = 1
    private var selectedMonth = 1

    private lateinit var viewModel: GameViewModel
    private lateinit var gameEngine: GameEngine
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(GameViewModel::class.java)

        setupTabs()
        setupContent()
        setupListeners()
        setupGame()
    }

    private fun setupTabs() {
        val tabHost = findViewById<TabHost>(android.R.id.tabhost)
        tabHost.setup()
        val tabs = listOf(
            "reg" to R.id.tabRegistration,
            "rules" to R.id.tabRules,
            "authors" to R.id.tabAuthors,
            "settings" to R.id.tabSettings,
            "game" to R.id.tabGame
        )
        val titles = listOf("Регистрация", "Правила", "Авторы", "Настройки", "Игра")

        tabs.forEachIndexed { i, tab ->
            tabHost.addTab(tabHost.newTabSpec(tab.first).setIndicator(titles[i]).setContent(tab.second))
        }

        tabHost.currentTab = viewModel.currentTab
        tabHost.setOnTabChangedListener { tabId ->
            viewModel.currentTab = tabHost.currentTab
        }
    }

    private fun setupContent() {
        findViewById<TextView>(R.id.textViewRules).text = HtmlCompat.fromHtml(getString(R.string.game_rules), HtmlCompat.FROM_HTML_MODE_LEGACY)
        findViewById<ListView>(R.id.listViewAuthors).adapter = AuthorAdapter(this, listOf(
            Author("Бабешко А.В. ИП-314", R.drawable.cat1),
            Author("Брунилин С.Д. ИП-314", R.drawable.cat2)
        ))
    }

    private fun setupGame() {
        val gameView = findViewById<GameView>(R.id.gameView)
        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val textViewScore = findViewById<TextView>(R.id.textViewScore)

        gameEngine = GameEngine(1000, 1000)

        buttonStart.setOnClickListener {
            it.visibility = View.GONE
            viewModel.gameSpeed = findViewById<SeekBar>(R.id.seekBarSpeed).progress.toFloat() + 1.0f
            viewModel.maxBugs = findViewById<EditText>(R.id.editTextMaxBugs).text.toString().toIntOrNull() ?: 10

            gameEngine.startGame(viewModel)
        }

        val runnable = object : Runnable {
            override fun run() {
                if (buttonStart.visibility == View.GONE) {
                    gameEngine.spawnBug()
                    gameEngine.updateBugs(viewModel.gameSpeed)
                    gameView.bugs = gameEngine.bugs
                    gameView.invalidate()
                    textViewScore.text = "Очки: ${viewModel.score}"
                }
                handler.postDelayed(this, 30)
            }
        }
        handler.post(runnable)

        gameView.onBugClick = { x, y ->
            if (buttonStart.visibility == View.GONE) {
                if (gameEngine.checkHit(x, y)) viewModel.hitBug() else viewModel.missBug()
                true
            } else false
        }
    }

    private fun setupListeners() {
        findViewById<CalendarView>(R.id.calendarView).setOnDateChangeListener { _, _, month, dayOfMonth ->
            selectedDay = dayOfMonth
            selectedMonth = month + 1
        }

        findViewById<Button>(R.id.buttonRegister).setOnClickListener {
            val name = findViewById<EditText>(R.id.editTextFullName).text.toString()
            if (name.isEmpty()) return@setOnClickListener

            val zodiac = ZodiacHelper.getZodiac(selectedMonth, selectedDay)
            findViewById<ImageView>(R.id.imageViewZodiac).setImageResource(ZodiacHelper.getZodiacImage(selectedMonth, selectedDay))
            findViewById<TextView>(R.id.textViewResult).text = "Игрок: $name\nЗнак: $zodiac\nСчет: ${viewModel.score}"
            Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
        }
    }
}
