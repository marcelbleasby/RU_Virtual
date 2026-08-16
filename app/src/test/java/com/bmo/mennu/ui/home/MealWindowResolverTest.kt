package com.bmo.mennu.ui.home

import com.bmo.mennu.data.model.TipoRefeicaoResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MealWindowResolverTest {

    private fun tipo(nome: String, inicio: String, fim: String, ordem: Int = 0) =
        TipoRefeicaoResponse(id = ordem, nome = nome, unidadeId = 1, horarioInicio = inicio, horarioFim = fim, ordem = ordem)

    private val cafe = tipo("Café", "07:00:00", "09:00:00", ordem = 1)
    private val almoco = tipo("Almoço", "11:00:00", "14:00:00", ordem = 2)
    private val jantar = tipo("Jantar", "18:00:00", "21:00:00", ordem = 3)
    private val tipos = listOf(cafe, almoco, jantar)

    @Test
    fun `a time inside a window matches that meal type`() {
        val resolved = MealWindowResolver.resolveCurrentOrNext(nowMinutes = 12 * 60 + 30, tipos = tipos)
        assertEquals(almoco, resolved)
    }

    @Test
    fun `horario_inicio is inclusive`() {
        val resolved = MealWindowResolver.resolveCurrentOrNext(nowMinutes = 11 * 60, tipos = tipos)
        assertEquals(almoco, resolved)
    }

    @Test
    fun `horario_fim is inclusive`() {
        val resolved = MealWindowResolver.resolveCurrentOrNext(nowMinutes = 14 * 60, tipos = tipos)
        assertEquals(almoco, resolved)
    }

    @Test
    fun `no window matches falls back to the next meal starting later today`() {
        // 10:00 — after café (ends 09:00), before almoço (starts 11:00).
        val resolved = MealWindowResolver.resolveCurrentOrNext(nowMinutes = 10 * 60, tipos = tipos)
        assertEquals(almoco, resolved)
    }

    @Test
    fun `nothing left today returns null`() {
        // 22:00 — after jantar, nothing else starts today.
        val resolved = MealWindowResolver.resolveCurrentOrNext(nowMinutes = 22 * 60, tipos = tipos)
        assertNull(resolved)
    }

    @Test
    fun `empty list returns null`() {
        val resolved = MealWindowResolver.resolveCurrentOrNext(nowMinutes = 12 * 60, tipos = emptyList())
        assertNull(resolved)
    }
}
