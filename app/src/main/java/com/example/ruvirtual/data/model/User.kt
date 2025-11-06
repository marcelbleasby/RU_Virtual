package com.example.ruvirtual.data.model

data class User(
    val vCardId: String,
    val nome: String,
    val matricula: String,
    val creditos: Int,
    val transacoes: List<Transaction>
)
