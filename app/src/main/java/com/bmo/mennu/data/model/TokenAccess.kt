package com.bmo.mennu.data.model

import com.google.gson.annotations.SerializedName

data class TokenAccess(
    @SerializedName("token")
    val token: String,
    @SerializedName("expirado_em")
    val expiradoEm: String
)
