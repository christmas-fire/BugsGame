package com.example.bugsgame

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.bugsgame.adapter.AuthorAdapter
import com.example.bugsgame.adapter.RecordAdapter
import com.example.bugsgame.data.AppDatabase
import com.example.bugsgame.data.GameRepository
import com.example.bugsgame.engine.GameEngine
import com.example.bugsgame.model.Author
import com.example.bugsgame.util.ZodiacHelper
import com.example.bugsgame.view.GameView
import com.example.bugsgame.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private var selectedDay = 1
    private var selectedMonth = 1

    private lateinit var viewModel: GameViewModel
    private lateinit var gameEngine: GameEngine
    private lateinit var repository: GameRepository
    private val handler = Handler(Looper.getMainLooper())
    private var lastTickTime = 0L
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val database = AppDatabase.getInstance(this)
        repository = GameRepository(database)

        viewModel = ViewModelProvider(this).get(GameViewModel::class.java)

        setupTabs()
        setupContent()
        setupListeners()
        setupGame()
        setupSettings()
        setupSwipeGestures()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select_user -> {
                showSelectUserDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupTabs() {
        val tabHost = findViewById<TabHost>(android.R.id.tabhost)
        tabHost.setup()
        val tabs = listOf(
            "reg" to R.id.tabRegistration,
            "rules" to R.id.tabRules,
            "authors" to R.id.tabAuthors,
            "settings" to R.id.tabSettings,
            "game" to R.id.tabGame,
            "records" to R.id.tabRecords
        )
        val titles = listOf("Регистрация", "Правила", "Авторы", "Настройки", "Игра", "Рекорды")

        tabs.forEachIndexed { i, tab ->
            tabHost.addTab(tabHost.newTabSpec(tab.first).setIndicator(titles[i]).setContent(tab.second))
        }

        tabHost.currentTab = viewModel.currentTab
        tabHost.setOnTabChangedListener {
            viewModel.currentTab = tabHost.currentTab
            if (tabHost.currentTab == 5) {
                loadRecords()
            }
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

    private fun updateGameHeader() {
        val textViewScore = findViewById<TextView?>(R.id.textViewScore)
        textViewScore?.text = "Игрок: ${viewModel.currentUserName} | Очки: ${viewModel.score} | Время: ${viewModel.remainingTime}с"
    }

    private fun startRound() {
        val gameView = findViewById<GameView?>(R.id.gameView)
        val buttonStart = findViewById<Button?>(R.id.buttonStart)

        buttonStart?.visibility = View.GONE

        if (gameView != null && gameView.width > 0 && gameView.height > 0) {
            gameEngine.updateSize(gameView.width, gameView.height, 0)
        }

        findViewById<SeekBar?>(R.id.seekBarSpeed)?.let {
            viewModel.gameSpeed = it.progress.toFloat() + 1.0f
        }
        findViewById<EditText?>(R.id.editTextMaxBugs)?.text?.toString()?.toIntOrNull()?.let {
            if (it > 0) viewModel.maxBugs = it
        }
        findViewById<EditText?>(R.id.editTextRoundDuration)?.text?.toString()?.toIntOrNull()?.let {
            if (it > 0) viewModel.roundDuration = it
        }

        viewModel.startNewGame()
        gameEngine.startGame(viewModel)
        lastTickTime = System.currentTimeMillis()
        updateGameHeader()
    }

    private fun setupGame() {
        val gameView = findViewById<GameView?>(R.id.gameView)
        val buttonStart = findViewById<Button?>(R.id.buttonStart)

        gameEngine = GameEngine()
        gameEngine.maxBugs = viewModel.maxBugs

        gameView?.post {
            if (gameView.width > 0 && gameView.height > 0) {
                gameEngine.updateSize(gameView.width, gameView.height, 0)
            }
        }

        updateGameHeader()
        buttonStart?.visibility = if (viewModel.isGameRunning) View.GONE else View.VISIBLE

        buttonStart?.setOnClickListener {
            startRound()
        }

        findViewById<View?>(R.id.layoutHeader)?.setOnClickListener {
            if (!viewModel.isGameRunning) {
                showSelectUserDialog()
            }
        }

        val runnable = object : Runnable {
            override fun run() {
                if (viewModel.isGameRunning && gameView != null) {
                    if (gameView.width > 0 && gameView.height > 0 &&
                        (gameEngine.width != gameView.width || gameEngine.height != gameView.height)
                    ) {
                        gameEngine.updateSize(gameView.width, gameView.height, 0)
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
                        gameView.bugs = ArrayList(gameEngine.bugs)
                        gameView.invalidate()
                        updateGameHeader()
                    }
                }

                handler.postDelayed(this, 30)
            }
        }
        handler.post(runnable)

        gameView?.onBugClick = { x, y ->
            if (viewModel.isGameRunning) {
                val hitBug = gameEngine.checkHit(x, y)
                if (hitBug != null) {
                    viewModel.hitBug(hitBug.points)
                } else {
                    viewModel.missBug()
                }
                updateGameHeader()
                true
            } else false
        }
    }

    private fun onGameOver() {
        viewModel.isGameRunning = false
        gameEngine.bugs.clear()
        val gameView = findViewById<GameView>(R.id.gameView)
        gameView?.bugs = emptyList()
        gameView?.invalidate()

        findViewById<Button?>(R.id.buttonStart)?.visibility = View.VISIBLE
        findViewById<TextView?>(R.id.textViewScore)?.text = "Игра окончена! Очки: ${viewModel.score}"

        lifecycleScope.launch {
            repository.saveGameSession(
                playerName = viewModel.currentUserName,
                playerZodiac = viewModel.currentUserZodiac,
                score = viewModel.score,
                difficulty = viewModel.difficultyName,
                hits = viewModel.hits,
                misses = viewModel.misses,
                durationSeconds = viewModel.roundDuration
            )
        }

        val message = """
            Раунд завершен!
            
            Игрок: ${viewModel.currentUserName}
            Итоговые очки: ${viewModel.score}
            Сложность: ${viewModel.difficultyName}
            Попадания: ${viewModel.hits}
            Промахи: ${viewModel.misses}
            Точность: ${String.format(Locale.getDefault(), "%.1f%%", viewModel.accuracy)}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Время вышло!")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Играть снова") { _, _ ->
                startRound()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun loadRecords() {
        lifecycleScope.launch {
            val records = repository.getTopRecords()
            val listView = findViewById<ListView?>(R.id.listViewRecords)
            val emptyView = findViewById<TextView?>(R.id.textViewEmptyRecords)
            if (records.isEmpty()) {
                emptyView?.visibility = View.VISIBLE
                listView?.visibility = View.GONE
            } else {
                emptyView?.visibility = View.GONE
                listView?.visibility = View.VISIBLE
                listView?.adapter = RecordAdapter(this@MainActivity, records)
            }
        }
    }

    private fun showSelectUserDialog() {
        lifecycleScope.launch {
            val players = repository.getAllPlayers()
            if (players.isEmpty()) {
                Toast.makeText(this@MainActivity, "Нет зарегистрированных игроков", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = players.map { "${it.name} (${it.zodiac})" }.toTypedArray()
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Выберите игрока")
                .setItems(names) { _, which ->
                    val selected = players[which]
                    viewModel.currentUserName = selected.name
                    viewModel.currentUserZodiac = selected.zodiac
                    updateGameHeader()
                    Toast.makeText(this@MainActivity, "Текущий игрок: ${selected.name}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun registerUser(name: String, zodiac: String) {
        lifecycleScope.launch {
            val player = repository.registerOrGetPlayer(name, zodiac)
            viewModel.currentUserName = player.name
            viewModel.currentUserZodiac = player.zodiac
            updateGameHeader()
            Toast.makeText(this@MainActivity, "Игрок '${player.name}' активирован!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        findViewById<CalendarView>(R.id.calendarView).setOnDateChangeListener { _, _, month, dayOfMonth ->
            selectedDay = dayOfMonth
            selectedMonth = month + 1
        }

        findViewById<Button>(R.id.buttonRegister).setOnClickListener {
            val name = findViewById<EditText>(R.id.editTextFullName).text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Введите имя игрока", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val zodiac = ZodiacHelper.getZodiac(selectedMonth, selectedDay)
            findViewById<ImageView>(R.id.imageViewZodiac).setImageResource(
                ZodiacHelper.getZodiacImage(selectedMonth, selectedDay)
            )
            findViewById<TextView>(R.id.textViewResult).text =
                "Игрок: $name\nЗнак: $zodiac\nСчет: ${viewModel.score}"

            registerUser(name, zodiac)
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
                    val tabsCount = 6

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
        val seekBarSpeed = findViewById<SeekBar?>(R.id.seekBarSpeed)
        val editTextMaxBugs = findViewById<EditText?>(R.id.editTextMaxBugs)
        val editTextRoundDuration = findViewById<EditText?>(R.id.editTextRoundDuration)

        seekBarSpeed?.progress = (viewModel.gameSpeed - 1.0f).toInt().coerceAtLeast(0)
        if (editTextMaxBugs?.text.isNullOrEmpty()) {
            editTextMaxBugs?.setText(viewModel.maxBugs.toString())
        }
        if (editTextRoundDuration?.text.isNullOrEmpty()) {
            editTextRoundDuration?.setText(viewModel.roundDuration.toString())
        }

        seekBarSpeed?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.gameSpeed = progress.toFloat() + 1.0f
                    resetGameState()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        editTextMaxBugs?.addTextChangedListener(object : TextWatcher {
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

        editTextRoundDuration?.addTextChangedListener(object : TextWatcher {
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

    private fun resetGameState() {
        viewModel.resetGame()

        findViewById<Button?>(R.id.buttonStart)?.visibility = View.VISIBLE
        updateGameHeader()

        if (::gameEngine.isInitialized) {
            gameEngine.bugs.clear()
            gameEngine.maxBugs = viewModel.maxBugs
        }
        val gameView = findViewById<GameView?>(R.id.gameView)
        gameView?.bugs = emptyList()
        gameView?.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}