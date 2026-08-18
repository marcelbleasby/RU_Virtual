package com.bmo.mennu.data

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.bmo.mennu.data.model.User
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.gson.Gson

class UserRepository(
    private val sharedPreferences: SharedPreferences,
    private val dataClient: DataClient
    ) {

    private var user: User? = null

    companion object {
        private const val USER_KEY = "user"
        private const val USER_DATA_PATH = "/user_data"
        private const val VCARD_ID_KEY = "vcard_id"
        private const val TENANT_SALT_KEY = "tenant_salt"
        private const val REFEICOES_MES_KEY = "refeicoes_mes"
        private const val REMEMBERED_EMAIL_KEY = "remembered_email"
    }

    fun saveRememberedEmail(email: String) {
        sharedPreferences.edit { putString(REMEMBERED_EMAIL_KEY, email) }
    }

    fun getRememberedEmail(): String? {
        return sharedPreferences.getString(REMEMBERED_EMAIL_KEY, null)
    }

    fun clearRememberedEmail() {
        sharedPreferences.edit { remove(REMEMBERED_EMAIL_KEY) }
    }

    fun saveUser(user: User) {
        this.user = user
        val userJson = Gson().toJson(user)
        sharedPreferences.edit { putString(USER_KEY, userJson) }
        syncToWear(user.vCardId, user.tenantSalt, refeicoesMes = null)
    }

    fun getUser(): User? {
        if (user != null) {
            return user
        }

        val userJson = sharedPreferences.getString(USER_KEY, null)
        if (userJson != null) {
            user = Gson().fromJson(userJson, User::class.java)
            return user
        }

        return null
    }

    // Repropaga o consumo do período pro relógio sem precisar reemitir o User inteiro
    // (chamado depois que a tela de cartão busca o histórico de refeições).
    fun updateRefeicoesMes(refeicoesMes: Int) {
        val currentUser = getUser()
        syncToWear(currentUser?.vCardId, currentUser?.tenantSalt, refeicoesMes)
    }

    fun clearUser() {
        user = null
        sharedPreferences.edit { remove(USER_KEY) }
    }

    private fun syncToWear(vCardId: String?, tenantSalt: String?, refeicoesMes: Int?) {
        val putDataMapReq = PutDataMapRequest.create(USER_DATA_PATH).apply {
            dataMap.putString(VCARD_ID_KEY, vCardId ?: "")
            dataMap.putString(TENANT_SALT_KEY, tenantSalt ?: "")
            if (refeicoesMes != null) {
                dataMap.putInt(REFEICOES_MES_KEY, refeicoesMes)
            }
        }
        val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
        dataClient.putDataItem(putDataReq).addOnSuccessListener {
            Log.d("UserRepository", "Dados sincronizados com o Wear (vCardId=$vCardId, refeicoesMes=$refeicoesMes)")
        }.addOnFailureListener {
            Log.e("UserRepository", "Falha ao sincronizar dados com o Wear", it)
        }
    }
}
