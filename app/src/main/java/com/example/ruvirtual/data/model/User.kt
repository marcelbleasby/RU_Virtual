package com.example.ruvirtual.data.model

data class User(
    val nome: String?,
    val matricula: String,
    val creditos: Int,
    val transacoes: List<Transaction>,
    val vCardId: String?
)
