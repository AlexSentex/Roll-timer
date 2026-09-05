package com.example.rolltimer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * 15 різних звукових сигналів, згенерованих самим телефоном (без аудіофайлів).
 * Гучність береться з поточного рівня гучності "Будильник" на пристрої —
 * тобто регулюється звичайними кнопками гучності або повзунком у Налаштуваннях.
 */
object SoundSignals {

    val NAMES = listOf(
        "Три короткі гудки",
        "Один довгий гудок",
        "Висхідна трель",
        "Тривожні подвійні",
        "М'який сигнал «Готово»",
        "Спадна трель",
        "Два довгих гудки",
        "Швидкі п'ять гудків",
        "Класичний дзвінок",
        "SOS (три-три-три)",
        "Висхідна гама",
        "Спадна гама",
        "Подвійний піп-піп",
        "Акорд підтвердження",
        "Сирена (чергування)"
    )

    // Триплет: (тип тону, тривалість мс, пауза після мс)
    private fun pattern(index: Int): List<Triple<Int, Int, Int>> = when (index) {
        0 -> listOf(
            Triple(ToneGenerator.TONE_PROP_BEEP2, 150, 100),
            Triple(ToneGenerator.TONE_PROP_BEEP2, 150, 100),
            Triple(ToneGenerator.TONE_PROP_BEEP2, 150, 0)
        )
        1 -> listOf(Triple(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900, 0))
        2 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_1, 120, 40),
            Triple(ToneGenerator.TONE_DTMF_3, 120, 40),
            Triple(ToneGenerator.TONE_DTMF_5, 120, 40),
            Triple(ToneGenerator.TONE_DTMF_7, 180, 0)
        )
        3 -> listOf(
            Triple(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200, 120),
            Triple(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200, 300),
            Triple(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200, 120),
            Triple(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200, 0)
        )
        4 -> listOf(
            Triple(ToneGenerator.TONE_PROP_ACK, 300, 60),
            Triple(ToneGenerator.TONE_PROP_ACK, 500, 0)
        )
        5 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_9, 120, 40),
            Triple(ToneGenerator.TONE_DTMF_7, 120, 40),
            Triple(ToneGenerator.TONE_DTMF_5, 120, 40),
            Triple(ToneGenerator.TONE_DTMF_3, 120, 40),
            Triple(ToneGenerator.TONE_DTMF_1, 180, 0)
        )
        6 -> listOf(
            Triple(ToneGenerator.TONE_PROP_BEEP2, 900, 250),
            Triple(ToneGenerator.TONE_PROP_BEEP2, 900, 0)
        )
        7 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_0, 90, 60),
            Triple(ToneGenerator.TONE_DTMF_0, 90, 60),
            Triple(ToneGenerator.TONE_DTMF_0, 90, 60),
            Triple(ToneGenerator.TONE_DTMF_0, 90, 60),
            Triple(ToneGenerator.TONE_DTMF_0, 90, 0)
        )
        8 -> listOf(
            Triple(ToneGenerator.TONE_SUP_RINGTONE, 400, 200),
            Triple(ToneGenerator.TONE_SUP_RINGTONE, 400, 0)
        )
        9 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_1, 100, 80),
            Triple(ToneGenerator.TONE_DTMF_1, 100, 80),
            Triple(ToneGenerator.TONE_DTMF_1, 100, 200),
            Triple(ToneGenerator.TONE_DTMF_1, 260, 80),
            Triple(ToneGenerator.TONE_DTMF_1, 260, 80),
            Triple(ToneGenerator.TONE_DTMF_1, 260, 200),
            Triple(ToneGenerator.TONE_DTMF_1, 100, 80),
            Triple(ToneGenerator.TONE_DTMF_1, 100, 80),
            Triple(ToneGenerator.TONE_DTMF_1, 100, 0)
        )
        10 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_0, 130, 30),
            Triple(ToneGenerator.TONE_DTMF_2, 130, 30),
            Triple(ToneGenerator.TONE_DTMF_4, 130, 30),
            Triple(ToneGenerator.TONE_DTMF_6, 130, 30),
            Triple(ToneGenerator.TONE_DTMF_8, 180, 0)
        )
        11 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_8, 130, 30),
            Triple(ToneGenerator.TONE_DTMF_6, 130, 30),
            Triple(ToneGenerator.TONE_DTMF_4, 130, 30),
            Triple(ToneGenerator.TONE_DTMF_2, 130, 30),
            Triple(ToneGenerator.TONE_DTMF_0, 180, 0)
        )
        12 -> listOf(
            Triple(ToneGenerator.TONE_CDMA_PIP, 150, 100),
            Triple(ToneGenerator.TONE_CDMA_PIP, 150, 300),
            Triple(ToneGenerator.TONE_CDMA_PIP, 150, 100),
            Triple(ToneGenerator.TONE_CDMA_PIP, 150, 0)
        )
        13 -> listOf(
            Triple(ToneGenerator.TONE_CDMA_CONFIRM, 250, 80),
            Triple(ToneGenerator.TONE_PROP_ACK, 350, 0)
        )
        else -> listOf(
            Triple(ToneGenerator.TONE_DTMF_1, 140, 60),
            Triple(ToneGenerator.TONE_DTMF_9, 140, 60),
            Triple(ToneGenerator.TONE_DTMF_1, 140, 60),
            Triple(ToneGenerator.TONE_DTMF_9, 140, 60),
            Triple(ToneGenerator.TONE_DTMF_1, 140, 60),
            Triple(ToneGenerator.TONE_DTMF_9, 140, 0)
        )
    }

    fun play(context: Context, signalIndex: Int) {
        val steps = pattern(signalIndex)
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val cur = am.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val volumePct = ((cur * 100) / max).coerceIn(0, 100)

        val tg = try {
            ToneGenerator(AudioManager.STREAM_ALARM, volumePct)
        } catch (e: Exception) {
            return
        }
        val handler = Handler(Looper.getMainLooper())
        var delay = 0L
        for ((tone, dur, gap) in steps) {
            handler.postDelayed({
                try { tg.startTone(tone, dur) } catch (e: Exception) { }
            }, delay)
            delay += dur + gap
        }
        handler.postDelayed({
            try { tg.release() } catch (e: Exception) { }
        }, delay + 300)
    }
}
