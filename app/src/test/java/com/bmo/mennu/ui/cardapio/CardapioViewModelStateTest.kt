package com.bmo.mennu.ui.cardapio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class CardapioViewModelStateTest {

    private val almoco = MealType(id = 2, nome = "Almoço", ordem = 2)
    private val jantar = MealType(id = 3, nome = "Jantar", ordem = 3)
    private val arroz = Dish("arroz", "Arroz Branco", FoodCategory.GUARNICAO, emptyList())

    private val novaSemana = listOf(
        DayMenu(date = Date(0), dayLabel = "Segunda-feira", meals = mapOf(almoco to listOf(arroz)))
    )

    @Test
    fun `refresh (resetSelection=false) preserves the user's current day and meal-type selection`() {
        val estadoAnterior = CardapioUiState(
            isLoading = false,
            weekDays = emptyList(),
            selectedDayIndex = 3,
            selectedMealType = jantar
        )

        val novoEstado = estadoAnterior.withFetchedWeek(novaSemana, resetSelection = false)

        assertEquals(3, novoEstado.selectedDayIndex)
        assertEquals(jantar, novoEstado.selectedMealType)
        assertEquals(novaSemana, novoEstado.weekDays)
        assertEquals(false, novoEstado.isLoading)
    }

    @Test
    fun `week navigation (resetSelection=true) resets the selected day but keeps the meal-type filter`() {
        val estadoAnterior = CardapioUiState(
            isLoading = true,
            weekDays = emptyList(),
            selectedDayIndex = 3,
            selectedMealType = jantar
        )

        val novoEstado = estadoAnterior.withFetchedWeek(novaSemana, resetSelection = true)

        assertEquals(0, novoEstado.selectedDayIndex)
        assertEquals(jantar, novoEstado.selectedMealType)
    }

    @Test
    fun `selectedMealType null is preserved through resetSelection=true`() {
        val estadoAnterior = CardapioUiState(selectedDayIndex = 5, selectedMealType = null)

        val novoEstado = estadoAnterior.withFetchedWeek(novaSemana, resetSelection = true)

        assertNull(novoEstado.selectedMealType)
        assertEquals(0, novoEstado.selectedDayIndex)
    }
}
