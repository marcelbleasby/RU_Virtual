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
            tokenStore.salvar(Sessao(body.tokenAccess.token, body.contextos, escolherContexto(body.contextos, null)))
            revalidar()
        }
    }

    suspend fun refreshUsuarioAtivo(): Result<User> = mutex.withLock { resultado { revalidar() } }

    private suspend fun revalidar(): User {
        var response = apiService.getUsuarioAtivo()
        if (response.code() == 403) {
            userRepository.clearUser()
            tokenStore.salvar(tokenStore.sessao.value.copy(selecionado = null, validada = false))
            response = apiService.getUsuarioAtivo()
        }
        if (response.code() == 401) {
            tokenStore.clear()
            userRepository.clearUser()
            throw IllegalStateException("Sessão expirada. Faça login novamente.")
        }
        var body = response.body()
        if (!response.isSuccessful || body == null) throw IllegalStateException("Não foi possível validar a sessão (${response.code()}).")
        val anterior = tokenStore.sessao.value.selecionado
        tokenStore.atualizarContextos(body.contextos, validada = tokenStore.sessao.value.validada)
        if (!tokenStore.sessao.value.selecionado?.mesmoPar(anterior).let { it == true } && tokenStore.sessao.value.selecionado != null) {
            response = apiService.getUsuarioAtivo()
            body = response.body()
            if (!response.isSuccessful || body == null) throw IllegalStateException("Não foi possível validar a unidade.")
        }
        val user = body.toUser()
        userRepository.saveUser(user)
        tokenStore.atualizarContextos(body.contextos)
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
