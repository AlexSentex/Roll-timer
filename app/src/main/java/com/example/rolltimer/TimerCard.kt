package com.example.rolltimer

import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.example.rolltimer.databinding.TimerCardBinding

/** Контролер однієї картки-таймера: калібрування, розрахунок часу, старт/пауза/скидання. */
class TimerCard(
    private val activity: MainActivity,
    val binding: TimerCardBinding,
    val data: TimerData,
    private val onChange: () -> Unit,
    private val onRemove: (TimerCard) -> Unit
) {
    private var calibStartMs: Long = 0L
    private var calibRunning = false

    fun bindInitial() {
        binding.nameEdit.setText(data.name)
        binding.totalLengthEdit.setText(fmtNum(data.totalLength))
        binding.alreadyPassedEdit.setText(fmtNum(data.alreadyPassed))
        if (data.rate > 0) {
            binding.rateManualEdit.setText("%.3f".format(data.rate))
            binding.rateInfoText.text = "Швидкість: ${"%.3f".format(data.rate)} сек/м"
        }

        val adapter = ArrayAdapter(
            activity, android.R.layout.simple_spinner_dropdown_item, SoundSignals.NAMES
        )
        binding.signalSpinner.adapter = adapter
        binding.signalSpinner.setSelection(data.signalIndex.coerceIn(0, SoundSignals.NAMES.size - 1))

        binding.nameEdit.doAfterTextChanged {
            data.name = it?.toString()?.ifBlank { data.name } ?: data.name
            onChange()
        }

        binding.calibStartButton.setOnClickListener { startCalib() }
        binding.calibMarkButton.setOnClickListener { markCalib() }
        binding.calibMarkButton.isEnabled = false

        binding.rateManualEdit.doAfterTextChanged {
            val v = it?.toString()?.replace(',', '.')?.toDoubleOrNull()
            if (v != null && v > 0) {
                data.rate = v
                binding.rateInfoText.text = "Швидкість (вручну): ${"%.3f".format(v)} сек/м"
            }
        }

        binding.computeButton.setOnClickListener { computeFullTime(showError = true) }
        binding.listenButton.setOnClickListener {
            SoundSignals.play(binding.signalSpinner.selectedItemPosition)
        }

        binding.startButton.setOnClickListener { start() }
        binding.pauseButton.setOnClickListener { pause() }
        binding.resetButton.setOnClickListener { reset() }
        binding.removeButton.setOnClickListener { onRemove(this) }

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
        binding.calibElapsedText.text = fmtTime(elapsed)
        binding.calibElapsedText.postDelayed({ tickCalib() }, 200)
    }

    private fun markCalib() {
        if (!calibRunning) return
        val elapsedSec = (System.currentTimeMillis() - calibStartMs) / 1000.0
        val meters = binding.calibMetersEdit.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
        if (meters == null || meters <= 0) {
            Toast.makeText(activity, "Введіть коректну кількість метрів (>0).", Toast.LENGTH_SHORT).show()
            return
        }
        data.rate = elapsedSec / meters
        binding.rateManualEdit.setText("%.3f".format(data.rate))
        binding.rateInfoText.text =
            "Швидкість: ${"%.3f".format(data.rate)} сек/м (${"%.1f".format(elapsedSec)} с на ${fmtNum(meters)} м)"
        calibRunning = false
        binding.calibMarkButton.isEnabled = false
        binding.calibStartButton.isEnabled = true
        onChange()
    }

    private fun computeFullTime(showError: Boolean): Double? {
        if (data.rate <= 0) {
            if (showError) Toast.makeText(activity, "Спершу визначте швидкість (замір або вручну).", Toast.LENGTH_SHORT).show()
            return null
        }
        val totalLen = binding.totalLengthEdit.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
        if (totalLen == null || totalLen <= 0) {
            if (showError) Toast.makeText(activity, "Введіть коректну довжину рулону (>0).", Toast.LENGTH_SHORT).show()
            return null
        }
        data.totalLength = totalLen
        val fullTime = totalLen * data.rate
        binding.fullTimeInfoText.text = "Повний час рулону: ${fmtTime(fullTime)}"
        onChange()
        return fullTime
    }

    private fun firstCycleTime(): Double? {
        val fullTime = computeFullTime(showError = true) ?: return null
        val already = binding.alreadyPassedEdit.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        data.alreadyPassed = already
        val remainingM = (data.totalLength - already).coerceAtLeast(0.0)
        return remainingM * data.rate
    }

    private fun start() {
        if (data.running) return
        val remainSec = if (data.cycle == 1) firstCycleTime() else computeFullTime(showError = true)
        if (remainSec == null) return
        data.signalIndex = binding.signalSpinner.selectedItemPosition
        data.endTimestamp = System.currentTimeMillis() + Math.round(remainSec * 1000)
        data.running = true
        AlarmReceiver.scheduleAlarm(
            activity, data.id, data.name, data.rate, data.totalLength,
            data.signalIndex, data.cycle + 1, data.endTimestamp
        )
        onChange()
        refreshDisplay()
    }

    private fun pause() {
        data.running = false
        AlarmReceiver.cancelAlarm(activity, data.id)
        onChange()
        refreshDisplay()
    }

    private fun reset() {
        data.running = false
        data.cycle = 1
        data.endTimestamp = 0
        AlarmReceiver.cancelAlarm(activity, data.id)
        onChange()
        refreshDisplay()
    }

    /** Викликається раз/сек з MainActivity — лише оновлює цифри на екрані. */
    fun tickUiOnly() {
        if (!data.running) return
        val remaining = (data.endTimestamp - System.currentTimeMillis()) / 1000.0
        binding.displayText.text = if (remaining <= 0) "00:00" else fmtTime(remaining)
        binding.cycleText.text = "Рулон №${data.cycle}"
    }

    fun refreshDisplay() {
        binding.startButton.isEnabled = !data.running
        binding.pauseButton.isEnabled = data.running
        binding.displayText.text = if (data.running) {
            fmtTime((data.endTimestamp - System.currentTimeMillis()) / 1000.0)
        } else "--:--"
        binding.cycleText.text = "Рулон №${data.cycle}"
    }

    private fun fmtTime(sec: Double): String {
        val s = Math.round(sec.coerceAtLeast(0.0))
        val h = s / 3600
        val m = (s % 3600) / 60
        val ss = s % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, ss) else "%02d:%02d".format(m, ss)
    }
}
