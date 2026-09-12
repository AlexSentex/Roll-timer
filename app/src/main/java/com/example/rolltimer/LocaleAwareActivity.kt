package com.example.rolltimer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

/**
 * Базовий клас для всіх екранів застосунку.
 *
 * Проблема: коли мову міняють у Налаштуваннях, AndroidX автоматично
 * перебудовує (recreate) лише той екран, що зараз видно. Екрани, які
 * лишились "позаду" в стеку навігації (наприклад дешборд під час
 * відкритих Налаштувань), НЕ перебудовуються самі — їхні вже намальовані
 * написи лишаються старою мовою, поки їх не відкрити знову з нуля.
 *
 * Рішення: кожен екран запам'ятовує, якою мовою його було створено,
 * і при поверненні на нього (onResume) звіряє з поточною — якщо мова
 * змінилась, сам себе перебудовує.
 */
abstract class LocaleAwareActivity : AppCompatActivity() {

    private var createdWithLocaleTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createdWithLocaleTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }

    override fun onResume() {
        super.onResume()
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (createdWithLocaleTag != null && createdWithLocaleTag != currentTag) {
            recreate()
        }
    }
}
