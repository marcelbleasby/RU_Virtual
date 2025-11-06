package com.example.ruvirtual.data

import android.content.SharedPreferences
import androidx.core.content.edit

class UserRepository(private val sharedPreferences: SharedPreferences) {

    companion object {
        private const val VIRTUAL_CARD_ID = "vCardId"
    }

    fun saveVirtualCardId(vCardId: String) {
        sharedPreferences.edit { putString(VIRTUAL_CARD_ID, vCardId) }
    }

    fun getVirtualCardId(): String? {
        return sharedPreferences.getString(VIRTUAL_CARD_ID, null)
    }

    fun clearVirtualCardId() {
        sharedPreferences.edit { remove(VIRTUAL_CARD_ID) }
    }
}
