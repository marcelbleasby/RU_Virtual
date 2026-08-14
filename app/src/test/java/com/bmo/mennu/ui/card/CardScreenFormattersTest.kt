package com.bmo.mennu.ui.card

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CardScreenFormattersTest {

    private lateinit var originalDefaultTimeZone: TimeZone

    // Pino o timezone padrão da JVM em UTC pra hora/dia relativo ficarem
    // determinísticos, independente do fuso da máquina que roda o teste.
    @Before
    fun pinUtcTimeZone() {
        originalDefaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone)
    }

    private fun isoAt(hourOfDay: Int, daysAgo: Int = 0): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, 30)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(cal.time)
    }

    // maskMatricula

    @Test
    fun maskMatricula_null_returnsFullyMaskedPlaceholder() {
        assertEquals("•••• •••• •••• ••••", maskMatricula(null))
    }

    @Test
    fun maskMatricula_blank_returnsFullyMaskedPlaceholder() {
        assertEquals("•••• •••• •••• ••••", maskMatricula("   "))
    }

    @Test
    fun maskMatricula_shorterThanFour_revealsAllDigits() {
        assertEquals("123", maskMatricula("123"))
    }

    @Test
    fun maskMatricula_exactlyFour_revealsAllFourDigits() {
        assertEquals("4821", maskMatricula("4821"))
    }

    @Test
    fun maskMatricula_notMultipleOfFour_lastGroupIsTrueLastFourChars() {
        assertEquals("••• 4567", maskMatricula("1234567"))
    }

    @Test
    fun maskMatricula_sixteenChars_masksAllButLastFour() {
        assertEquals("•••• •••• •••• 4821", maskMatricula("1234567890004821"))
    }

    // inferMealTypeFromTime

    @Test
    fun inferMealType_beforeTen_isCafeDaManha() {
        assertEquals("Café da manhã", inferMealTypeFromTime(isoAt(hourOfDay = 9)))
    }

    @Test
    fun inferMealType_atTenBoundary_isAlmoco() {
        assertEquals("Almoço", inferMealTypeFromTime(isoAt(hourOfDay = 10)))
    }

    @Test
    fun inferMealType_beforeSixteen_isAlmoco() {
        assertEquals("Almoço", inferMealTypeFromTime(isoAt(hourOfDay = 15)))
    }

    @Test
    fun inferMealType_atSixteenBoundary_isJantar() {
        assertEquals("Jantar", inferMealTypeFromTime(isoAt(hourOfDay = 16)))
    }

    @Test
    fun inferMealType_nullInput_fallsBackToGenericLabel() {
        assertEquals("Refeição", inferMealTypeFromTime(null))
    }

    // formatRelativeDay

    @Test
    fun formatRelativeDay_today_returnsHoje() {
        assertEquals("Hoje", formatRelativeDay(isoAt(hourOfDay = 12)))
    }

    @Test
    fun formatRelativeDay_yesterday_returnsOntem() {
        assertEquals("Ontem", formatRelativeDay(isoAt(hourOfDay = 12, daysAgo = 1)))
    }

    @Test
    fun formatRelativeDay_older_returnsDayMonth() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.add(Calendar.DAY_OF_YEAR, -8)
        val expected = SimpleDateFormat("dd/MM", Locale.getDefault()).format(cal.time)
        assertEquals(expected, formatRelativeDay(isoAt(hourOfDay = 12, daysAgo = 8)))
    }

    @Test
    fun formatRelativeDay_nullInput_returnsUnavailableMessage() {
        assertEquals("Data indisponível", formatRelativeDay(null))
    }
}
