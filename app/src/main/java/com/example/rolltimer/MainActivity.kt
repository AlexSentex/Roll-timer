package com.example.rolltimer

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.rolltimer.databinding.ActivityMainBinding
import com.example.rolltimer.databinding.TimerRowBinding
import java.util.UUID

/** Дешборд: список усіх таймерів одразу видно, чи скоро треба міняти рулон. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val rows = mutableListOf<RowHolder>()

    private data class RowHolder(var data: TimerData, val binding: TimerRowBinding)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        volumeControlStream = AudioManager.STREAM_ALARM

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.addButton.setOnClickListener { addNewTimerAndOpenDetail() }

        handler.post(tickRunnable)
    }

    override fun onResume() {
        super.onResume()
        binding.root.keepScreenOn = SettingsStore.isKeepScreenOn(this)
        rebuildList()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            tickAllRows()
            handler.postDelayed(this, 1000)
        }
    }

    private fun rebuildList() {
        binding.dashboardList.removeAllViews()
        rows.clear()
        val timers = TimerStore.loadAll(this)
        binding.emptyText.visibility = if (timers.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        timers.forEach { addRow(it) }
    }

    private fun addRow(data: TimerData) {
        val rowBinding = TimerRowBinding.inflate(layoutInflater, binding.dashboardList, false)
        val holder = RowHolder(data, rowBinding)
        rowBinding.root.setOnClickListener { openDetail(holder.data.id) }
        rowBinding.rowSettingsButton.setOnClickListener { openDetail(holder.data.id) }
        binding.dashboardList.addView(rowBinding.root)
        rows.add(holder)
        updateRow(holder)
    }

    private fun tickAllRows() {
        val needsReload = rows.any { holder ->
            holder.data.running &&
                (holder.data.endTimestamp - System.currentTimeMillis()) / 1000.0 <= 0
        }
        if (needsReload) {
            val fresh = TimerStore.loadAll(this).associateBy { it.id }
            rows.forEach { holder ->
                fresh[holder.data.id]?.let {
                    holder.data.cycle = it.cycle
                    holder.data.endTimestamp = it.endTimestamp
                    holder.data.running = it.running
                }
            }
        }
        rows.forEach { updateRow(it) }
    }

    private fun updateRow(holder: RowHolder) {
        val d = holder.data
        val b = holder.binding
        b.rowNameText.text = d.name
        b.rowCycleText.text = "Рулон №${d.cycle}"

        if (!d.running) {
            b.rowTimeText.text = "--:--"
            b.statusDot.setBackgroundResource(R.drawable.dot_grey)
            return
        }
        val remaining = ((d.endTimestamp - System.currentTimeMillis()) / 1000.0).coerceAtLeast(0.0)
        b.rowTimeText.text = TimeFmt.format(remaining)
        b.statusDot.setBackgroundResource(
            when {
                remaining <= 60 -> R.drawable.dot_red
                remaining <= 300 -> R.drawable.dot_orange
                else -> R.drawable.dot_green
            }
        )
    }

    private fun openDetail(id: String) {
        startActivity(Intent(this, TimerDetailActivity::class.java).putExtra(TimerDetailActivity.EXTRA_TIMER_ID, id))
    }

    private fun addNewTimerAndOpenDetail() {
        val all = TimerStore.loadAll(this)
        val data = TimerData(
            id = UUID.randomUUID().toString(),
            name = "Плівка ${all.size + 1}",
            speed = 0.0,
            totalLength = 100.0,
            alreadyPassed = 0.0,
            signalIndex = 0,
            cycle = 1,
            endTimestamp = 0L,
            running = false
        )
        all.add(data)
        TimerStore.saveAll(this, all)
        openDetail(data.id)
    }
}        binding.exactAlarmButton.setOnClickListener { requestExactAlarmPermission() }
        binding.addButton.setOnClickListener { addTimer(null) }

        updateWakeUi()
        updateNotifUi()
        updateExactAlarmUi()

        val saved = TimerStore.loadAll(this)
        if (saved.isEmpty()) {
            addTimer(null)
        } else {
            saved.forEach { addTimer(it) }
        }

        handler.post(tickRunnable)
    }

    override fun onResume() {
        super.onResume()
        updateWakeUi()
        updateNotifUi()
        updateExactAlarmUi()
        // Підхоплюємо стан, який міг змінитись у фоні через AlarmReceiver
        val saved = TimerStore.loadAll(this).associateBy { it.id }
        cards.forEach { card ->
            saved[card.data.id]?.let { fresh ->
                card.data.cycle = fresh.cycle
                card.data.endTimestamp = fresh.endTimestamp
                card.data.running = fresh.running
                card.refreshDisplay()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            cards.forEach { it.tickUiOnly() }
            handler.postDelayed(this, 1000)
        }
    }

    // ---------- Wake lock ----------
    private fun toggleWakeLock() {
        wakeLockWanted = !wakeLockWanted
        if (wakeLockWanted) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "RollTimer:wakelock"
            )
            wakeLock?.acquire(4 * 60 * 60 * 1000L)
        } else {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        }
        updateWakeUi()
    }

    private fun updateWakeUi() {
        if (wakeLockWanted && wakeLock?.isHeld == true) {
            binding.wakeStatusText.text = "💡 Екран не вимикається"
            binding.wakeButton.text = "Вимкнути"
        } else {
            binding.wakeStatusText.text = "💡 Екран вимкнеться сам"
            binding.wakeButton.text = "Не вимикати екран"
        }
    }

    // ---------- Сповіщення ----------
    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        updateNotifUi()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateNotifUi()
    }

    private fun updateNotifUi() {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) {
            binding.notifStatusText.text = "🔔 Сповіщення увімкнено"
            binding.notifButton.visibility = View.GONE
        } else {
            binding.notifStatusText.text = "🔔 Сповіщення вимкнено"
            binding.notifButton.visibility = View.VISIBLE
        }
    }

    // ---------- Точні будильники (Android 12+) ----------
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun updateExactAlarmUi() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (ok) {
            binding.exactAlarmStatusText.text = "⏰ Точні будильники дозволені"
            binding.exactAlarmButton.visibility = View.GONE
        } else {
            binding.exactAlarmStatusText.text = "⏰ Точні будильники не дозволені"
            binding.exactAlarmButton.visibility = View.VISIBLE
        }
    }

    // ---------- Таймери ----------
    private fun addTimer(existing: TimerData?) {
        val data = existing ?: TimerData(
            id = UUID.randomUUID().toString(),
            name = "Плівка ${cards.size + 1}",
            rate = 0.0,
            totalLength = 100.0,
            alreadyPassed = 0.0,
            signalIndex = 0,
            cycle = 1,
            endTimestamp = 0L,
            running = false
        )
        val cardBinding = TimerCardBinding.inflate(layoutInflater, binding.timerContainer, false)
        val card = TimerCard(this, cardBinding, data, ::persist, ::removeCard)
        cards.add(card)
        binding.timerContainer.addView(cardBinding.root)
        card.bindInitial()
        persist()
    }

    private fun removeCard(card: TimerCard) {
        AlarmReceiver.cancelAlarm(this, card.data.id)
        binding.timerContainer.removeView(card.binding.root)
        cards.remove(card)
        persist()
    }

    private fun persist() {
        TimerStore.saveAll(this, cards.map { it.data })
    }
}
