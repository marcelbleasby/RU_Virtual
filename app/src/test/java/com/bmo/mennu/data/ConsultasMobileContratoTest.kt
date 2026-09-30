package com.bmo.mennu.data

import com.bmo.mennu.data.model.CardapioResponse
import com.bmo.mennu.data.model.PaginatedResponse
import com.bmo.mennu.data.model.RefeicaoServida
import com.bmo.mennu.data.model.TipoRefeicaoResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

// Fixtures geradas pelos endpoints reais da API mobile, usando SQLite de teste.
class ConsultasMobileContratoTest {
    private val gson = Gson()
    private fun fixture(nome: String) = requireNotNull(javaClass.getResource("/mobile/$nome.json")).readText()

    @Test fun cardapioMobilePreservaCamposConsumidosPeloApp() {
        val tipo = object : TypeToken<PaginatedResponse<CardapioResponse>>() {}.type
        val resposta: PaginatedResponse<CardapioResponse> = gson.fromJson(fixture("cardapio"), tipo)
        val cardapio = resposta.results.single()
        assertEquals("Ok", resposta.message)
        assertEquals(1, resposta.metadados.totalResults)
        assertEquals("2026-09-30", cardapio.dataRefeicao)
        assertEquals("Jantar", cardapio.tipoRefeicaoNome)
        assertEquals(2, cardapio.tipoRefeicaoOrdem)
        assertTrue(cardapio.tipoRefeicaoId > 0)
        assertEquals("Arroz", cardapio.pratos.single().nome)
        assertEquals(listOf("vegano"), cardapio.pratos.single().restricoes)
    }

    @Test fun horarioNoturnoMobilePreservaJanelaAtravessandoMeiaNoite() {
        val tipo = object : TypeToken<PaginatedResponse<TipoRefeicaoResponse>>() {}.type
        val resposta: PaginatedResponse<TipoRefeicaoResponse> = gson.fromJson(fixture("horarios"), tipo)
        val horario = resposta.results.single()
        assertEquals("23:00:00", horario.horarioInicio)
        assertEquals("01:00:00", horario.horarioFim)
        assertEquals("Jantar", horario.nome)
        assertTrue(horario.unidadeId > 0)
    }

    @Test fun historicoMobilePreservaMatriculaDoVinculoEEnvelope() {
        val tipo = object : TypeToken<PaginatedResponse<RefeicaoServida>>() {}.type
        val resposta: PaginatedResponse<RefeicaoServida> = gson.fromJson(fixture("historico"), tipo)
        val refeicao = resposta.results.single()
        assertEquals("A-100", refeicao.usuarioMatricula)
        assertEquals("Unidade A", refeicao.unidadeNome)
        assertEquals("Comensal", refeicao.usuarioNome)
        assertEquals("2026-09-30T12:00:00Z", refeicao.dataHora)
        assertFalse(refeicao.manual)
        assertNull(refeicao.motivo)
        assertEquals(1, resposta.metadados.page)
        assertEquals(1, resposta.metadados.totalPages)
        assertNull(resposta.metadados.next)
        assertTrue(refeicao.cardapioId > 0)
        assertTrue(refeicao.usuarioId > 0)
    }
}
