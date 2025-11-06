package com.example.ruvirtual.data.model

import com.google.gson.annotations.SerializedName

data class Transaction(
@SerializedName("id")
val id: String,
@SerializedName("matricula")
val matricula: String,
@SerializedName("tipo")
val tipo: String,
@SerializedName("valor")
val valor: Double, // <--- Changed to Double
@SerializedName("data")
val data: String,
@SerializedName("local")
val local: String?,
@SerializedName("metodo")
val metodo: String,
@SerializedName("referencia")
val referencia: String
)
