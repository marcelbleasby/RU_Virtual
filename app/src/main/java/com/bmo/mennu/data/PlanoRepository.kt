package com.bmo.mennu.data

import javax.inject.Inject

data class PlanoInfo(
    val nomePlano: String,
    val creditosDisponiveis: Int,
    val dataRenovacaoExibicao: String
)

class PlanoRepository @Inject constructor() {
    // MOCK — não há endpoint de plano/créditos/renovação no mennu-api hoje
    // (ApiService só tem login, ativo, logout e /api/refeicao/). Fonte única desses
    // valores pra Home e Cartão não divergirem. suspend já hoje pra trocar por uma
    // chamada real no futuro não exigir mudar nenhum call site.
    suspend fun getPlanoInfo(): PlanoInfo = PlanoInfo(
        nomePlano = "Plano Corporativo",
        creditosDisponiveis = 18,
        dataRenovacaoExibicao = "01 de Março"
    )
}
