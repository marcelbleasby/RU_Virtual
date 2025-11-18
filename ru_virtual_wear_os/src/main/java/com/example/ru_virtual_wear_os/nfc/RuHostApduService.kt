package com.example.ru_virtual_wear_os.nfc

import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Arrays

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class RuHostApduService : HostApduService() {

    companion object {
        // In-memory cache for the VCardId to ensure fast responses.
        // It's volatile to ensure visibility across threads, although not strictly necessary here.
        @Volatile
        var vCardId: String? = null

        private const val SELECT_APDU_HEADER = "00A40400"
        private const val APP_AID = "F0010203040506"
        private val SELECT_APDU_HEADER_BYTES by lazy { hexStringToByteArray(SELECT_APDU_HEADER) }
        private val APP_AID_BYTES by lazy { hexStringToByteArray(APP_AID) }
        private const val MAX_RESPONSE_SIZE = 240
        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())

        private fun hexStringToByteArray(hex: String): ByteArray {
            val len = hex.length
            if (len % 2 != 0) throw IllegalArgumentException("hex string must have even length")
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                val hi = Character.digit(hex[i], 16)
                val lo = Character.digit(hex[i + 1], 16)
                if (hi == -1 || lo == -1) throw IllegalArgumentException("Invalid hex character in: $hex")
                data[i / 2] = ((hi shl 4) + lo).toByte()
                i += 2
            }
            return data
        }
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        // If the cache is empty (e.g., after a reboot), try to hydrate it from DataStore once.
        if (vCardId == null) {
            Log.d("RuHostApduService", "Cache is empty. Hydrating from DataStore.")
            runBlocking {
                vCardId = getVCardIdFromDataStore()
            }
            Log.d("RuHostApduService", "Hydrated cache with VCardId: $vCardId")
        }

        try {
            if (commandApdu.size < 5) return SW_CONDITIONS_NOT_SATISFIED

            val header = commandApdu.copyOfRange(0, 4)
            if (Arrays.equals(header, SELECT_APDU_HEADER_BYTES)) {
                val aidLength = commandApdu[4].toInt() and 0xFF
                if (aidLength < 0 || commandApdu.size < 5 + aidLength) return SW_CONDITIONS_NOT_SATISFIED

                val aid = commandApdu.copyOfRange(5, 5 + aidLength)
                if (Arrays.equals(aid, APP_AID_BYTES)) {
                    val currentVCardId = vCardId // Read from the fast in-memory cache
                    Log.d("RuHostApduService", "Retrieved vCardId for emulation: $currentVCardId")
                    if (currentVCardId.isNullOrEmpty()) {
                        Log.w("RuHostApduService", "vCardId is null or empty, returning SW_CONDITIONS_NOT_SATISFIED.")
                        return SW_CONDITIONS_NOT_SATISFIED
                    }

                    val vCardBytes = currentVCardId.toByteArray(Charsets.UTF_8)
                    val payload = if (vCardBytes.size > MAX_RESPONSE_SIZE) {
                        Log.w("RuHostApduService", "vCardId payload (${vCardBytes.size} bytes) exceeds max; truncating to $MAX_RESPONSE_SIZE bytes")
                        vCardBytes.copyOfRange(0, MAX_RESPONSE_SIZE)
                    } else vCardBytes

                    return payload + SW_OK
                }
            }
            return SW_CONDITIONS_NOT_SATISFIED
        } catch (e: Exception) {
            Log.e("RuHostApduService", "Error processing APDU", e)
            return SW_CONDITIONS_NOT_SATISFIED
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d("RuHostApduService", "Service deactivated. Reason: $reason")
    }

    private suspend fun getVCardIdFromDataStore(): String? {
        val key = stringPreferencesKey("vcard_id")
        return try {
            val preferences = dataStore.data.first()
            preferences[key]
        } catch (e: Exception) {
            Log.e("RuHostApduService", "Error reading VCardId from DataStore", e)
            null
        }
    }
}
