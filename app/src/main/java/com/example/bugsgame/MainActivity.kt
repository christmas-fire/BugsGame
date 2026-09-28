package com.example.bugsgame

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import com.example.bugsgame.adapter.AuthorAdapter
import com.example.bugsgame.engine.GameEngine
import com.example.bugsgame.model.Author
import com.example.bugsgame.util.ZodiacHelper
import com.example.bugsgame.view.GameView
import com.example.bugsgame.viewmodel.GameViewModel
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private var selectedDay = 1
    private var selectedMonth = 1

    private lateinit var viewModel: GameViewModel
    private lateinit var gameEngine: GameEngine
    private val handler = Handler(Looper.getMainLooper())
    private var lastTickTime = 0L
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(GameViewModel::class.java)

        setupTabs()
        setupContent()
        setupListeners()
        setupGame()
        setupSettings()
        setupSwipeGestures()
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
        tabHost.setOnTabChangedListener {
            viewModel.currentTab = tabHost.currentTab
        }
    }

    private fun setupContent() {
        findViewById<TextView>(R.id.textViewRules).text =
            HtmlCompat.fromHtml(getString(R.string.game_rules), HtmlCompat.FROM_HTML_MODE_LEGACY)
        findViewById<ListView>(R.id.listViewAuthors).adapter = AuthorAdapter(
            this, listOf(
                Author("Бабешко А.В. ИП-314", R.drawable.cat1),
                Author("Брунилин С.Д. ИП-314", R.drawable.cat2)
            )
        )
    }

    private fun setupGame() {
        val gameView = findViewById<GameView>(R.id.gameView)
        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val textViewScore = findViewById<TextView>(R.id.textViewScore)
        val layoutHeader = findViewById<View>(R.id.layoutHeader)

        gameEngine = GameEngine()
        gameEngine.maxBugs = viewModel.maxBugs

        gameView.post {
            val headerHeight = layoutHeader?.height ?: 0
            if (gameView.width > 0 && gameView.height > 0) {
                gameEngine.updateSize(gameView.width, gameView.height, headerHeight)
            }
        }

        textViewScore.text = "Очки: ${viewModel.score} | Время: ${viewModel.remainingTime}с"
        buttonStart.visibility = if (viewModel.isGameRunning) View.GONE else View.VISIBLE

        buttonStart.setOnClickListener {
            it.visibility = View.GONE

            val headerHeight = layoutHeader?.height ?: 0
            if (gameView.width > 0 && gameView.height > 0) {
                gameEngine.updateSize(gameView.width, gameView.height, headerHeight)
            }

            viewModel.gameSpeed = findViewById<SeekBar>(R.id.seekBarSpeed).progress.toFloat() + 1.0f
            viewModel.maxBugs = findViewById<EditText>(R.id.editTextMaxBugs).text.toString().toIntOrNull() ?: 10
            viewModel.roundDuration = findViewById<EditText>(R.id.editTextRoundDuration).text.toString().toIntOrNull() ?: 60

            viewModel.score = 0
            viewModel.remainingTime = viewModel.roundDuration
            viewModel.isGameRunning = true

            gameEngine.startGame(viewModel)
            lastTickTime = System.currentTimeMillis()
            textViewScore.text = "Очки: ${viewModel.score} | Время: ${viewModel.remainingTime}с"
        }

        val runnable = object : Runnable {
            override fun run() {
                if (viewModel.isGameRunning) {
                    val headerHeight = layoutHeader?.height ?: 0

                    if (gameView.width > 0 && gameView.height > 0 &&
                        (gameEngine.width != gameView.width || gameEngine.height != gameView.height || gameEngine.topOffset != headerHeight)
                    ) {
                        gameEngine.updateSize(gameView.width, gameView.height, headerHeight)
                    }

                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastTickTime >= 1000L) {
                        viewModel.remainingTime--
                        lastTickTime = currentTime

                        if (viewModel.remainingTime <= 0) {
                            onGameOver()
                        }
                    }

                    if (viewModel.isGameRunning) {
                        gameEngine.spawnBug()
                        gameEngine.updateBugs(viewModel.gameSpeed)
                        gameView.bugs = gameEngine.bugs
                        gameView.invalidate()
                        textViewScore.text = "Очки: ${viewModel.score} | Время: ${viewModel.remainingTime}с"
                    }
                }

                handler.postDelayed(this, 30)
            }
        }
        handler.post(runnable)

        gameView.onBugClick = { x, y ->
            if (viewModel.isGameRunning) {
                if (gameEngine.checkHit(x, y)) viewModel.hitBug() else viewModel.missBug()
                textViewScore.text = "Очки: ${viewModel.score} | Время: ${viewModel.remainingTime}с"
                true
            } else false
        }
    }

    private fun setupSwipeGestures() {
        val tabHost = findViewById<TabHost>(android.R.id.tabhost)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY) &&
                    abs(diffX) > SWIPE_THRESHOLD &&
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    val tabsCount = 5

                    if (tabHost.currentTab == 4 && viewModel.isGameRunning) {
                        return false
                    }

                    if (diffX > 0) {
                        if (tabHost.currentTab > 0) {
                            tabHost.currentTab -= 1
                            return true
                        }
                    } else {
                        if (tabHost.currentTab < tabsCount - 1) {
                            tabHost.currentTab += 1
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun setupSettings() {
        val seekBarSpeed = findViewById<SeekBar>(R.id.seekBarSpeed)
        val editTextMaxBugs = findViewById<EditText>(R.id.editTextMaxBugs)
        val editTextRoundDuration = findViewById<EditText>(R.id.editTextRoundDuration)

        seekBarSpeed.progress = (viewModel.gameSpeed - 1.0f).toInt().coerceAtLeast(0)
        if (editTextMaxBugs.text.isNullOrEmpty()) {
            editTextMaxBugs.setText(viewModel.maxBugs.toString())
        }
        if (editTextRoundDuration.text.isNullOrEmpty()) {
            editTextRoundDuration.setText(viewModel.roundDuration.toString())
        }

        seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.gameSpeed = progress.toFloat() + 1.0f
                    resetGameState()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        editTextMaxBugs.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editTextMaxBugs.hasFocus()) {
                    val newMax = s?.toString()?.toIntOrNull()
                    if (newMax != null && newMax > 0 && newMax != viewModel.maxBugs) {
                        viewModel.maxBugs = newMax
                        gameEngine.maxBugs = newMax
                        resetGameState()
                    }
                }
            }
        })

        editTextRoundDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editTextRoundDuration.hasFocus()) {
                    val newDuration = s?.toString()?.toIntOrNull()
                    if (newDuration != null && newDuration > 0 && newDuration != viewModel.roundDuration) {
                        viewModel.roundDuration = newDuration
                        resetGameState()
                    }
                }
            }
        })
    }

    private fun onGameOver() {
        viewModel.isGameRunning = false
        gameEngine.bugs.clear()
        val gameView = findViewById<GameView>(R.id.gameView)
        gameView.bugs = emptyList()
        gameView.invalidate()

        findViewById<Button>(R.id.buttonStart).visibility = View.VISIBLE
        findViewById<TextView>(R.id.textViewScore).text = "Игра окончена! Очки: ${viewModel.score}"

        AlertDialog.Builder(this)
            .setTitle("Время вышло!")
            .setMessage("Раунд завершен.\nВаш итоговый счет: ${viewModel.score}")
            .setPositiveButton("ОК", null)
            .show()
    }

    private fun resetGameState() {
        viewModel.score = 0
        viewModel.remainingTime = viewModel.roundDuration
        viewModel.isGameRunning = false

        findViewById<Button>(R.id.buttonStart).visibility = View.VISIBLE
        findViewById<TextView>(R.id.textViewScore).text = "Очки: 0 | Время: ${viewModel.roundDuration}с"

        if (::gameEngine.isInitialized) {
            gameEngine.bugs.clear()
            gameEngine.maxBugs = viewModel.maxBugs
        }
        val gameView = findViewById<GameView>(R.id.gameView)
        gameView?.bugs = emptyList()
        gameView?.invalidate()
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
            findViewById<ImageView>(R.id.imageViewZodiac).setImageResource(
                ZodiacHelper.getZodiacImage(selectedMonth, selectedDay)
            )
            findViewById<TextView>(R.id.textViewResult).text =
                "Игрок: $name\nЗнак: $zodiac\nСчет: ${viewModel.score}"
            Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
