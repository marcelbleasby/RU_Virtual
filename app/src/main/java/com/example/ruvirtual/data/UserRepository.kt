package com.example.ruvirtual.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.ruvirtual.data.model.User
import com.google.gson.Gson

class UserRepository(private val sharedPreferences: SharedPreferences) {

    private var user: User? = null

    companion object {
        private const val USER_KEY = "user"
    }

    fun saveUser(user: User) {
        this.user = user
        val userJson = Gson().toJson(user)
        sharedPreferences.edit { putString(USER_KEY, userJson) }
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
