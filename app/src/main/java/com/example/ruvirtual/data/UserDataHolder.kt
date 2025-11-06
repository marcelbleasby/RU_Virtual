package com.example.ruvirtual.data

import com.example.ruvirtual.data.model.ProvisionResponse
import com.example.ruvirtual.data.model.Transaction

object UserDataHolder {
    var provisionResponse: ProvisionResponse? = null
    var nome: String = ""
    var matricula: String = ""
    var creditos: Int = 0
    var transacoes: List<Transaction> = emptyList()

    fun updateFromResponse(response: ProvisionResponse) {
        provisionResponse = response
        nome = response.nome
        matricula = response.matricula
        creditos = response.creditos
        transacoes = response.transacoes
    }

    fun clear() {
        provisionResponse = null
        nome = ""
        matricula = ""
        creditos = 0
        transacoes = emptyList()
    }
}
