package com.example.rolltimer

import android.content.Context

/** Глобальні налаштування застосунку (не прив'язані до конкретного таймера). */
object SettingsStore {
    private const val PREFS = "roll_timer_settings"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_LANGUAGE_CHOSEN = "language_chosen"

    fun isKeepScreenOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_KEEP_SCREEN_ON, false)

    fun setKeepScreenOn(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
    }

    // Окремо від дозволу ОС: навіть якщо дозвіл на сповіщення надано,
    // цей перемикач дозволяє вимкнути показ сповіщень усередині застосунку
    // (дозвіл ОС назад забрати з коду не можна — лише через системні налаштування).
    fun isNotificationsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setNotificationsEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()
    }

    // Чи вже проходив користувач екран вибору мови при першому запуску.
    fun isLanguageChosen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LANGUAGE_CHOSEN, false)

    fun setLanguageChosen(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LANGUAGE_CHOSEN, value).apply()
    }
}
