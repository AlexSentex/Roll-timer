package com.example.rolltimer

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.rolltimer.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        volumeControlStream = AudioManager.STREAM_ALARM

        binding.backButton.setOnClickListener { finish() }

        binding.keepScreenSwitch.isChecked = SettingsStore.isKeepScreenOn(this)
        binding.keepScreenSwitch.setOnCheckedChangeListener { _, checked ->
            SettingsStore.setKeepScreenOn(this, checked)
        }

        binding.notifButton.setOnClickListener { requestNotifPermission() }
        binding.exactAlarmButton.setOnClickListener { requestExactAlarmPermission() }

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        binding.volumeSeekBar.max = max
        binding.volumeSeekBar.progress = am.getStreamVolume(AudioManager.STREAM_ALARM)
        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) am.setStreamVolume(AudioManager.STREAM_ALARM, progress, 0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.listenTestButton.setOnClickListener { SoundSignals.play(this, 0) }
    }

    override fun onResume() {
        super.onResume()
        updateNotifUi()
        updateExactAlarmUi()
    }

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
}
