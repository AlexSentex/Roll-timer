package com.example.rolltimer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Спрацьовує навіть якщо застосунок закритий чи екран заблокований
 * (якщо дозволено "Точні будильники" в системі). Грає сигнал, показує
 * сповіщення (тап по ньому відкриває дешборд) і сам планує наступний
 * цикл — уже на повний час рулону.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val name = intent.getStringExtra(EXTRA_NAME) ?: "Таймер"
        val speed = intent.getDoubleExtra(EXTRA_SPEED, 0.0)
        val totalLength = intent.getDoubleExtra(EXTRA_TOTAL_LENGTH, 0.0)
        val signalIndex = intent.getIntExtra(EXTRA_SIGNAL, 0)
        val cycle = intent.getIntExtra(EXTRA_CYCLE, 1)

        SoundSignals.play(context, signalIndex)
        showNotification(context, id, name, cycle)

        val fullTimeMs = if (speed > 0) (totalLength / speed * 1000).toLong() else 0L
        if (fullTimeMs > 0) {
            val nextTrigger = System.currentTimeMillis() + fullTimeMs
            scheduleAlarm(context, id, name, speed, totalLength, signalIndex, cycle + 1, nextTrigger)
            TimerStore.updateOnFire(context, id, cycle + 1, nextTrigger)
        } else {
            TimerStore.markStopped(context, id)
        }
    }

    private fun showNotification(context: Context, id: String, name: String, cycle: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Сигнали таймера рулонів",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPI = PendingIntent.getActivity(
            context, id.hashCode(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$name: рулон закінчився")
            .setContentText("Час вийшов — час міняти рулон (цикл $cycle).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPI)
            .build()
        nm.notify(id.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "roll_timer_alerts"
        const val EXTRA_ID = "id"
        const val EXTRA_NAME = "name"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_TOTAL_LENGTH = "total_length"
        const val EXTRA_SIGNAL = "signal"
        const val EXTRA_CYCLE = "cycle"

        fun scheduleAlarm(
            context: Context,
            id: String,
            name: String,
            speed: Double,
            totalLength: Double,
            signalIndex: Int,
            cycle: Int,
            triggerAtMillis: Long
        ) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_SPEED, speed)
                putExtra(EXTRA_TOTAL_LENGTH, totalLength)
                putExtra(EXTRA_SIGNAL, signalIndex)
                putExtra(EXTRA_CYCLE, cycle)
            }
            val pi = PendingIntent.getBroadcast(
                context, id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        }

        fun cancelAlarm(context: Context, id: String) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)
        }
    }
}
