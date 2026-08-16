package com.bmo.mennu.data

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class TokenStore @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val TOKEN_KEY = "auth_token"
        private const val EMPRESA_ID_KEY = "empresa_id"
    }

    fun saveSession(token: String?, empresaId: Int?) {
        sharedPreferences.edit {
            if (token != null) putString(TOKEN_KEY, token) else remove(TOKEN_KEY)
            if (empresaId != null) putInt(EMPRESA_ID_KEY, empresaId) else remove(EMPRESA_ID_KEY)
        }
    }

    fun getToken(): String? = sharedPreferences.getString(TOKEN_KEY, null)

    fun getEmpresaId(): Int? {
        if (!sharedPreferences.contains(EMPRESA_ID_KEY)) return null
        return sharedPreferences.getInt(EMPRESA_ID_KEY, -1).takeIf { it != -1 }
    }

    fun clear() {
        sharedPreferences.edit {
            remove(TOKEN_KEY)
            remove(EMPRESA_ID_KEY)
        }
    }
}
