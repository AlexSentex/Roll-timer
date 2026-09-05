package com.example.rolltimer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Дані одного таймера. Зберігаються в SharedPreferences у форматі JSON,
 * щоб і дешборд, і екран деталей, і фоновий AlarmReceiver бачили той самий стан.
 */
data class TimerData(
    var id: String,
    var name: String,
    var speed: Double,         // швидкість, м/с (0 якщо ще не визначено)
    var totalLength: Double,   // довжина рулону, м
    var alreadyPassed: Double, // скільки вже проїхало (для 1-го циклу), м
    var signalIndex: Int,
    var cycle: Int,
    var endTimestamp: Long,    // час закінчення поточного циклу (мс, epoch); 0 якщо не запущено
    var running: Boolean
)

object TimerStore {
    private const val PREFS = "roll_timer_prefs"
    private const val KEY_TIMERS = "timers_json"

    fun loadAll(context: Context): MutableList<TimerData> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TIMERS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val result = mutableListOf<TimerData>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                TimerData(
                    id = o.getString("id"),
                    name = o.optString("name", "Таймер"),
                    speed = o.optDouble("speed", 0.0),
                    totalLength = o.optDouble("totalLength", 0.0),
                    alreadyPassed = o.optDouble("alreadyPassed", 0.0),
                    signalIndex = o.optInt("signalIndex", 0),
                    cycle = o.optInt("cycle", 1),
                    endTimestamp = o.optLong("endTimestamp", 0L),
                    running = o.optBoolean("running", false)
                )
            )
        }
        return result
    }

    fun saveAll(context: Context, timers: List<TimerData>) {
        val arr = JSONArray()
        for (t in timers) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("name", t.name)
            o.put("speed", t.speed)
            o.put("totalLength", t.totalLength)
            o.put("alreadyPassed", t.alreadyPassed)
            o.put("signalIndex", t.signalIndex)
            o.put("cycle", t.cycle)
            o.put("endTimestamp", t.endTimestamp)
            o.put("running", t.running)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TIMERS, arr.toString()).apply()
    }

    /** Викликається з AlarmReceiver, коли сигнал спрацював у фоні. */
    fun updateOnFire(context: Context, id: String, newCycle: Int, newEndTimestamp: Long) {
        val timers = loadAll(context)
        val t = timers.find { it.id == id } ?: return
        t.cycle = newCycle
        t.endTimestamp = newEndTimestamp
        t.running = true
        saveAll(context, timers)
    }

    fun markStopped(context: Context, id: String) {
        val timers = loadAll(context)
        val t = timers.find { it.id == id } ?: return
        t.running = false
        t.endTimestamp = 0
        saveAll(context, timers)
    }
}
