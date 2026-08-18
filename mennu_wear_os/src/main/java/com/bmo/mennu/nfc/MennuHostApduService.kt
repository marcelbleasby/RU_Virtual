package com.bmo.mennu.nfc

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

class MennuHostApduService : HostApduService() {

    companion object {
        // In-memory cache for the VCardId to ensure fast responses.
        // It's volatile to ensure visibility across threads, although not strictly necessary here.
        @Volatile
        var vCardId: String? = null

        // Salt de tenant, entregue pelo phone via Data Layer (mesmo caminho do vCardId).
        @Volatile
        var tenantSalt: String? = null

        private const val SELECT_APDU_HEADER = "00A40400"
        private const val APP_AID = "F0010203040506"
        private val SELECT_APDU_HEADER_BYTES by lazy { hexStringToByteArray(SELECT_APDU_HEADER) }
        private val APP_AID_BYTES by lazy { hexStringToByteArray(APP_AID) }
        private const val MAX_RESPONSE_SIZE = 240
        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())

        private const val INS_SELECT = 0xA4.toByte().toInt()
        private const val INS_INTERNAL_AUTHENTICATE = 0x88.toByte().toInt()

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

    // Verdadeiro só após um SELECT do nosso AID bem-sucedido nesta sessão — exigido antes de
    // aceitar INTERNAL AUTHENTICATE, igual um cartão real exige SELECT antes de qualquer comando.
    @Volatile
    private var isSelected = false

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        // If the cache is empty (e.g., after a reboot), try to hydrate it from DataStore once.
        if (vCardId == null || tenantSalt == null) {
            Log.d("MennuHostApduService", "Cache incompleto. Hidratando do DataStore.")
            runBlocking {
                if (vCardId == null) vCardId = getVCardIdFromDataStore()
                if (tenantSalt == null) tenantSalt = getTenantSaltFromDataStore()
            }
            Log.d("MennuHostApduService", "Cache hidratado: vCardId=$vCardId, tenantSalt presente=${tenantSalt != null}")
        }

        try {
            if (commandApdu.size < 4) return SW_CONDITIONS_NOT_SATISFIED
            return when (commandApdu[1].toInt() and 0xFF) {
                INS_SELECT -> handleSelect(commandApdu)
                INS_INTERNAL_AUTHENTICATE -> handleInternalAuthenticate(commandApdu)
                else -> SW_CONDITIONS_NOT_SATISFIED
            }
        } catch (e: Exception) {
            Log.e("MennuHostApduService", "Error processing APDU", e)
            return SW_CONDITIONS_NOT_SATISFIED
        }
    }

    private fun handleSelect(commandApdu: ByteArray): ByteArray {
        if (commandApdu.size < 5) return SW_CONDITIONS_NOT_SATISFIED

        val header = commandApdu.copyOfRange(0, 4)
        if (!Arrays.equals(header, SELECT_APDU_HEADER_BYTES)) return SW_CONDITIONS_NOT_SATISFIED

        val aidLength = commandApdu[4].toInt() and 0xFF
        if (aidLength < 0 || commandApdu.size < 5 + aidLength) return SW_CONDITIONS_NOT_SATISFIED

        val aid = commandApdu.copyOfRange(5, 5 + aidLength)
        if (!Arrays.equals(aid, APP_AID_BYTES)) return SW_CONDITIONS_NOT_SATISFIED

        val currentVCardId = vCardId // Read from the fast in-memory cache
        Log.d("MennuHostApduService", "Retrieved vCardId for emulation: $currentVCardId")
        if (currentVCardId.isNullOrEmpty()) {
            Log.w("MennuHostApduService", "vCardId is null or empty, returning SW_CONDITIONS_NOT_SATISFIED.")
            return SW_CONDITIONS_NOT_SATISFIED
        }

        val vCardBytes = currentVCardId.toByteArray(Charsets.UTF_8)
        val payload = if (vCardBytes.size > MAX_RESPONSE_SIZE) {
            Log.w("MennuHostApduService", "vCardId payload (${vCardBytes.size} bytes) exceeds max; truncating to $MAX_RESPONSE_SIZE bytes")
            vCardBytes.copyOfRange(0, MAX_RESPONSE_SIZE)
        } else vCardBytes

        isSelected = true
        return payload + SW_OK
    }

    /**
     * Challenge-response anti-clonagem: recebe um nonce do terminal e devolve o CRC-16
     * calculado com o salt de tenant, provando posse do vCardId sem depender de um bloco
     * de memória estático (que HCE não tem).
     */
    private fun handleInternalAuthenticate(commandApdu: ByteArray): ByteArray {
        if (!isSelected) return SW_CONDITIONS_NOT_SATISFIED
        if (commandApdu.size < 5) return SW_CONDITIONS_NOT_SATISFIED

        val lc = commandApdu[4].toInt() and 0xFF
        if (lc <= 0 || commandApdu.size < 5 + lc) return SW_CONDITIONS_NOT_SATISFIED
        val nonce = commandApdu.copyOfRange(5, 5 + lc)

        val currentVCardId = vCardId
        val currentTenantSalt = tenantSalt
        if (currentVCardId.isNullOrEmpty() || currentTenantSalt.isNullOrEmpty()) {
            Log.w("MennuHostApduService", "vCardId ou tenantSalt ausente para INTERNAL AUTHENTICATE.")
            return SW_CONDITIONS_NOT_SATISFIED
        }

        return try {
            val crc = AntiCloneCrc.compute(hexStringToByteArray(currentVCardId), nonce, currentTenantSalt.toInt(16))
            byteArrayOf(((crc shr 8) and 0xFF).toByte(), (crc and 0xFF).toByte()) + SW_OK
        } catch (e: Exception) {
            Log.e("MennuHostApduService", "Erro ao calcular assinatura INTERNAL AUTHENTICATE", e)
            SW_CONDITIONS_NOT_SATISFIED
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d("MennuHostApduService", "Service deactivated. Reason: $reason")
        isSelected = false
    }

    private suspend fun getVCardIdFromDataStore(): String? {
        val key = stringPreferencesKey("vcard_id")
        return try {
            val preferences = dataStore.data.first()
            preferences[key]
        } catch (e: Exception) {
            Log.e("MennuHostApduService", "Error reading VCardId from DataStore", e)
            null
        }
    }

    private suspend fun getTenantSaltFromDataStore(): String? {
        val key = stringPreferencesKey("tenant_salt")
        return try {
            val preferences = dataStore.data.first()
            preferences[key]
        } catch (e: Exception) {
            Log.e("MennuHostApduService", "Error reading tenantSalt from DataStore", e)
            null
        }
    }
}
