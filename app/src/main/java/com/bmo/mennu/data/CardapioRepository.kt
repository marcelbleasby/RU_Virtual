package com.bmo.mennu.data

import com.bmo.mennu.data.local.CardapioDao
import com.bmo.mennu.data.local.CardapioEntity
import com.bmo.mennu.data.local.SyncMetaDao
import com.bmo.mennu.data.local.SyncMetaEntity
import com.bmo.mennu.data.local.TipoRefeicaoDao
import com.bmo.mennu.data.local.TipoRefeicaoEntity
import com.bmo.mennu.data.model.CardapioResponse
import com.bmo.mennu.data.model.PratoResponse
import com.bmo.mennu.data.model.TipoRefeicaoResponse
import com.bmo.mennu.data.remote.ApiService
import com.bmo.mennu.ui.cardapio.DayMenu
import com.bmo.mennu.ui.cardapio.DietTag
import com.bmo.mennu.ui.cardapio.Dish
import com.bmo.mennu.ui.cardapio.FoodCategory
import com.bmo.mennu.ui.cardapio.MealType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class CardapioRepository @Inject constructor(
    private val cardapioDao: CardapioDao,
    private val tipoRefeicaoDao: TipoRefeicaoDao,
    private val syncMetaDao: SyncMetaDao,
    private val apiService: ApiService
) {
    private val isoDateFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val gson = Gson()

    // Offline-first: emite o cache do Room na hora (mesmo sem rede); refreshWeekMenu()
    // busca a semana no servidor por cima e regrava o cache, o que a Flow reemite sozinha.
    fun observeWeekMenu(weekStart: Date): Flow<List<DayMenu>> {
        val weekEnd = shiftDays(weekStart, 6)
        val startIso = isoDateFormat.format(weekStart)
        val endIso = isoDateFormat.format(weekEnd)
        return cardapioDao.observeRange(startIso, endIso).map { entities ->
            buildWeekDays(weekStart, entities.map { it.toResponse(gson) })
        }
    }

    fun observeWeekMenuLastSyncedAt(weekStart: Date): Flow<Long?> =
        syncMetaDao.observeLastSyncedAt(weekSyncKey(weekStart))

    // GET /cardapio/ é self-service por unidade (ver core/api/routers/cardapio.py no
    // mennu-api) — sem precisar de nenhum Cargo, retorna só a(s) unidade(s) do usuário
    // autenticado. Sempre retorna 7 DayMenu (segunda a domingo, nessa ordem) — dias
    // sem cardápio cadastrado vêm com meals vazio, nunca omitidos da lista.
    suspend fun refreshWeekMenu(weekStart: Date): Result<Unit> {
        return try {
            val weekEnd = shiftDays(weekStart, 6)
            val startIso = isoDateFormat.format(weekStart)
            val endIso = isoDateFormat.format(weekEnd)
            val response = apiService.getCardapios(dataRefeicaoApos = startIso, dataRefeicaoAntes = endIso)
            if (!response.isSuccessful) {
                return Result.failure(IOException("Não foi possível carregar o cardápio (${response.code()})."))
            }
            val cardapios = response.body()?.results.orEmpty()
            cardapioDao.replaceRange(startIso, endIso, cardapios.map { it.toEntity(gson) })
            syncMetaDao.upsert(SyncMetaEntity(weekSyncKey(weekStart), System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeTiposRefeicao(): Flow<List<TipoRefeicaoResponse>> =
        tipoRefeicaoDao.observeAll().map { entities -> entities.map { it.toResponse() } }

    // GET /tipo-refeicao/minhas também é self-service por unidade, mesmo padrão
    // de refreshWeekMenu — usado pra saber o horário real de cada refeição (não vem
    // no /cardapio/) e resolver qual é "a refeição de agora".
    suspend fun refreshTiposRefeicao(): Result<Unit> {
        return try {
            val response = apiService.getTiposRefeicaoMinhas()
            if (!response.isSuccessful) {
                return Result.failure(IOException("Não foi possível carregar os tipos de refeição (${response.code()})."))
            }
            val tipos = response.body()?.results.orEmpty()
            tipoRefeicaoDao.replaceAll(tipos.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun weekSyncKey(weekStart: Date) = "cardapio_week_${isoDateFormat.format(weekStart)}"
}

internal fun buildWeekDays(weekStart: Date, cardapios: List<CardapioResponse>): List<DayMenu> {
    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayLabelFormat = SimpleDateFormat("EEEE", Locale.forLanguageTag("pt-BR"))
    val cardapiosPorData = cardapios.groupBy { it.dataRefeicao }

    return (0 until 7).map { offset ->
        val date = shiftDays(weekStart, offset)
        val meals = cardapiosPorData[isoDateFormat.format(date)].orEmpty()
            .associate { cardapio -> cardapio.toMealType() to cardapio.pratos.map { it.toDish() } }
        DayMenu(
            date = date,
            dayLabel = dayLabelFormat.format(date).replaceFirstChar { it.uppercase() },
            meals = meals
        )
    }
}

private fun shiftDays(date: Date, days: Int): Date =
    Calendar.getInstance().apply { time = date; add(Calendar.DAY_OF_MONTH, days) }.time

private fun CardapioResponse.toMealType() =
    MealType(id = tipoRefeicaoId, nome = tipoRefeicaoNome, ordem = tipoRefeicaoOrdem)

private fun PratoResponse.toDish() = Dish(
    id = id.toString(),
    name = nome,
    category = FoodCategory.valueOf(tipoPrato.uppercase()),
    tags = restricoes.mapNotNull { DietTag.fromSlug(it) }
)

private val pratosListType = object : TypeToken<List<PratoResponse>>() {}.type

private fun CardapioResponse.toEntity(gson: Gson) = CardapioEntity(
    id = id,
    dataRefeicao = dataRefeicao,
    tipoRefeicaoId = tipoRefeicaoId,
    tipoRefeicaoNome = tipoRefeicaoNome,
    tipoRefeicaoOrdem = tipoRefeicaoOrdem,
    pratosJson = gson.toJson(pratos)
)

private fun CardapioEntity.toResponse(gson: Gson) = CardapioResponse(
    id = id,
    dataRefeicao = dataRefeicao,
    tipoRefeicaoId = tipoRefeicaoId,
    tipoRefeicaoNome = tipoRefeicaoNome,
    tipoRefeicaoOrdem = tipoRefeicaoOrdem,
    pratos = gson.fromJson<List<PratoResponse>>(pratosJson, pratosListType) ?: emptyList()
)

private fun TipoRefeicaoResponse.toEntity() = TipoRefeicaoEntity(
    id = id,
    nome = nome,
    unidadeId = unidadeId,
    horarioInicio = horarioInicio,
    horarioFim = horarioFim,
    ordem = ordem
)

private fun TipoRefeicaoEntity.toResponse() = TipoRefeicaoResponse(
    id = id,
    nome = nome,
    unidadeId = unidadeId,
    horarioInicio = horarioInicio,
    horarioFim = horarioFim,
    ordem = ordem
)
