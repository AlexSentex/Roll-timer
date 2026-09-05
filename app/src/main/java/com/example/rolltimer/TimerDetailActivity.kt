package com.example.rolltimer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.rolltimer.databinding.ActivityTimerDetailBinding

/** Повні налаштування й керування одним таймером: калібрування, довжина, сигнал, старт/пауза. */
class TimerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimerDetailBinding
    private lateinit var data: TimerData
    private val handler = Handler(Looper.getMainLooper())

    private var calibStartMs = 0L
    private var calibRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra(EXTRA_TIMER_ID)
        val found = TimerStore.loadAll(this).find { it.id == id }
        if (found == null) {
            finish()
            return
        }
        data = found

        bindUi()
        binding.backButton.setOnClickListener { finish() }
        binding.deleteButton.setOnClickListener { deleteTimer() }
    }

    override fun onResume() {
        super.onResume()
        syncFromStore()
        handler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tickRunnable)
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            tickUi()
            handler.postDelayed(this, 1000)
        }
    }

    private fun syncFromStore() {
        val fresh = TimerStore.loadAll(this).find { it.id == data.id } ?: return
        data.cycle = fresh.cycle
        data.endTimestamp = fresh.endTimestamp
        data.running = fresh.running
        refreshDisplay()
    }

    private fun bindUi() {
        binding.titleText.text = data.name
        binding.nameEdit.setText(data.name)
        binding.totalLengthEdit.setText(fmtNum(data.totalLength))
        binding.alreadyPassedEdit.setText(fmtNum(data.alreadyPassed))
        if (data.speed > 0) {
            binding.speedManualEdit.setText("%.3f".format(data.speed))
            binding.speedInfoText.text = "Швидкість: ${"%.3f".format(data.speed)} м/с"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, SoundSignals.NAMES)
        binding.signalSpinner.adapter = adapter
        binding.signalSpinner.setSelection(data.signalIndex.coerceIn(0, SoundSignals.NAMES.size - 1))

        binding.nameEdit.doAfterTextChanged {
            val v = it?.toString()
            if (!v.isNullOrBlank()) {
                data.name = v
                binding.titleText.text = v
                persist()
            }
        }

        binding.calibStartButton.setOnClickListener { startCalib() }
        binding.calibMarkButton.setOnClickListener { markCalib() }
        binding.calibMarkButton.isEnabled = false

        binding.speedManualEdit.doAfterTextChanged {
            val v = it?.toString()?.replace(',', '.')?.toDoubleOrNull()
            if (v != null && v > 0) {
                data.speed = v
                binding.speedInfoText.text = "Швидкість (вручну): ${"%.3f".format(v)} м/с"
                persist()
            }
        }

        binding.computeButton.setOnClickListener { computeFullTime(showError = true) }
        binding.listenButton.setOnClickListener {
            SoundSignals.play(this, binding.signalSpinner.selectedItemPosition)
        }

        binding.startButton.setOnClickListener { start() }
        binding.pauseButton.setOnClickListener { pause() }
        binding.resetButton.setOnClickListener { reset() }

        refreshDisplay()
    }

    private fun fmtNum(v: Double) =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

    private fun startCalib() {
        calibStartMs = System.currentTimeMillis()
        calibRunning = true
        binding.calibStartButton.isEnabled = false
        binding.calibMarkButton.isEnabled = true
        tickCalib()
    }

    private fun tickCalib() {
        if (!calibRunning) return
        val elapsed = (System.currentTimeMillis() - calibStartMs) / 1000.0
        binding.calibElapsedText.text = TimeFmt.format(elapsed)
        binding.calibElapsedText.postDelayed({ tickCalib() }, 200)
    }

    private fun markCalib() {
        if (!calibRunning) return
        val elapsedSec = (System.currentTimeMillis() - calibStartMs) / 1000.0
        val meters = binding.calibMetersEdit.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
        if (meters == null || meters <= 0 || elapsedSec <= 0) {
            Toast.makeText(this, "Введіть коректну кількість метрів (>0).", Toast.LENGTH_SHORT).show()
            return
        }
        data.speed = meters / elapsedSec
        binding.speedManualEdit.setText("%.3f".format(data.speed))
        binding.speedInfoText.text =
            "Швидкість: ${"%.3f".format(data.speed)} м/с (${fmtNum(meters)} м за ${"%.1f".format(elapsedSec)} с)"
        calibRunning = false
        binding.calibMarkButton.isEnabled = false
        binding.calibStartButton.isEnabled = true
        persist()
    }

    private fun computeFullTime(showError: Boolean): Double? {
        if (data.speed <= 0) {
            if (showError) Toast.makeText(this, "Спершу визначте швидкість (замір або вручну).", Toast.LENGTH_SHORT).show()
            return null
        }
        val totalLen = binding.totalLengthEdit.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
        if (totalLen == null || totalLen <= 0) {
            if (showError) Toast.makeText(this, "Введіть коректну довжину рулону (>0).", Toast.LENGTH_SHORT).show()
            return null
        }
        data.totalLength = totalLen
        val fullTime = totalLen / data.speed
        binding.fullTimeInfoText.text = "Повний час рулону: ${TimeFmt.format(fullTime)}"
        persist()
        return fullTime
    }

    private fun firstCycleTime(): Double? {
        val fullTime = computeFullTime(showError = true) ?: return null
        val already = binding.alreadyPassedEdit.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        data.alreadyPassed = already
        val remainingM = (data.totalLength - already).coerceAtLeast(0.0)
        return remainingM / data.speed
    }

    private fun start() {
        if (data.running) return
        val remainSec = if (data.cycle == 1) firstCycleTime() else computeFullTime(showError = true)
        if (remainSec == null) return
        data.signalIndex = binding.signalSpinner.selectedItemPosition
        data.endTimestamp = System.currentTimeMillis() + Math.round(remainSec * 1000)
        data.running = true
        AlarmReceiver.scheduleAlarm(
            this, data.id, data.name, data.speed, data.totalLength,
            data.signalIndex, data.cycle + 1, data.endTimestamp
        )
        persist()
        refreshDisplay()
    }

    private fun pause() {
        data.running = false
        AlarmReceiver.cancelAlarm(this, data.id)
        persist()
        refreshDisplay()
    }

    private fun reset() {
        data.running = false
        data.cycle = 1
        data.endTimestamp = 0
        AlarmReceiver.cancelAlarm(this, data.id)
        persist()
        refreshDisplay()
    }

    private fun deleteTimer() {
        AlarmReceiver.cancelAlarm(this, data.id)
        val all = TimerStore.loadAll(this).filterNot { it.id == data.id }
        TimerStore.saveAll(this, all)
        finish()
    }

    private fun persist() {
        val all = TimerStore.loadAll(this)
        val idx = all.indexOfFirst { it.id == data.id }
        if (idx >= 0) all[idx] = data else all.add(data)
        TimerStore.saveAll(this, all)
    }

    private fun tickUi() {
        if (!data.running) {
            binding.startButton.isEnabled = true
            binding.pauseButton.isEnabled = false
            return
        }
        val remaining = (data.endTimestamp - System.currentTimeMillis()) / 1000.0
        if (remaining <= 0) {
            // Alarm уже мав спрацювати у фоні й оновити сховище — підхоплюємо новий цикл
            syncFromStore()
        } else {
            binding.displayText.text = TimeFmt.format(remaining)
            binding.cycleText.text = "Рулон №${data.cycle}"
        }
    }

    private fun refreshDisplay() {
        binding.startButton.isEnabled = !data.running
        binding.pauseButton.isEnabled = data.running
        binding.displayText.text = if (data.running) {
            TimeFmt.format((data.endTimestamp - System.currentTimeMillis()) / 1000.0)
        } else "--:--"
        binding.cycleText.text = "Рулон №${data.cycle}"
    }

    companion object {
        const val EXTRA_TIMER_ID = "timer_id"
    }
}
