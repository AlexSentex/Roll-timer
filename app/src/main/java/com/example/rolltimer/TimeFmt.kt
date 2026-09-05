package com.example.rolltimer

/** Спільне форматування часу для дешборда й екрана деталей таймера. */
object TimeFmt {
    fun format(sec: Double): String {
        val s = Math.round(sec.coerceAtLeast(0.0))
        val h = s / 3600
        val m = (s % 3600) / 60
        val ss = s % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, ss) else "%02d:%02d".format(m, ss)
    }
}
