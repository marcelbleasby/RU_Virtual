package com.bmo.mennu.data

import android.content.SharedPreferences
import com.bmo.mennu.data.model.Contexto
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class Sessao(
    val token: String? = null,
    val contextos: List<Contexto> = emptyList(),
    val selecionado: Contexto? = null,
    val validada: Boolean = false
)

internal fun escolherContexto(contextos: List<Contexto>, anterior: Contexto?): Contexto? =
    contextos.firstOrNull { it.mesmoPar(anterior) } ?: contextos.singleOrNull()

@Singleton
class TokenStore @Inject constructor(private val sharedPreferences: SharedPreferences) {
    private val gson = Gson()
    private val _sessao = MutableStateFlow(restaurar())
    val sessao = _sessao.asStateFlow()

    private fun restaurar(): Sessao = try {
        val dados = sharedPreferences.getString("sessao_mobile", null)
        if (dados != null) gson.fromJson(dados, Sessao::class.java).copy(validada = false)
        else Sessao(token = sharedPreferences.getString("auth_token", null))
    } catch (_: Exception) { Sessao() }

    @Synchronized
    fun salvar(sessao: Sessao) {
        // Um único registro: nunca persistir token/empresa/unidade de sessões diferentes.
        check(sharedPreferences.edit().putString("sessao_mobile", gson.toJson(sessao))
            .remove("auth_token").remove("empresa_id").commit())
        _sessao.value = sessao
    }

    fun atualizarContextos(contextos: List<Contexto>, validada: Boolean = true) {
        val atual = _sessao.value
        salvar(atual.copy(contextos = contextos,
            selecionado = escolherContexto(contextos, atual.selecionado), validada = validada))
    }

    fun getToken(): String? = _sessao.value.token
    fun getEmpresaId(): Int? = _sessao.value.selecionado?.empresaId
    fun clear() = salvar(Sessao())
}
