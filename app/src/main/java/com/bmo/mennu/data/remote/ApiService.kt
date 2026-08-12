package com.bmo.mennu.data.remote

import com.bmo.mennu.data.model.LoginRequest
import com.bmo.mennu.data.model.LoginResponse
import com.bmo.mennu.data.model.PaginatedResponse
import com.bmo.mennu.data.model.RefeicaoServida
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/ativo")
    suspend fun getUsuarioAtivo(): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/refeicao/")
    suspend fun getRefeicoesServidas(
        @Query("usuario_id") usuarioId: Int,
        @Query("page_size") pageSize: Int = 100
    ): Response<PaginatedResponse<RefeicaoServida>>
}
