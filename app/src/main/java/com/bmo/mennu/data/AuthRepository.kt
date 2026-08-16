package com.bmo.mennu.data

import com.bmo.mennu.data.model.LoginRequest
import com.bmo.mennu.data.model.LoginResponse
import com.bmo.mennu.data.model.User
import com.bmo.mennu.data.remote.ApiService
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenStore: TokenStore,
    private val userRepository: UserRepository
) {
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                tokenStore.saveSession(body.tokenAccess?.token, body.empresaId)
                val user = body.toUser()
                userRepository.saveUser(user)
                Result.success(user)
            } else {
                Result.failure(IllegalStateException(loginErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshUsuarioAtivo(): Result<User> {
        return try {
            val response = apiService.getUsuarioAtivo()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val user = body.toUser()
                userRepository.saveUser(user)
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Sessão expirada."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            apiService.logout()
        } catch (_: Exception) {
            // Best effort — mesmo se a chamada de logout falhar, a sessão local é limpa.
        }
        tokenStore.clear()
        userRepository.clearUser()
    }

    private fun loginErrorMessage(code: Int): String = when (code) {
        401 -> "Credenciais inválidas."
        403 -> "Usuário não pertence a esta empresa."
        else -> "Erro de login (código $code)."
    }
}

private fun LoginResponse.toUser() = User(
    id = id,
    nome = nome,
    email = email,
    matricula = matricula,
    cargo = cargo,
    empresaId = empresaId,
    vCardId = numeroCartao
)
