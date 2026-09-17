package com.example.rolltimer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.rolltimer.databinding.ActivityLanguageSelectBinding

/**
 * Показується один-єдиний раз — при першому запуску застосунку, поки
 * користувач ще не обрав мову. Після вибору більше ніколи не з'являється
 * (мову потім можна змінити в Налаштуваннях).
 */
class LanguageSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pickEnButton.setOnClickListener { choose("en") }
        binding.pickUkButton.setOnClickListener { choose("uk") }
        binding.pickLtButton.setOnClickListener { choose("lt") }
    }

    private fun choose(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        SettingsStore.setLanguageChosen(this, true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
