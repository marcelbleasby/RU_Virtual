package com.bmo.mennu.data

import com.bmo.mennu.data.local.RefeicaoServidaDao
import com.bmo.mennu.data.local.RefeicaoServidaEntity
import com.bmo.mennu.data.local.SyncMetaDao
import com.bmo.mennu.data.local.SyncMetaEntity
import com.bmo.mennu.data.model.RefeicaoServida
import com.bmo.mennu.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed interface MealHistoryResult {
    data class Success(val refeicoes: List<RefeicaoServida>) : MealHistoryResult
    // 401/403: conta do usuário não tem o Cargo/permissão "refeicaoservida.view.list"
    // liberado no mennu-api — tratado como feature indisponível, não como erro.
    object Unavailable : MealHistoryResult
    data class Failure(val message: String) : MealHistoryResult
}

class MealRepository @Inject constructor(
    private val refeicaoServidaDao: RefeicaoServidaDao,
    private val syncMetaDao: SyncMetaDao,
    private val apiService: ApiService
) {
    // Offline-first: emite o cache do Room na hora; refreshRefeicoesServidas() busca
    // no servidor por cima e regrava o cache, o que a Flow reemite sozinha.
    fun observeRefeicoesServidas(usuarioId: Int): Flow<List<RefeicaoServida>> =
        refeicaoServidaDao.observeForUser(usuarioId).map { entities -> entities.map { it.toModel() } }

    fun observeLastSyncedAt(usuarioId: Int): Flow<Long?> =
        syncMetaDao.observeLastSyncedAt(mealSyncKey(usuarioId))

    suspend fun refreshRefeicoesServidas(usuarioId: Int): MealHistoryResult {
        return try {
            val response = apiService.getRefeicoesServidas(usuarioId)
            when {
                response.isSuccessful -> {
                    val refeicoes = response.body()?.results ?: emptyList()
                    refeicaoServidaDao.replaceForUser(usuarioId, refeicoes.map { it.toEntity(usuarioId) })
                    syncMetaDao.upsert(SyncMetaEntity(mealSyncKey(usuarioId), System.currentTimeMillis()))
                    MealHistoryResult.Success(refeicoes)
                }
                response.code() == 401 || response.code() == 403 -> MealHistoryResult.Unavailable
                else -> MealHistoryResult.Failure("Erro ao carregar refeições (código ${response.code()}).")
            }
        } catch (e: Exception) {
            MealHistoryResult.Failure(e.localizedMessage ?: "Erro de conexão.")
        }
    }

    private fun mealSyncKey(usuarioId: Int) = "meal_history_$usuarioId"
}

private fun RefeicaoServida.toEntity(usuarioId: Int) = RefeicaoServidaEntity(
    id = id,
    usuarioId = usuarioId,
    cardapioId = cardapioId,
    unidadeNome = unidadeNome,
    dataHora = dataHora,
    manual = manual
)

private fun RefeicaoServidaEntity.toModel() = RefeicaoServida(
    id = id,
    cardapioId = cardapioId,
    unidadeNome = unidadeNome,
    dataHora = dataHora,
    manual = manual
)
