package com.bmo.mennu.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bmo.mennu.data.CardapioRepository
import com.bmo.mennu.data.MealHistoryResult
import com.bmo.mennu.data.MealRepository
import com.bmo.mennu.data.UserRepository
import com.bmo.mennu.util.mondayOfCurrentWeek
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CardSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cardapioRepository: CardapioRepository,
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val weekStart = mondayOfCurrentWeek()
        val cardapioResult = cardapioRepository.refreshWeekMenu(weekStart)
        val tiposResult = cardapioRepository.refreshTiposRefeicao()

        val usuarioId = userRepository.getUser()?.id
        val mealFailed = usuarioId != null &&
            mealRepository.refreshRefeicoesServidas(usuarioId) is MealHistoryResult.Failure

        val anyFailure = cardapioResult.isFailure || tiposResult.isFailure || mealFailed
        return if (!anyFailure) {
            Result.success()
        } else if (runAttemptCount < 3) {
            Result.retry()
        } else {
            Result.failure()
        }
    }
}
