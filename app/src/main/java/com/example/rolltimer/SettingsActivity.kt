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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.rolltimer.databinding.ActivitySettingsBinding

class SettingsActivity : LocaleAwareActivity() {

    private lateinit var binding: ActivitySettingsBinding

    // Список підтримуваних мов: (тег локалі, назва мовою оригіналу).
    // Щоб додати нову мову: сюди новий рядок + values-XX/strings.xml +
    // рядок у app/src/main/res/xml/locales_config.xml.
    private val languages = listOf(
        "en" to "English",
        "uk" to "Українська",
        "lt" to "Lietuvių"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        volumeControlStream = AudioManager.STREAM_MUSIC

        binding.backButton.setOnClickListener { finish() }

        setupLanguageSpinner()

        binding.keepScreenSwitch.isChecked = SettingsStore.isKeepScreenOn(this)
        binding.keepScreenSwitch.setOnCheckedChangeListener { _, checked ->
            SettingsStore.setKeepScreenOn(this, checked)
        }

        binding.notificationsEnabledSwitch.isChecked = SettingsStore.isNotificationsEnabled(this)
        binding.notificationsEnabledSwitch.setOnCheckedChangeListener { _, checked ->
            SettingsStore.setNotificationsEnabled(this, checked)
        }

        binding.notifButton.setOnClickListener { requestNotifPermission() }
        binding.exactAlarmButton.setOnClickListener { requestExactAlarmPermission() }

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.volumeSeekBar.max = max
        binding.volumeSeekBar.progress = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
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

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, languages.map { it.second }
        )
        binding.languageSpinner.adapter = adapter

        val currentTag = AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .substringBefore("-")
            .ifBlank { "en" }
        val currentIndex = languages.indexOfFirst { it.first == currentTag }.let { if (it >= 0) it else 0 }
        binding.languageSpinner.setSelection(currentIndex)

        // Слухач ставимо ПІСЛЯ setSelection, щоб не спрацював одразу при відкритті екрана
        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val tag = languages[position].first
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                SettingsStore.setLanguageChosen(this@SettingsActivity, true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
            binding.notifStatusText.text = getString(R.string.notif_on)
            binding.notifButton.visibility = View.GONE
        } else {
            binding.notifStatusText.text = getString(R.string.notif_off)
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
            binding.exactAlarmStatusText.text = getString(R.string.exact_alarm_on)
            binding.exactAlarmButton.visibility = View.GONE
        } else {
            binding.exactAlarmStatusText.text = getString(R.string.exact_alarm_off)
            binding.exactAlarmButton.visibility = View.VISIBLE
        }
    }
}
