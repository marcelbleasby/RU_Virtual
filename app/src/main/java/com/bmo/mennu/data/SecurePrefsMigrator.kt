package com.bmo.mennu.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit

object SecurePrefsMigrator {
    private const val OLD_PREFS_NAME = "mennu_prefs"
    private const val MIGRATED_KEY = "migrated_from_plain_prefs"

    // Chaves conhecidas hoje espalhadas entre TokenStore e UserRepository,
    // ambos escritos na mesma SharedPreferences ("mennu_prefs").
    private val STRING_KEYS = listOf("auth_token", "user", "remembered_email")
    private val INT_KEYS = listOf("empresa_id")

    fun migrateIfNeeded(context: Context, securePrefs: SharedPreferences) {
        if (securePrefs.getBoolean(MIGRATED_KEY, false)) return

        val oldPrefs = context.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
        securePrefs.edit {
            STRING_KEYS.forEach { key ->
                oldPrefs.getString(key, null)?.let { putString(key, it) }
            }
            INT_KEYS.forEach { key ->
                if (oldPrefs.contains(key)) putInt(key, oldPrefs.getInt(key, -1))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences(OLD_PREFS_NAME)
        } else {
            oldPrefs.edit { clear() }
        }

        securePrefs.edit { putBoolean(MIGRATED_KEY, true) }
    }
}
