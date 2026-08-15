package com.bmo.mennu.ui.cardapio

import java.util.Date

// Espelha RestricaoAlimentar (mennu-api core/models/enums/restricao_alimentar.py) —
// slug bate exatamente com os valores de Prato.restricoes vindos da API.
enum class DietTag(val label: String, val slug: String) {
    VEGETARIANO("Vegetariano", "vegetariano"),
    VEGANO("Vegano", "vegano"),
    SEM_GLUTEN("Sem Glúten", "sem_gluten"),
    SEM_LACTOSE("Sem Lactose", "sem_lactose");

    companion object {
        fun fromSlug(slug: String): DietTag? = entries.find { it.slug == slug }
    }
}

// Espelha TipoRefeicao (mennu-api core/models/tipo_refeicao.py): tipo de refeição é
// configurável por unidade, não um enum fixo — ordenado pelo campo `ordem` real.
data class MealType(val id: Int, val nome: String, val ordem: Int)

// Espelha TipoPrato (mennu-api core/models/enums/tipo_prato.py) — mesmos 5 valores e
// labels do get_tipo_prato_display() do backend.
enum class FoodCategory(val label: String) {
    PRINCIPAL("Prato Principal"),
    GUARNICAO("Guarnição"),
    SALADA("Salada"),
    SOBREMESA("Sobremesa"),
    BEBIDA("Bebida")
}

// Espelha Prato (mennu-api core/models/prato.py).
data class Dish(
    val id: String,
    val name: String,
    val category: FoodCategory,
    val tags: List<DietTag>
)

// Um dia da semana com seus Cardapio agrupados por MealType — no backend real, cada
// combinação (data_refeicao, tipo_refeicao, unidade) é um Cardapio próprio com sua
// lista de Prato; aqui agrupados num único Map por conveniência de UI.
data class DayMenu(
    val date: Date,
    val dayLabel: String,
    val meals: Map<MealType, List<Dish>>
)
