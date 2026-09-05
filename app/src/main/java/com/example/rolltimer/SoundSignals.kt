package com.example.rolltimer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * 15 звукових сигналів. Використовуються ЛИШЕ прості одиночні тони
 * (DTMF-ноти телефонної клавіатури + прості "beep"-тони) — жодних
 * системних багатотональних сигналів (як CDMA-алерти), бо вони звучать
 * інакше, ніж написано в назві. Кожен сигнал триває 1–4 секунди.
 * Гучність береться з медіа-потоку (як музика), а не з будильника —
 * тому не дублюється одночасно в динамік і навушники.
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

    // Триплет: (тип тону, тривалість мс, пауза після мс). Сума всіх — 1-4 сек.
    private fun pattern(index: Int): List<Triple<Int, Int, Int>> = when (index) {
        0 -> listOf(
            Triple(ToneGenerator.TONE_PROP_BEEP2, 250, 200),
            Triple(ToneGenerator.TONE_PROP_BEEP2, 250, 200),
            Triple(ToneGenerator.TONE_PROP_BEEP2, 250, 0)
        )
        1 -> listOf(
            Triple(ToneGenerator.TONE_PROP_BEEP2, 1500, 0)
        )
        2 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_1, 200, 60),
            Triple(ToneGenerator.TONE_DTMF_3, 200, 60),
            Triple(ToneGenerator.TONE_DTMF_5, 200, 60),
            Triple(ToneGenerator.TONE_DTMF_7, 250, 0)
        )
        3 -> listOf(
            Triple(ToneGenerator.TONE_PROP_BEEP, 150, 80),
            Triple(ToneGenerator.TONE_PROP_BEEP, 150, 300),
            Triple(ToneGenerator.TONE_PROP_BEEP, 150, 80),
            Triple(ToneGenerator.TONE_PROP_BEEP, 150, 0)
        )
        4 -> listOf(
            Triple(ToneGenerator.TONE_PROP_ACK, 400, 120),
            Triple(ToneGenerator.TONE_PROP_ACK, 500, 0)
        )
        5 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_9, 200, 60),
            Triple(ToneGenerator.TONE_DTMF_7, 200, 60),
            Triple(ToneGenerator.TONE_DTMF_5, 200, 60),
            Triple(ToneGenerator.TONE_DTMF_3, 250, 0)
        )
        6 -> listOf(
            Triple(ToneGenerator.TONE_PROP_BEEP2, 900, 300),
            Triple(ToneGenerator.TONE_PROP_BEEP2, 900, 0)
        )
        7 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_0, 150, 80),
            Triple(ToneGenerator.TONE_DTMF_0, 150, 80),
            Triple(ToneGenerator.TONE_DTMF_0, 150, 80),
            Triple(ToneGenerator.TONE_DTMF_0, 150, 80),
            Triple(ToneGenerator.TONE_DTMF_0, 150, 0)
        )
        8 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_3, 350, 150),
            Triple(ToneGenerator.TONE_DTMF_6, 350, 150),
            Triple(ToneGenerator.TONE_DTMF_3, 350, 0)
        )
        9 -> listOf(
            Triple(ToneGenerator.TONE_PROP_BEEP, 100, 80),
            Triple(ToneGenerator.TONE_PROP_BEEP, 100, 80),
            Triple(ToneGenerator.TONE_PROP_BEEP, 100, 200),
            Triple(ToneGenerator.TONE_PROP_BEEP, 280, 80),
            Triple(ToneGenerator.TONE_PROP_BEEP, 280, 80),
            Triple(ToneGenerator.TONE_PROP_BEEP, 280, 200),
            Triple(ToneGenerator.TONE_PROP_BEEP, 100, 80),
            Triple(ToneGenerator.TONE_PROP_BEEP, 100, 80),
            Triple(ToneGenerator.TONE_PROP_BEEP, 100, 0)
        )
        10 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_0, 150, 50),
            Triple(ToneGenerator.TONE_DTMF_2, 150, 50),
            Triple(ToneGenerator.TONE_DTMF_4, 150, 50),
            Triple(ToneGenerator.TONE_DTMF_6, 150, 50),
            Triple(ToneGenerator.TONE_DTMF_8, 180, 0)
        )
        11 -> listOf(
            Triple(ToneGenerator.TONE_DTMF_8, 150, 50),
            Triple(ToneGenerator.TONE_DTMF_6, 150, 50),
            Triple(ToneGenerator.TONE_DTMF_4, 150, 50),
            Triple(ToneGenerator.TONE_DTMF_2, 150, 50),
            Triple(ToneGenerator.TONE_DTMF_0, 180, 0)
        )
        12 -> listOf(
            Triple(ToneGenerator.TONE_PROP_BEEP, 150, 100),
            Triple(ToneGenerator.TONE_PROP_BEEP, 150, 350),
            Triple(ToneGenerator.TONE_PROP_BEEP, 150, 100),
            Triple(ToneGenerator.TONE_PROP_BEEP, 150, 0)
        )
        13 -> listOf(
            Triple(ToneGenerator.TONE_PROP_ACK, 450, 150),
            Triple(ToneGenerator.TONE_DTMF_5, 450, 0)
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
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val volumePct = ((cur * 100) / max).coerceIn(0, 100)

        val tg = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, volumePct)
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
