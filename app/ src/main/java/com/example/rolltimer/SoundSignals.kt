package com.example.rolltimer

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Прості звукові сигнали, що генеруються самим телефоном (без аудіофайлів),
 * тому не потрібно нічого вантажити чи прикріпляти до проєкту.
 */
object SoundSignals {

    val NAMES = listOf(
        "Три короткі гудки",
        "Один довгий гудок",
        "Висхідна трель",
        "Тривожні подвійні",
        "М'який сигнал «Готово»"
    )

    // Триплет: (тип тону, тривалість мс, пауза після мс)
    private fun pattern(index: Int): List<Triple<Int, Int, Int>> = when (index) {
        0 -> listOf(
            Triple(ToneGenerator.TONE_PROP_BEEP2, 150, 100),
            Triple(ToneGenerator.TONE_PROP_BEEP2, 150, 100),
            Triple(ToneGenerator.TONE_PROP_BEEP2, 150, 0)
        )
        1 -> listOf(
            Triple(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900, 0)
        )
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
        else -> listOf(
            Triple(ToneGenerator.TONE_PROP_ACK, 300, 60),
            Triple(ToneGenerator.TONE_PROP_ACK, 500, 0)
        )
    }

    fun play(signalIndex: Int) {
        val steps = pattern(signalIndex)
        val tg = try {
            ToneGenerator(AudioManager.STREAM_ALARM, 100)
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
