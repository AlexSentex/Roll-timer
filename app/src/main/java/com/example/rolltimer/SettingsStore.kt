package com.example.rolltimer

import android.content.Context

/** Глобальні налаштування застосунку (не прив'язані до конкретного таймера). */
object SettingsStore {
    private const val PREFS = "roll_timer_settings"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"

    fun isKeepScreenOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_KEEP_SCREEN_ON, false)

    fun setKeepScreenOn(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
    }
}
