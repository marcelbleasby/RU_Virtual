package com.bmo.mennu.ui.cardapio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class CardapioUiStateTest {

    private val cafe = MealType(id = 1, nome = "Café", ordem = 1)
    private val almoco = MealType(id = 2, nome = "Almoço", ordem = 2)
    private val jantar = MealType(id = 4, nome = "Jantar", ordem = 4)

    private val arroz = Dish("arroz", "Arroz Branco", FoodCategory.GUARNICAO, emptyList())
    private val frango = Dish("frango", "Frango Grelhado", FoodCategory.PRINCIPAL, emptyList())
    private val paoDeQueijo = Dish("pao-queijo", "Pão de Queijo", FoodCategory.PRINCIPAL, emptyList())

    private val segunda = DayMenu(
        date = Date(0),
        dayLabel = "Segunda-feira",
        meals = mapOf(
            cafe to listOf(paoDeQueijo),
            almoco to listOf(arroz, frango)
        )
    )

    // Domingo real: RU só serve almoço, sem café nem jantar.
    private val domingo = DayMenu(
        date = Date(0),
        dayLabel = "Domingo",
        meals = mapOf(almoco to listOf(arroz))
    )

    private fun state(
        weekDays: List<DayMenu>,
        selectedDayIndex: Int = 0,
        selectedMealType: MealType? = null
    ) = CardapioUiState(
        isLoading = false,
        weekDays = weekDays,
        selectedDayIndex = selectedDayIndex,
        selectedMealType = selectedMealType
    )

    @Test
    fun `selectedDay reads the day at selectedDayIndex`() {
        val uiState = state(listOf(segunda, domingo), selectedDayIndex = 1)
        assertEquals(domingo, uiState.selectedDay)
    }

    @Test
    fun `selectedDay is null for an out-of-range index`() {
        val uiState = state(listOf(segunda), selectedDayIndex = 5)
        assertEquals(null, uiState.selectedDay)
    }

    @Test
    fun `availableMealTypes unions meal types across the week, sorted by ordem`() {
        val uiState = state(listOf(segunda, domingo))
        assertEquals(listOf(cafe, almoco), uiState.availableMealTypes)
    }

    @Test
    fun `Todos filter shows every meal type present on the selected day`() {
        val uiState = state(listOf(segunda, domingo), selectedDayIndex = 0, selectedMealType = null)
        assertEquals(listOf(cafe, almoco), uiState.visibleMealTypes)
    }

    @Test
    fun `selecting a meal type restricts visibleMealTypes to it`() {
        val uiState = state(listOf(segunda, domingo), selectedDayIndex = 0, selectedMealType = almoco)
        assertEquals(listOf(almoco), uiState.visibleMealTypes)
    }

    @Test
    fun `selecting a meal type not served that day yields an empty state`() {
        val uiState = state(listOf(segunda, domingo), selectedDayIndex = 1, selectedMealType = jantar)
        assertTrue(uiState.visibleMealTypes.isEmpty())
    }

    @Test
    fun `dishes group by category within a meal type`() {
        val uiState = state(listOf(segunda), selectedDayIndex = 0, selectedMealType = almoco)
        val dishes = uiState.selectedDay?.meals?.get(almoco).orEmpty()
        val byCategory = dishes.groupBy { it.category }

        assertEquals(listOf(frango), byCategory[FoodCategory.PRINCIPAL])
        assertEquals(listOf(arroz), byCategory[FoodCategory.GUARNICAO])
    }
}
