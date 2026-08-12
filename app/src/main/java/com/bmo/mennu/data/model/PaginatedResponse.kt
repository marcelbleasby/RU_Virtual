package com.bmo.mennu.data.model

import com.google.gson.annotations.SerializedName

// Espelha o envelope de paginação padrão do mennu-api (ApiMennuPaginatedResponse).
data class PaginationMeta(
    val count: Int,
    val next: String?,
    val previous: String?,
    val page: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_results")
    val totalResults: Int
)

data class PaginatedResponse<T>(
    val message: String,
    val metadados: PaginationMeta,
    val results: List<T>
)
