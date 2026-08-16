package com.bmo.mennu.data.remote

import com.bmo.mennu.data.model.CardapioResponse
import com.bmo.mennu.data.model.LoginRequest
import com.bmo.mennu.data.model.LoginResponse
import com.bmo.mennu.data.model.PaginatedResponse
import com.bmo.mennu.data.model.RefeicaoServida
import com.bmo.mennu.data.model.TipoRefeicaoResponse
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

    // Self-service: sem unidade_id, retorna só a(s) unidade(s) do usuário autenticado
    // (ver core/api/routers/cardapio.py no mennu-api).
    @GET("api/cardapio/")
    suspend fun getCardapios(
        @Query("data_refeicao_after") dataRefeicaoApos: String,
        @Query("data_refeicao_before") dataRefeicaoAntes: String,
        @Query("page_size") pageSize: Int = 100
    ): Response<PaginatedResponse<CardapioResponse>>

    // Self-service: sem exigir Cargo, retorna só os tipos de refeição da(s)
    // unidade(s) do usuário autenticado (ver core/api/routers/tipo_refeicao.py).
    @GET("api/tipo-refeicao/minhas")
    suspend fun getTiposRefeicaoMinhas(
        @Query("page_size") pageSize: Int = 100
    ): Response<PaginatedResponse<TipoRefeicaoResponse>>
}
