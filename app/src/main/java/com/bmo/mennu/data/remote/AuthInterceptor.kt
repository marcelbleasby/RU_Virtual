package com.bmo.mennu.data.remote

import com.bmo.mennu.data.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class AuthInterceptor @Inject constructor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val caminho = request.url.encodedPath.trimEnd('/')
        val sessao = tokenStore.sessao.value
        val builder = request.newBuilder()
            .removeHeader("Authorization").removeHeader("Empresa-id-x").removeHeader("Unidade-id-x")
        if (caminho == "/api/auth/login") return chain.proceed(builder.build())
        sessao.token?.let { builder.header("Authorization", "Bearer $it") }
        val descoberta = caminho in setOf("/api/auth/contextos", "/api/auth/logout")
        val ativo = caminho == "/api/auth/ativo"
        if (!descoberta) {
            if (!ativo && (!sessao.validada || sessao.selecionado == null)) {
                throw IOException("Escolha uma unidade autorizada para continuar.")
            }
            sessao.selecionado?.let {
                builder.header("Empresa-id-x", it.empresaId.toString())
                builder.header("Unidade-id-x", it.unidadeId.toString())
            }
        }
        val response = chain.proceed(builder.build())
        // Descarta também respostas concluídas durante uma troca/logout/login.
        if (tokenStore.sessao.value != sessao) {
            response.close()
            throw IOException("A sessão mudou durante a consulta. Tente novamente.")
        }
        return response
    }
}
