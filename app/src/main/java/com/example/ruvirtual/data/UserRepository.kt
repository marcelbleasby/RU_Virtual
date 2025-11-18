package com.example.ruvirtual.data

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.ruvirtual.data.model.User
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
    }

    fun saveUser(user: User) {
        this.user = user
        val userJson = Gson().toJson(user)
        sharedPreferences.edit { putString(USER_KEY, userJson) }

        val putDataMapReq = PutDataMapRequest.create(USER_DATA_PATH).apply {
            dataMap.putString(VCARD_ID_KEY, user.vCardId)
        }
        val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
        dataClient.putDataItem(putDataReq).addOnSuccessListener {
            Log.d("UserRepository", "VCardId sent to wear: ${user.vCardId}")
        }.addOnFailureListener {
            Log.e("UserRepository", "Failed to send VCardId to wear", it)
        }
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

    fun clearUser() {
        user = null
        sharedPreferences.edit { remove(USER_KEY) }
    }
}
