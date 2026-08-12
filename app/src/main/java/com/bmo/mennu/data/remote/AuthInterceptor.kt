package com.bmo.mennu.data.remote

import com.bmo.mennu.data.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

// Anexa Authorization: Bearer <token> e o header de tenant Empresa-id-x
// (mennu-api é multi-tenant) em toda chamada, quando a sessão já tiver esses dados.
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        tokenStore.getToken()?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
        tokenStore.getEmpresaId()?.let { requestBuilder.addHeader("Empresa-id-x", it.toString()) }
        return chain.proceed(requestBuilder.build())
    }
}
