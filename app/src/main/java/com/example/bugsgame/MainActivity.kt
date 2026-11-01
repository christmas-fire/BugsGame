package com.example.bugsgame

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
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
import androidx.lifecycle.lifecycleScope
import com.example.bugsgame.adapter.AuthorAdapter
import com.example.bugsgame.adapter.RecordAdapter
import com.example.bugsgame.data.PlayerEntity
import com.example.bugsgame.model.Author
import com.example.bugsgame.model.BugType
import com.example.bugsgame.model.SkillType
import com.example.bugsgame.util.ZodiacHelper
import com.example.bugsgame.view.GameView
import com.example.bugsgame.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {
    private var selectedDay = 1
    private var selectedMonth = 1

    private val viewModel: GameViewModel by viewModel()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var screamPlayer: MediaPlayer? = null

    private var filterPlayerId: Long? = null
    private var sortByScore: Boolean = true
    private var playersList: List<PlayerEntity> = emptyList()

    private var gameLoopRunnable: Runnable? = null
    private var availableSkills: List<SkillType> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setupTabs()
        setupContent()
        setupListeners()
        setupGame()
        setupShop()
        setupSettings()
        setupRecordFilters()
        setupSwipeGestures()

        registerUser("Игрок", "Козерог")
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        viewModel.lastTickTime = System.currentTimeMillis()
        updateShopUI()
        updateGameSkillsSpinner()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        stopBugScream()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = -event.values[0]
            val ay = event.values[1]
            viewModel.gameEngine.tiltX = ax
            viewModel.gameEngine.tiltY = ay
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun playBugScream() {
        try {
            stopBugScream()
            val resId = resources.getIdentifier("bug_scream", "raw", packageName)
            if (resId != 0) {
                screamPlayer = MediaPlayer.create(this, resId)
                screamPlayer?.setOnCompletionListener {
                    stopBugScream()
                }
                screamPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopBugScream() {
        try {
            screamPlayer?.stop()
            screamPlayer?.release()
            screamPlayer = null
        } catch (e: Exception) {
            screamPlayer = null
        }
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
            "shop" to R.id.tabShop,
            "records" to R.id.tabRecords
        )
        val titles = listOf("Регистрация", "Правила", "Авторы", "Настройки", "Игра", "Магазин", "Рекорды")

        tabs.forEachIndexed { i, tab ->
            tabHost.addTab(tabHost.newTabSpec(tab.first).setIndicator(titles[i]).setContent(tab.second))
        }

        tabHost.currentTab = viewModel.currentTab
        tabHost.setOnTabChangedListener {
            viewModel.currentTab = tabHost.currentTab
            when (tabHost.currentTab) {
                4 -> updateGameSkillsSpinner()
                5 -> updateShopUI()
                6 -> {
                    updatePlayerFilterSpinner()
                    loadRecords()
                }
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
        var status = ""
        if (viewModel.gameEngine.isTiltModeActive) status += " | ⚡ ГРАВИТАЦИЯ!"
        if (viewModel.gameEngine.isFrozen) status += " | ❄️ ЗАМОРОЗКА!"
        if (viewModel.gameEngine.isSlowed) status += " | ⏳ ЗАМЕДЛЕНИЕ!"
        if (viewModel.gameEngine.isMultitouchActive) status += " | ⚡ МУЛЬТИТАЧ!"

        textViewScore?.text = "Игрок: ${viewModel.currentUserName} (${viewModel.currentPlayerCoins}💰) | Очки: ${viewModel.score} | Время: ${viewModel.remainingTime}с$status"
    }

    private fun startRound() {
        val gameView = findViewById<GameView?>(R.id.gameView)
        val buttonStart = findViewById<Button?>(R.id.buttonStart)

        buttonStart?.visibility = View.GONE

        if (gameView != null && gameView.width > 0 && gameView.height > 0) {
            viewModel.gameEngine.updateSize(gameView.width, gameView.height, 0)
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
        updateGameHeader()
        updateGameSkillsSpinner()
    }

    private fun setupGame() {
        val gameView = findViewById<GameView>(R.id.gameView)
        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val buttonActivateSkill = findViewById<Button>(R.id.buttonActivateSkill)
        val spinnerGameSkills = findViewById<Spinner>(R.id.spinnerGameSkills)

        viewModel.gameEngine.maxBugs = viewModel.maxBugs

        gameView?.post {
            if (gameView.width > 0 && gameView.height > 0) {
                viewModel.gameEngine.updateSize(gameView.width, gameView.height, 0)
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

        buttonActivateSkill.setOnClickListener {
            val skill = viewModel.selectedSkill
            if (skill == null) {
                Toast.makeText(this, "Выберите способность!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val res = viewModel.activateSkill(skill)
                if (res.isSuccess) {
                    Toast.makeText(this@MainActivity, res.getOrNull(), Toast.LENGTH_SHORT).show()
                    updateGameHeader()
                    updateGameSkillsSpinner()
                } else {
                    Toast.makeText(this@MainActivity, res.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        spinnerGameSkills.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (availableSkills.isNotEmpty() && position < availableSkills.size) {
                    viewModel.selectedSkill = availableSkills[position]
                } else {
                    viewModel.selectedSkill = null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        gameLoopRunnable = object : Runnable {
            override fun run() {
                val currentView = findViewById<GameView?>(R.id.gameView)
                if (viewModel.isGameRunning && currentView != null) {
                    if (currentView.width > 0 && currentView.height > 0 &&
                        (viewModel.gameEngine.width != currentView.width || viewModel.gameEngine.height != currentView.height)
                    ) {
                        viewModel.gameEngine.updateSize(currentView.width, currentView.height, 0)
                    }

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - viewModel.lastTickTime >= 1000L) {
                        viewModel.remainingTime--
                        viewModel.lastTickTime = currentTime

                        if (viewModel.remainingTime <= 0) {
                            onGameOver()
                        }
                    }

                    if (viewModel.isGameRunning) {
                        viewModel.gameEngine.spawnBug()
                        viewModel.gameEngine.updateBugs(viewModel.gameSpeed)
                        currentView.bugs = ArrayList(viewModel.gameEngine.bugs)
                        currentView.bonus = viewModel.gameEngine.bonus
                        currentView.isFrozen = viewModel.gameEngine.isFrozen
                        currentView.isSlowed = viewModel.gameEngine.isSlowed
                        currentView.isMultitouchActive = viewModel.gameEngine.isMultitouchActive
                        currentView.invalidate()
                        updateGameHeader()
                    }
                } else if (!viewModel.isGameRunning && currentView != null) {
                    currentView.bugs = ArrayList(viewModel.gameEngine.bugs)
                    currentView.bonus = viewModel.gameEngine.bonus
                    currentView.isFrozen = false
                    currentView.isSlowed = false
                    currentView.isMultitouchActive = false
                    currentView.invalidate()
                }

                handler.postDelayed(this, 30)
            }
        }
        gameLoopRunnable?.let { handler.post(it) }

        gameView?.onBugClick = { x, y ->
            if (viewModel.isGameRunning) {
                if (viewModel.gameEngine.checkBonusHit(x, y)) {
                    playBugScream()
                    Toast.makeText(this@MainActivity, "Бонус взят! Наклоняйте телефон!", Toast.LENGTH_SHORT).show()
                    updateGameHeader()
                    true
                } else {
                    val hitBug = viewModel.gameEngine.checkHit(x, y)
                    if (hitBug != null) {
                        viewModel.hitBug(hitBug.points)
                        if (hitBug.type == BugType.GOLDEN) {
                            Toast.makeText(this@MainActivity, "Пойман Золотой таракан! +${hitBug.points} очков (по курсу)", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.missBug()
                    }
                    updateGameHeader()
                    true
                }
            } else false
        }

        gameView?.onMultiTouchClick = { touches ->
            if (viewModel.isGameRunning) {
                val hitBugs = viewModel.gameEngine.checkMultiHit(touches)
                if (hitBugs.isNotEmpty()) {
                    for (b in hitBugs) {
                        viewModel.hitBug(b.points)
                    }
                    Toast.makeText(this@MainActivity, "⚡ Мультитач поразил ${hitBugs.size} жуков!", Toast.LENGTH_SHORT).show()
                    updateGameHeader()
                }
            }
        }
    }

    private fun updateGameSkillsSpinner() {
        val spinner = findViewById<Spinner?>(R.id.spinnerGameSkills)
        val buttonActivate = findViewById<Button?>(R.id.buttonActivateSkill)
        availableSkills = viewModel.getOwnedSkills()

        if (availableSkills.isEmpty()) {
            val emptyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("УМЕНИЙ НЕТ"))
            spinner?.adapter = emptyAdapter
            buttonActivate?.isEnabled = false
            viewModel.selectedSkill = null
        } else {
            val items = availableSkills.map { "${it.icon} ${it.title} (зарядов: ${viewModel.getChargesForSkill(it)})" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            spinner?.adapter = adapter
            buttonActivate?.isEnabled = true
            viewModel.selectedSkill = availableSkills.firstOrNull()
        }
    }

    private fun setupShop() {
        findViewById<Button>(R.id.buttonBuyFreeze).setOnClickListener { buySkill(SkillType.FREEZE) }
        findViewById<Button>(R.id.buttonBuyBomb).setOnClickListener { buySkill(SkillType.BOMB) }
        findViewById<Button>(R.id.buttonBuySlow).setOnClickListener { buySkill(SkillType.SLOW) }
        findViewById<Button>(R.id.buttonBuyMultitouch).setOnClickListener { buySkill(SkillType.MULTITOUCH) }
    }

    private fun buySkill(skill: SkillType) {
        lifecycleScope.launch {
            val res = viewModel.buySkill(skill)
            if (res.isSuccess) {
                Toast.makeText(this@MainActivity, "Успешно куплено: ${skill.title} (+3 заряда)!", Toast.LENGTH_SHORT).show()
                updateShopUI()
                updateGameSkillsSpinner()
                updateGameHeader()
            } else {
                Toast.makeText(this@MainActivity, res.exceptionOrNull()?.message ?: "Ошибка покупки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateShopUI() {
        findViewById<TextView?>(R.id.textShopPlayerName)?.text = "Игрок: ${viewModel.currentUserName}"
        findViewById<TextView?>(R.id.textShopBalance)?.text = "Баланс: ${viewModel.currentPlayerCoins} монет"

        findViewById<TextView?>(R.id.textChargesFreeze)?.text = "В наличии: ${viewModel.freezeCharges} зарядов"
        findViewById<TextView?>(R.id.textChargesBomb)?.text = "В наличии: ${viewModel.bombCharges} зарядов"
        findViewById<TextView?>(R.id.textChargesSlow)?.text = "В наличии: ${viewModel.slowCharges} зарядов"
        findViewById<TextView?>(R.id.textChargesMultitouch)?.text = "В наличии: ${viewModel.multitouchCharges} зарядов"
    }

    private fun onGameOver() {
        viewModel.isGameRunning = false
        stopBugScream()
        viewModel.gameEngine.bugs.clear()
        viewModel.gameEngine.bonus = null
        viewModel.gameEngine.isTiltModeActive = false
        viewModel.gameEngine.isFrozen = false
        viewModel.gameEngine.isSlowed = false
        viewModel.gameEngine.isMultitouchActive = false
        val gameView = findViewById<GameView?>(R.id.gameView)
        gameView?.bugs = emptyList()
        gameView?.bonus = null
        gameView?.invalidate()

        findViewById<Button?>(R.id.buttonStart)?.visibility = View.VISIBLE
        findViewById<TextView?>(R.id.textViewScore)?.text = "Игра окончена! Очки: ${viewModel.score}"

        val coinsEarned = viewModel.earnedCoinsThisRound

        lifecycleScope.launch {
            viewModel.saveCurrentGameSession()
            updateShopUI()
            updateGameHeader()
        }

        val message = """
            Раунд завершен!
            
            Игрок: ${viewModel.currentUserName}
            Итоговые очки: ${viewModel.score}
            Получено монет: +$coinsEarned
            Использовано умение: ${viewModel.usedSkillTitle}
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

    private fun setupRecordFilters() {
        val spinnerSort = findViewById<Spinner?>(R.id.spinnerSortBy)
        val sortOptions = listOf("Лучшие (по очкам)", "Последние (по дате)")
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortOptions)
        spinnerSort?.adapter = sortAdapter

        spinnerSort?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortByScore = (position == 0)
                loadRecords()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val spinnerPlayer = findViewById<Spinner?>(R.id.spinnerFilterPlayer)
        spinnerPlayer?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterPlayerId = if (position == 0) null else playersList.getOrNull(position - 1)?.id
                loadRecords()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updatePlayerFilterSpinner() {
        lifecycleScope.launch {
            playersList = viewModel.getAllPlayers()
            val names = mutableListOf("Все игроки")
            names.addAll(playersList.map { it.name })

            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names)
            findViewById<Spinner?>(R.id.spinnerFilterPlayer)?.adapter = adapter
        }
    }

    private fun loadRecords() {
        lifecycleScope.launch {
            val records = viewModel.getFilteredRecords(filterPlayerId, sortByScore)
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
            val players = viewModel.getAllPlayers()
            if (players.isEmpty()) {
                Toast.makeText(this@MainActivity, "Нет зарегистрированных игроков", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val userNames = players.map { "${it.name} (монет: ${it.coins})" }.toTypedArray()
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Выберите игрока")
                .setItems(userNames) { _, which ->
                    val selectedUser = players[which]
                    viewModel.syncPlayer(selectedUser)
                    updateGameHeader()
                    updateShopUI()
                    updateGameSkillsSpinner()
                    Toast.makeText(this@MainActivity, "Текущий игрок: ${selectedUser.name}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun registerUser(name: String, zodiac: String) {
        lifecycleScope.launch {
            val player = viewModel.registerOrGetPlayer(name, zodiac)
            updateGameHeader()
            updateShopUI()
            updateGameSkillsSpinner()
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
                    val tabsCount = 7

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
        stopBugScream()
        findViewById<Button?>(R.id.buttonStart)?.visibility = View.VISIBLE
        updateGameHeader()
        updateGameSkillsSpinner()

        val gameView = findViewById<GameView?>(R.id.gameView)
        gameView?.bugs = emptyList()
        gameView?.bonus = null
        gameView?.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameLoopRunnable?.let { handler.removeCallbacks(it) }
        stopBugScream()
    }
}