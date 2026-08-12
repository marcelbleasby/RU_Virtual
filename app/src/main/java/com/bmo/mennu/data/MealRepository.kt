package com.bmo.mennu.data

import com.bmo.mennu.data.model.RefeicaoServida
import com.bmo.mennu.data.remote.ApiService
import javax.inject.Inject

sealed interface MealHistoryResult {
    data class Success(val refeicoes: List<RefeicaoServida>) : MealHistoryResult
    // 401/403: conta do usuário não tem o Cargo/permissão "refeicaoservida.view.list"
    // liberado no mennu-api — tratado como feature indisponível, não como erro.
    object Unavailable : MealHistoryResult
    data class Failure(val message: String) : MealHistoryResult
}

class MealRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getRefeicoesServidas(usuarioId: Int): MealHistoryResult {
        return try {
            val response = apiService.getRefeicoesServidas(usuarioId)
            when {
                response.isSuccessful -> MealHistoryResult.Success(response.body()?.results ?: emptyList())
                response.code() == 401 || response.code() == 403 -> MealHistoryResult.Unavailable
                else -> MealHistoryResult.Failure("Erro ao carregar refeições (código ${response.code()}).")
            }
        } catch (e: Exception) {
            MealHistoryResult.Failure(e.localizedMessage ?: "Erro de conexão.")
        }
    }
}
