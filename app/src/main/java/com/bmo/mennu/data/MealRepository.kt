package com.bmo.mennu.data

import com.bmo.mennu.data.model.RefeicaoServida
import com.bmo.mennu.data.remote.ApiService
import javax.inject.Inject

sealed interface MealHistoryResult {
    data class Success(val refeicoes: List<RefeicaoServida>) : MealHistoryResult
    // GET /refeicao/ com usuario_id = o próprio usuário autenticado é sempre
    // self-service no mennu-api (core/api/routers/refeicao.py: list_refeicoes
    // só exige a permissão "refeicaoservida.view.list" quando usuario_id é de
    // OUTRA pessoa) — na prática esse 403 não deveria mais acontecer pra essa
    // chamada, mas é mantido como fallback defensivo caso a regra mude.
    object Unavailable : MealHistoryResult
    // 401: o token parou de autenticar em pleno uso (ex.: conta desativada —
    // mennu_api/api/authenticate.py agora rejeita is_active=False mesmo com
    // token ainda não expirado). Sessão morta, não "recurso sem permissão" —
    // precisa derrubar a sessão local e voltar pro login, não mostrar
    // "indisponível" dentro do cartão.
    object SessionExpired : MealHistoryResult
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
                response.code() == 401 -> MealHistoryResult.SessionExpired
                response.code() == 403 -> MealHistoryResult.Unavailable
                else -> MealHistoryResult.Failure("Erro ao carregar refeições (código ${response.code()}).")
            }
        } catch (e: Exception) {
            MealHistoryResult.Failure(e.localizedMessage ?: "Erro de conexão.")
        }
    }
}
