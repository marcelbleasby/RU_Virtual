package com.bmo.mennu.util

import java.util.Calendar
import java.util.Date

fun mondayOfCurrentWeek(): Date {
    val calendar = Calendar.getInstance()
    // DAY_OF_WEEK: domingo=1 ... sábado=7. Normaliza pra dias desde segunda (0..6),
    // independente do "primeiro dia da semana" do Locale.
    val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}

fun shiftWeek(date: Date, days: Int): Date {
    val calendar = Calendar.getInstance().apply { time = date }
    calendar.add(Calendar.DAY_OF_MONTH, days)
    return calendar.time
}
