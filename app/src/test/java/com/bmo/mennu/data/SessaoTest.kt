package com.bmo.mennu.data

import android.content.SharedPreferences
import com.bmo.mennu.data.model.Contexto
import com.bmo.mennu.data.model.LoginResponse
import com.bmo.mennu.data.model.UsuarioAtivoResponse
import com.bmo.mennu.data.remote.AuthInterceptor
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SessaoTest {
    private val unidadeA = Contexto(1, "Empresa A", 10, "Unidade A")
    private val unidadeB = Contexto(2, "Empresa B", 20, "Unidade B")

    @Test fun selecionaUnicoContextoMasExigeEscolhaEntreVarios() {
        assertEquals(unidadeA, escolherContexto(listOf(unidadeA), null))
        assertNull(escolherContexto(listOf(unidadeA, unidadeB), null))
        assertNull(escolherContexto(emptyList(), unidadeA))
    }

    @Test fun restauraSomenteParQueContinuaAutorizado() {
        assertEquals(unidadeB, escolherContexto(listOf(unidadeA, unidadeB), unidadeB))
        val transferida = Contexto(1, "Empresa A", 20, "Nova unidade")
        assertNull(escolherContexto(listOf(unidadeA, unidadeB), transferida))
    }

    @Test fun sessaoPersistidaPrecisaSerRevalidadaAoReabrir() {
        val preferencias = PreferenciasMemoria()
        val store = TokenStore(preferencias)
        store.salvar(Sessao("token", listOf(unidadeA), unidadeA, true))
        val restaurada = TokenStore(preferencias).sessao.value
        assertEquals("token", restaurada.token)
        assertEquals(unidadeA, restaurada.selecionado)
        assertFalse(restaurada.validada)
    }

    @Test fun selecaoRevogadaNaoContinuaNosHeaders() {
        val store = TokenStore(PreferenciasMemoria())
        store.salvar(Sessao("token", listOf(unidadeA, unidadeB), unidadeA, true))
        store.atualizarContextos(emptyList())
        assertNull(store.sessao.value.selecionado)
        assertEquals("token", store.getToken())
    }

    @Test fun logoutRemoveSessaoInclusiveAoReabrir() {
        val preferencias = PreferenciasMemoria()
        val store = TokenStore(preferencias)
        store.salvar(Sessao("token", listOf(unidadeA), unidadeA, true))
        store.clear()
        assertNull(TokenStore(preferencias).getToken())
    }

    private fun cliente(store: TokenStore, resposta: (Request) -> Unit): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(AuthInterceptor(store)).addInterceptor { chain ->
            resposta(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("{}".toResponseBody()).build()
        }.build()

    @Test fun requisicaoEnviaTokenEParConsistentes() {
        val store = TokenStore(PreferenciasMemoria())
        store.salvar(Sessao("token", listOf(unidadeA), unidadeA, true))
        cliente(store) { request ->
            assertEquals("Bearer token", request.header("Authorization"))
            assertEquals("1", request.header("Empresa-id-x"))
            assertEquals("10", request.header("Unidade-id-x"))
        }.newCall(Request.Builder().url("https://teste/api/cardapio/").build()).execute().close()
    }

    @Test fun bootstrapELogoutNaoEnviamSelecaoAntiga() {
        val store = TokenStore(PreferenciasMemoria())
        store.salvar(Sessao("token", listOf(unidadeA), unidadeA, true))
        for (path in listOf("login", "contextos", "logout")) {
            cliente(store) { request ->
                assertNull(request.header("Empresa-id-x"))
                assertNull(request.header("Unidade-id-x"))
                if (path == "login") assertNull(request.header("Authorization"))
                else assertEquals("Bearer token", request.header("Authorization"))
            }.newCall(Request.Builder().url("https://teste/api/auth/$path").build()).execute().close()
        }
    }

    @Test fun operacoesExigemSessaoRevalidadaEUnidade() {
        val store = TokenStore(PreferenciasMemoria())
        store.salvar(Sessao("token", listOf(unidadeA), unidadeA, false))
        try {
            cliente(store) { fail("Não deve consultar dados antes de validar sessão") }
                .newCall(Request.Builder().url("https://teste/api/cardapio/").build()).execute()
            fail("Era esperado bloqueio de contexto")
        } catch (_: IOException) { }
    }

    @Test fun respostaAtrasadaDeOutraSessaoERecusada() {
        val store = TokenStore(PreferenciasMemoria())
        store.salvar(Sessao("token", listOf(unidadeA, unidadeB), unidadeA, true))
        try {
            cliente(store) {
                store.salvar(store.sessao.value.copy(selecionado = unidadeB))
            }.newCall(Request.Builder().url("https://teste/api/cardapio/").build()).execute()
            fail("Não deve devolver resposta da seleção antiga")
        } catch (_: IOException) { }
    }

    @Test fun dtosMobileAceitamCategoriaNulaESessaoAtivaSemToken() {
        val gson = Gson()
        val json = """{"id":1,"email":"teste@teste.com","ativo":true,"categoria_usuario":null,
            "criado_em":"2026-09-30T00:00:00Z","atualizado_em":"2026-09-30T00:00:00Z",
            "contextos":[{"empresa_id":1,"unidade_id":10,"slugs":[]}]}"""
        val ativo = gson.fromJson(json, UsuarioAtivoResponse::class.java)
        assertNull(ativo.categoriaUsuario)
        assertEquals(10, ativo.contextos.single().unidadeId)
        val login = gson.fromJson(json.dropLast(1)+",\"token_access\":{\"token\":\"token\",\"expirado_em\":\"2026-10-01T00:00:00Z\"}}", LoginResponse::class.java)
        assertEquals("token", login.tokenAccess.token)
        assertFalse(gson.toJson(ativo).contains("token_access"))
    }
}

private class PreferenciasMemoria : SharedPreferences {
    private val valores = mutableMapOf<String, Any?>()
    override fun getAll(): MutableMap<String, *> = valores.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = valores[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = valores[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = valores[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = valores[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = valores[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = valores.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { }
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { }
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val novos = mutableMapOf<String, Any?>()
        private var limpar = false
        override fun putString(key: String?, value: String?) = apply { novos[key!!] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { novos[key!!] = values }
        override fun putInt(key: String?, value: Int) = apply { novos[key!!] = value }
        override fun putLong(key: String?, value: Long) = apply { novos[key!!] = value }
        override fun putFloat(key: String?, value: Float) = apply { novos[key!!] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { novos[key!!] = value }
        override fun remove(key: String?) = apply { novos[key!!] = null }
        override fun clear() = apply { limpar = true }
        override fun commit(): Boolean {
            if (limpar) valores.clear()
            novos.forEach { (chave, valor) -> if (valor == null) valores.remove(chave) else valores[chave] = valor }
            return true
        }
        override fun apply() { commit() }
    }
}
