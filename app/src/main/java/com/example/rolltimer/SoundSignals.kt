package com.example.rolltimer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * 15 звукових сигналів. Використовуються ЛИШЕ DTMF-тони (ноти телефонної
 * клавіатури 0-9) — вони тримають рівний звук рівно стільки мс, скільки
 * задано, без внутрішнього повторення короткими "бліпами". Кожен сигнал 1-4 сек.
 *
 * Перед сигналом система забирає аудіофокус (AUDIOFOCUS_GAIN_TRANSIENT) —
 * це змушує музику/подкасти або поставитись на паузу, або значно притишитись
 * на час сигналу, і відновитись одразу після. Гучність сигналу — з медіа-потоку,
 * на повну (в межах поточного рівня гучності медіа), щоб не губився серед звуку.
 */
object SoundSignals {

    val NAMES = listOf(
        "Три короткі гудки",
        "Один довгий гудок",
        "Висхідна трель (4 ноти)",
        "Швидкі подвійні гудки",
        "М'який подвійний сигнал",
        "Спадна трель (4 ноти)",
        "Два довгих гудки",
        "П'ять швидких гудків",
        "Дзвінок-трель",
        "SOS (коротко-довго-коротко)",
        "Висхідна гама (5 нот)",
        "Спадна гама (5 нот)",
        "Подвійний піп-піп",
        "Акорд підтвердження",
        "Чергування високий-низький"
    )

    // Триплет: (DTMF-цифра 0-9, тривалість мс, пауза після мс). Сума — 1-4 сек.
    private fun pattern(index: Int): List<Triple<Int, Int, Int>> = when (index) {
        0 -> listOf(Triple(9, 250, 200), Triple(9, 250, 200), Triple(9, 250, 0))
        1 -> listOf(Triple(9, 1500, 0))
        2 -> listOf(Triple(1, 200, 60), Triple(3, 200, 60), Triple(5, 200, 60), Triple(7, 250, 0))
        3 -> listOf(Triple(7, 150, 80), Triple(7, 150, 300), Triple(7, 150, 80), Triple(7, 150, 0))
        4 -> listOf(Triple(2, 400, 120), Triple(2, 500, 0))
        5 -> listOf(Triple(9, 200, 60), Triple(7, 200, 60), Triple(5, 200, 60), Triple(3, 250, 0))
        6 -> listOf(Triple(9, 900, 300), Triple(9, 900, 0))
        7 -> listOf(
            Triple(0, 150, 80), Triple(0, 150, 80), Triple(0, 150, 80),
            Triple(0, 150, 80), Triple(0, 150, 0)
        )
        8 -> listOf(Triple(3, 350, 150), Triple(6, 350, 150), Triple(3, 350, 0))
        9 -> listOf(
            Triple(7, 100, 80), Triple(7, 100, 80), Triple(7, 100, 200),
            Triple(7, 280, 80), Triple(7, 280, 80), Triple(7, 280, 200),
            Triple(7, 100, 80), Triple(7, 100, 80), Triple(7, 100, 0)
        )
        10 -> listOf(
            Triple(0, 150, 50), Triple(2, 150, 50), Triple(4, 150, 50),
            Triple(6, 150, 50), Triple(8, 180, 0)
        )
        11 -> listOf(
            Triple(8, 150, 50), Triple(6, 150, 50), Triple(4, 150, 50),
            Triple(2, 150, 50), Triple(0, 180, 0)
        )
        12 -> listOf(Triple(7, 150, 100), Triple(7, 150, 350), Triple(7, 150, 100), Triple(7, 150, 0))
        13 -> listOf(Triple(2, 450, 150), Triple(5, 450, 0))
        else -> listOf(
            Triple(1, 140, 60), Triple(9, 140, 60), Triple(1, 140, 60),
            Triple(9, 140, 60), Triple(1, 140, 60), Triple(9, 140, 0)
        )
    }

    private fun dtmfToneType(digit: Int): Int = when (digit) {
        0 -> ToneGenerator.TONE_DTMF_0
        1 -> ToneGenerator.TONE_DTMF_1
        2 -> ToneGenerator.TONE_DTMF_2
        3 -> ToneGenerator.TONE_DTMF_3
        4 -> ToneGenerator.TONE_DTMF_4
        5 -> ToneGenerator.TONE_DTMF_5
        6 -> ToneGenerator.TONE_DTMF_6
        7 -> ToneGenerator.TONE_DTMF_7
        8 -> ToneGenerator.TONE_DTMF_8
        else -> ToneGenerator.TONE_DTMF_9
    }

    fun play(context: Context, signalIndex: Int) {
        val steps = pattern(signalIndex)
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val focusRequest = requestDuck(am)

        val tg = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            releaseDuck(am, focusRequest)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var delay = 0L
        for ((digit, dur, gap) in steps) {
            handler.postDelayed({
                try { tg.startTone(dtmfToneType(digit), dur) } catch (e: Exception) { }
            }, delay)
            delay += dur + gap
        }
        handler.postDelayed({
            try { tg.release() } catch (e: Exception) { }
            releaseDuck(am, focusRequest)
        }, delay + 300)
    }

    // Просимо систему тимчасово забрати аудіофокус — інші плеєри самі
    // поставлять на паузу або притишать себе на час сигналу.
    private fun requestDuck(am: AudioManager): Any? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { }
                .build()
            try {
                am.requestAudioFocus(req)
            } catch (e: Exception) { }
            req
        } else {
            try {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            } catch (e: Exception) { }
            null
        }
    }

    private fun releaseDuck(am: AudioManager, focusRequest: Any?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest is AudioFocusRequest) {
                am.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) { }
    }
}
