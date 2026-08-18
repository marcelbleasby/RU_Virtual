package com.bmo.mennu.data

import com.bmo.mennu.data.model.CardapioResponse
import com.bmo.mennu.data.model.PratoResponse
import com.bmo.mennu.ui.cardapio.DietTag
import com.bmo.mennu.ui.cardapio.FoodCategory
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CardapioRepositoryMapperTest {

    private lateinit var originalDefaultTimeZone: TimeZone

    // Pino o timezone padrão da JVM em UTC pra formatação de data em buildWeekDays
    // ficar determinística, independente do fuso da máquina que roda o teste.
    @Before
    fun pinUtcTimeZone() {
        originalDefaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone)
    }

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val weekStart = isoFormat.parse("2026-02-09")!! // segunda-feira

    @Test
    fun `always returns 7 days even with no cardapios`() {
        val days = buildWeekDays(weekStart, emptyList())
        assertEquals(7, days.size)
        assertTrue(days.all { it.meals.isEmpty() })
    }

    @Test
    fun `maps a cardapio into the matching day and meal type`() {
        val cardapio = CardapioResponse(
            id = 1,
            dataRefeicao = "2026-02-09",
            tipoRefeicaoId = 2,
            tipoRefeicaoNome = "Almoço",
            tipoRefeicaoOrdem = 2,
            pratos = listOf(PratoResponse(id = 10, tipoPrato = "principal", nome = "Frango Grelhado"))
        )

        val days = buildWeekDays(weekStart, listOf(cardapio))
        val monday = days[0]

        assertEquals(1, monday.meals.size)
        val (mealType, dishes) = monday.meals.entries.single()
        assertEquals("Almoço", mealType.nome)
        assertEquals(2, mealType.ordem)
        assertEquals(1, dishes.size)
        assertEquals("Frango Grelhado", dishes[0].name)
        assertEquals(FoodCategory.PRINCIPAL, dishes[0].category)
        assertTrue(dishes[0].tags.isEmpty())
    }

    @Test
    fun `two meal types on the same day both appear`() {
        val almoco = CardapioResponse(
            id = 1, dataRefeicao = "2026-02-10", tipoRefeicaoId = 2,
            tipoRefeicaoNome = "Almoço", tipoRefeicaoOrdem = 2,
            pratos = listOf(PratoResponse(id = 10, tipoPrato = "guarnicao", nome = "Arroz Branco")),
        )
        val jantar = CardapioResponse(
            id = 2, dataRefeicao = "2026-02-10", tipoRefeicaoId = 4,
            tipoRefeicaoNome = "Jantar", tipoRefeicaoOrdem = 4,
            pratos = listOf(PratoResponse(id = 11, tipoPrato = "sobremesa", nome = "Fruta")),
        )

        val days = buildWeekDays(weekStart, listOf(almoco, jantar))
        val tuesday = days[1]

        assertEquals(2, tuesday.meals.size)
        assertEquals(setOf("Almoço", "Jantar"), tuesday.meals.keys.map { it.nome }.toSet())
    }

    @Test
    fun `maps restricoes slugs to DietTag`() {
        val cardapio = CardapioResponse(
            id = 1, dataRefeicao = "2026-02-09", tipoRefeicaoId = 2,
            tipoRefeicaoNome = "Almoço", tipoRefeicaoOrdem = 2,
            pratos = listOf(
                PratoResponse(
                    id = 10, tipoPrato = "guarnicao", nome = "Arroz Branco",
                    restricoes = listOf("vegetariano", "vegano", "sem_gluten", "sem_lactose")
                )
            )
        )

        val dish = buildWeekDays(weekStart, listOf(cardapio))[0].meals.values.single().single()

        assertEquals(
            setOf(DietTag.VEGETARIANO, DietTag.VEGANO, DietTag.SEM_GLUTEN, DietTag.SEM_LACTOSE),
            dish.tags.toSet()
        )
    }

    @Test
    fun `unknown restricao slug is ignored instead of crashing`() {
        val cardapio = CardapioResponse(
            id = 1, dataRefeicao = "2026-02-09", tipoRefeicaoId = 2,
            tipoRefeicaoNome = "Almoço", tipoRefeicaoOrdem = 2,
            pratos = listOf(
                PratoResponse(
                    id = 10, tipoPrato = "principal", nome = "Prato X",
                    restricoes = listOf("organico", "vegetariano")
                )
            )
        )

        val dish = buildWeekDays(weekStart, listOf(cardapio))[0].meals.values.single().single()

        assertEquals(listOf(DietTag.VEGETARIANO), dish.tags)
    }

    @Test
    fun `does not crash when the backend omits pratos or restricoes keys entirely`() {
        // Gson simples não respeita o default do Kotlin (= emptyList()) quando a chave
        // some do JSON — o campo vira null em runtime mesmo com tipo não-nulo declarado.
        // Reproduz exatamente o payload que causou "Attempt to invoke interface method
        // 'java.util.Iterator java.lang.Iterable.iterator()' on a null object reference".
        val json = """
            {
                "id": 1,
                "data_refeicao": "2026-02-09",
                "tipo_refeicao": 2,
                "tipo_refeicao_nome": "Almoço",
                "tipo_refeicao_ordem": 2
            }
        """.trimIndent()
        val cardapio = Gson().fromJson(json, CardapioResponse::class.java)

        val days = buildWeekDays(weekStart, listOf(cardapio))

        assertEquals(1, days[0].meals.size)
        assertTrue(days[0].meals.values.single().isEmpty())
    }

    @Test
    fun `does not crash when a prato omits restricoes entirely`() {
        val json = """{"id": 10, "tipo_prato": "principal", "nome": "Frango"}"""
        val prato = Gson().fromJson(json, PratoResponse::class.java)
        val cardapio = CardapioResponse(
            id = 1, dataRefeicao = "2026-02-09", tipoRefeicaoId = 2,
            tipoRefeicaoNome = "Almoço", tipoRefeicaoOrdem = 2,
            pratos = listOf(prato),
        )

        val dish = buildWeekDays(weekStart, listOf(cardapio))[0].meals.values.single().single()

        assertTrue(dish.tags.isEmpty())
    }

    @Test
    fun `cardapio dated outside the week is not placed on any day`() {
        val forewarnedNextWeek = CardapioResponse(
            id = 1, dataRefeicao = "2026-02-16", tipoRefeicaoId = 2,
            tipoRefeicaoNome = "Almoço", tipoRefeicaoOrdem = 2,
        )

        val days = buildWeekDays(weekStart, listOf(forewarnedNextWeek))
        assertTrue(days.all { it.meals.isEmpty() })
    }
}
