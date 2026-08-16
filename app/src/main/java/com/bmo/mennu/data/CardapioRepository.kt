package com.bmo.mennu.data

import com.bmo.mennu.data.model.CardapioResponse
import com.bmo.mennu.data.model.PratoResponse
import com.bmo.mennu.data.model.TipoRefeicaoResponse
import com.bmo.mennu.data.remote.ApiService
import com.bmo.mennu.ui.cardapio.DayMenu
import com.bmo.mennu.ui.cardapio.DietTag
import com.bmo.mennu.ui.cardapio.Dish
import com.bmo.mennu.ui.cardapio.FoodCategory
import com.bmo.mennu.ui.cardapio.MealType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class CardapioRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val isoDateFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // GET /cardapio/ é self-service por unidade (ver core/api/routers/cardapio.py no
    // mennu-api) — sem precisar de nenhum Cargo, retorna só a(s) unidade(s) do usuário
    // autenticado. Sempre retorna 7 DayMenu (segunda a domingo, nessa ordem) — dias
    // sem cardápio cadastrado vêm com meals vazio, nunca omitidos da lista.
    suspend fun getWeekMenu(weekStart: Date): List<DayMenu> {
        val weekEnd = shiftDays(weekStart, 6)
        val response = apiService.getCardapios(
            dataRefeicaoApos = isoDateFormat.format(weekStart),
            dataRefeicaoAntes = isoDateFormat.format(weekEnd)
        )
        if (!response.isSuccessful) {
            throw java.io.IOException("Não foi possível carregar o cardápio (${response.code()}).")
        }
        return buildWeekDays(weekStart, response.body()?.results.orEmpty())
    }

    // GET /tipo-refeicao/minhas também é self-service por unidade, mesmo padrão
    // de getWeekMenu — usado pra saber o horário real de cada refeição (não vem
    // no /cardapio/) e resolver qual é "a refeição de agora".
    suspend fun getMinhasTiposRefeicao(): List<TipoRefeicaoResponse> {
        val response = apiService.getTiposRefeicaoMinhas()
        if (!response.isSuccessful) {
            throw java.io.IOException("Não foi possível carregar os tipos de refeição (${response.code()}).")
        }
        return response.body()?.results.orEmpty()
    }
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
