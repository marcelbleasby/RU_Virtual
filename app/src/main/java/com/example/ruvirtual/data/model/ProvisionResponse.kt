package com.example.ruvirtual.data.model

import com.google.gson.annotations.SerializedName

data class ProvisionResponse(
    @SerializedName("matricula")
    val matricula: String,
    @SerializedName("nome")
    val nome: String,
    @SerializedName("vCardId")
    val vCardId: String?,
    @SerializedName("creditos")
    val creditos: Int,
    @SerializedName("transacoes")
    val transacoes: List<Transaction> = emptyList()
)
