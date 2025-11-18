package com.bmo.mennu.data.remote

import com.bmo.mennu.data.model.LoginRequest
import com.bmo.mennu.data.model.ProvisionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("/api/provision-card")
    suspend fun provisionCard(@Body request: LoginRequest): Response<ProvisionResponse>

    @GET("/api/card-details") // New endpoint for fetching card details
    suspend fun getCardDetails(): Response<ProvisionResponse>
}
