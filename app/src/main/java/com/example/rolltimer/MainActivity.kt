package com.example.rolltimer

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.rolltimer.databinding.ActivityMainBinding
import com.example.rolltimer.databinding.TimerRowBinding
import java.util.UUID

/** Дешборд: список усіх таймерів одразу видно, чи скоро треба міняти рулон. */
class MainActivity : LocaleAwareActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val rows = mutableListOf<RowHolder>()

    private data class RowHolder(var data: TimerData, val binding: TimerRowBinding)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        volumeControlStream = AudioManager.STREAM_MUSIC

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
        b.rowCycleText.text = getString(R.string.roll_label_fmt, d.cycle)

        if (!d.running) {
            b.rowTimeText.text = "--:--"
            b.statusDot.setBackgroundResource(R.drawable.dot_grey)
            b.rowMetersText.text = if (d.pausedRemainingMs > 0 && d.speed > 0) {
                metersProgressText(d, d.pausedRemainingMs / 1000.0)
            } else ""
            return
        }
        val remaining = ((d.endTimestamp - System.currentTimeMillis()) / 1000.0).coerceAtLeast(0.0)
        b.rowTimeText.text = TimeFmt.format(remaining)
        b.rowMetersText.text = if (d.speed > 0) metersProgressText(d, remaining) else ""
        b.statusDot.setBackgroundResource(
            when {
                remaining <= 60 -> R.drawable.dot_red
                remaining <= 300 -> R.drawable.dot_orange
                else -> R.drawable.dot_green
            }
        )
    }

    private fun metersProgressText(d: TimerData, remainingSec: Double): String {
        val metersPassed = (d.totalLength - remainingSec * d.speed).coerceIn(0.0, d.totalLength)
        return getString(R.string.meters_progress_fmt, fmtMeters(metersPassed), fmtMeters(d.totalLength))
    }

    private fun fmtMeters(v: Double): String {
        val rounded = Math.round(v * 10) / 10.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else "%.1f".format(rounded)
    }

    private fun openDetail(id: String) {
        startActivity(Intent(this, TimerDetailActivity::class.java).putExtra(TimerDetailActivity.EXTRA_TIMER_ID, id))
    }

    private fun addNewTimerAndOpenDetail() {
        val all = TimerStore.loadAll(this)
        val data = TimerData(
            id = UUID.randomUUID().toString(),
            name = getString(R.string.new_timer_default_name_fmt, all.size + 1),
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
}
