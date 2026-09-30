package com.bmo.mennu.data

import com.bmo.mennu.data.model.Contexto
import com.bmo.mennu.data.model.ContextosResponse
import com.bmo.mennu.data.model.LoginRequest
import com.bmo.mennu.data.model.LoginResponse
import com.bmo.mennu.data.model.UsuarioAtivoResponse
import com.bmo.mennu.data.remote.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response

class AuthRepositoryTest {
    private val a = Contexto(1, "Empresa A", 10, "Unidade A")
    private val b = Contexto(2, "Empresa B", 20, "Unidade B")
    private val api = mock(ApiService::class.java)
    private val usuarios = mock(UserRepository::class.java)
    private val store = TokenStore(PreferenciasMemoria())
    private val repository = AuthRepository(api, store, usuarios)
    private val gson = Gson()

    private fun json(empresa: Int, contextos: List<Contexto>) = """{
        "id":1,"email":"teste@teste.com","ativo":true,"empresa_id":$empresa,
        "criado_em":"2026-09-30T00:00:00Z","atualizado_em":"2026-09-30T00:00:00Z",
        "numero_cartao":"CARTAO","tenant_salt":"SALT","contextos":${gson.toJson(contextos)},
        "token_access":{"token":"token","expirado_em":"2026-10-01T00:00:00Z"}}
    """
    private fun perfil(empresa: Int, contextos: List<Contexto>) =
        gson.fromJson(json(empresa, contextos), UsuarioAtivoResponse::class.java)

    @Test fun loginDescobreDuasEmpresasAntesDeSelecionar(): Unit = runBlocking {
        `when`(api.login(LoginRequest("teste@teste.com", "senha")))
            .thenReturn(Response.success(gson.fromJson(json(1, listOf(a)), LoginResponse::class.java)))
        `when`(api.getContextos()).thenReturn(Response.success(ContextosResponse(listOf(a, b))))
        `when`(api.getUsuarioAtivo()).thenReturn(Response.success(perfil(1, listOf(a))))

        val user = repository.login("teste@teste.com", "senha").getOrThrow()

        assertEquals(listOf(a, b), store.sessao.value.contextos)
        assertNull(store.sessao.value.selecionado)
        assertNull(user.empresaId)
        assertNull(user.vCardId)
        assertTrue(store.sessao.value.validada)
        val ordem = inOrder(api)
        ordem.verify(api).login(LoginRequest("teste@teste.com", "senha"))
        ordem.verify(api).getContextos()
        ordem.verify(api).getUsuarioAtivo()
    }

    @Test fun refreshPreservaEmpresaBMesmoComPerfilContextual(): Unit = runBlocking {
        store.salvar(Sessao("token", listOf(a, b), b, false))
        `when`(api.getContextos()).thenReturn(Response.success(ContextosResponse(listOf(a, b))))
        `when`(api.getUsuarioAtivo()).thenReturn(Response.success(perfil(2, listOf(b))))

        val user = repository.refreshUsuarioAtivo().getOrThrow()

        assertEquals(listOf(a, b), store.sessao.value.contextos)
        assertEquals(b, store.sessao.value.selecionado)
        assertEquals(2, user.empresaId)
        assertTrue(store.sessao.value.validada)
    }

    @Test fun trocaParaBEAtualizacaoMantemAsDuasEmpresas(): Unit = runBlocking {
        store.salvar(Sessao("token", listOf(a, b), a, true))
        `when`(api.getContextos()).thenReturn(Response.success(ContextosResponse(listOf(a, b))))
        `when`(api.getUsuarioAtivo()).thenReturn(Response.success(perfil(2, listOf(b))))

        repository.selecionarContexto(b).getOrThrow()
        repository.refreshUsuarioAtivo().getOrThrow()

        assertEquals(b, store.sessao.value.selecionado)
        assertEquals(listOf(a, b), store.sessao.value.contextos)
    }

    @Test fun vinculoRevogadoDescobertoAntesDoPerfilSelecionaUnicoRestante(): Unit = runBlocking {
        store.salvar(Sessao("token", listOf(a, b), a, true))
        `when`(api.getContextos()).thenReturn(Response.success(ContextosResponse(listOf(b))))
        `when`(api.getUsuarioAtivo()).thenReturn(Response.success(perfil(2, listOf(b))))

        repository.refreshUsuarioAtivo().getOrThrow()

        assertEquals(b, store.sessao.value.selecionado)
        assertEquals(listOf(b), store.sessao.value.contextos)
        assertEquals("token", store.getToken())
        verify(usuarios).clearUser()
    }

    @Test fun revogacaoDurantePerfilReconsultaListaGlobal(): Unit = runBlocking {
        store.salvar(Sessao("token", listOf(a, b), a, true))
        `when`(api.getContextos()).thenReturn(
            Response.success(ContextosResponse(listOf(a, b))),
            Response.success(ContextosResponse(listOf(b))),
        )
        `when`(api.getUsuarioAtivo()).thenReturn(
            Response.error(403, "{}".toResponseBody()), Response.success(perfil(2, listOf(b))),
        )

        repository.refreshUsuarioAtivo().getOrThrow()

        assertEquals(b, store.sessao.value.selecionado)
        assertEquals(listOf(b), store.sessao.value.contextos)
        assertEquals("token", store.getToken())
        verify(api, times(2)).getContextos()
    }

    @Test fun descoberta401LimpaSessaoSemConsultarPerfil(): Unit = runBlocking {
        store.salvar(Sessao("token", listOf(a), a, true))
        `when`(api.getContextos()).thenReturn(Response.error(401, "{}".toResponseBody()))

        assertTrue(repository.refreshUsuarioAtivo().isFailure)

        assertNull(store.getToken())
        verify(usuarios).clearUser()
        verify(api, never()).getUsuarioAtivo()
    }

    @Test fun falhaNaDescobertaNaoUsaListaParcialComoFallback(): Unit = runBlocking {
        store.salvar(Sessao("token", listOf(a, b), b, false))
        `when`(api.getContextos()).thenReturn(Response.error(503, "{}".toResponseBody()))

        assertTrue(repository.refreshUsuarioAtivo().isFailure)

        assertEquals(listOf(a, b), store.sessao.value.contextos)
        assertFalse(store.sessao.value.validada)
        verify(api, never()).getUsuarioAtivo()
    }
}
