package com.bmo.mennu.data

import com.bmo.mennu.data.model.Contexto
import com.bmo.mennu.data.model.LoginRequest
import com.bmo.mennu.data.model.PerfilSessao
import com.bmo.mennu.data.model.User
import com.bmo.mennu.data.remote.ApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenStore: TokenStore,
    private val userRepository: UserRepository
) {
    private val mutex = Mutex()
    val sessao = tokenStore.sessao

    private suspend fun <T> resultado(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) { Result.failure(e) }

    suspend fun login(email: String, password: String): Result<User> = mutex.withLock {
        resultado {
            val response = apiService.login(LoginRequest(email, password))
            val body = response.body()
            if (!response.isSuccessful || body == null) throw IllegalStateException(when (response.code()) {
                401 -> "Credenciais inválidas."
                403 -> "Contexto não autorizado."
                else -> "Erro de login (código ${response.code()})."
            })
            require(body.tokenAccess.token.isNotBlank()) { "Login sem token de sessão." }
            userRepository.clearUser()
            // Login pode devolver só uma empresa na API principal. Descobrir antes de escolher.
            tokenStore.salvar(Sessao(token = body.tokenAccess.token))
            revalidar()
        }
    }

    suspend fun refreshUsuarioAtivo(): Result<User> = mutex.withLock { resultado { revalidar() } }

    private fun verificarSessaoExpirada(code: Int) {
        if (code == 401) {
            tokenStore.clear()
            userRepository.clearUser()
            throw IllegalStateException("Sessão expirada. Faça login novamente.")
        }
    }

    private suspend fun descobrirContextos() {
        // O interceptor envia só o token: esta é a lista global, inclusive na API principal.
        val response = apiService.getContextos()
        verificarSessaoExpirada(response.code())
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw IllegalStateException("Não foi possível consultar suas unidades (${response.code()}).")
        }
        val atual = tokenStore.sessao.value
        val selecionado = escolherContexto(body.contextos, atual.selecionado)
        val mesmaSelecao = selecionado?.mesmoPar(atual.selecionado) == true
        if (!mesmaSelecao) userRepository.clearUser()
        tokenStore.salvar(atual.copy(
            contextos = body.contextos, selecionado = selecionado,
            validada = atual.validada && mesmaSelecao,
        ))
    }

    private suspend fun revalidar(): User {
        descobrirContextos()
        var response = apiService.getUsuarioAtivo()
        verificarSessaoExpirada(response.code())
        if (response.code() == 403) {
            // O vínculo pode ter sido revogado entre a descoberta e a consulta do perfil.
            userRepository.clearUser()
            tokenStore.salvar(tokenStore.sessao.value.copy(selecionado = null, validada = false))
            descobrirContextos()
            response = apiService.getUsuarioAtivo()
            verificarSessaoExpirada(response.code())
        }
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw IllegalStateException("Não foi possível validar a sessão (${response.code()}).")
        }
        val atual = tokenStore.sessao.value
        val user = if (atual.selecionado != null) body.toUser() else body.toUser().copy(
            empresaId = null, matricula = null, cargo = null, vCardId = null, tenantSalt = null,
        )
        userRepository.saveUser(user)
        // /ativo pode conter só a empresa selecionada. Nunca substitui a lista global.
        tokenStore.salvar(atual.copy(validada = true))
        return user
    }

    suspend fun selecionarContexto(contexto: Contexto): Result<User> = mutex.withLock {
        resultado {
            val atual = tokenStore.sessao.value
            require(atual.contextos.any { it.mesmoPar(contexto) }) { "Contexto indisponível." }
            userRepository.clearUser()
            tokenStore.salvar(atual.copy(selecionado = contexto, validada = false))
            revalidar()
        }
    }

    suspend fun logout() = mutex.withLock {
        try { apiService.logout() } catch (e: CancellationException) { throw e
        } catch (_: Exception) { /* Limpeza local mesmo sem conexão. */
        } finally {
            tokenStore.clear()
            userRepository.clearUser()
        }
    }
}

private fun PerfilSessao.toUser() = User(
    id = id, nome = nome, email = email, matricula = matricula, cargo = cargo,
    empresaId = empresaId, vCardId = numeroCartao, tenantSalt = tenantSalt
)
