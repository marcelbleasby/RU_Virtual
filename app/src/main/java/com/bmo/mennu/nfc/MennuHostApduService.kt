package com.bmo.mennu.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.bmo.mennu.data.NfcTapEventBus
import com.bmo.mennu.data.UserRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Arrays

class MennuHostApduService : HostApduService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MennuHostApduServiceEntryPoint {
        fun userRepository(): UserRepository
        fun nfcTapEventBus(): NfcTapEventBus
    }

    private var userRepository: UserRepository? = null
    private var nfcTapEventBus: NfcTapEventBus? = null

    // Verdadeiro só após um SELECT do nosso AID bem-sucedido nesta sessão — exigido antes de
    // aceitar INTERNAL AUTHENTICATE, igual um cartão real exige SELECT antes de qualquer comando.
    @Volatile
    private var isSelected = false

    private val SELECT_APDU_HEADER = "00A40400"
    private val APP_AID = "F0010203040506"
    private val SELECT_APDU_HEADER_BYTES by lazy { hexStringToByteArray(SELECT_APDU_HEADER) }
    private val APP_AID_BYTES by lazy { hexStringToByteArray(APP_AID) }
    private val MAX_RESPONSE_SIZE = 240
    private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
    private val SW_CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())

    private val INS_SELECT = 0xA4.toByte()
    private val INS_INTERNAL_AUTHENTICATE = 0x88.toByte()

    private fun getUserRepository(): UserRepository {
        if (userRepository == null) {
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                MennuHostApduServiceEntryPoint::class.java
            )
            userRepository = hiltEntryPoint.userRepository()
        }
        return userRepository!!
    }

    private fun getNfcTapEventBus(): NfcTapEventBus {
        if (nfcTapEventBus == null) {
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                MennuHostApduServiceEntryPoint::class.java
            )
            nfcTapEventBus = hiltEntryPoint.nfcTapEventBus()
        }
        return nfcTapEventBus!!
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        try {
            if (commandApdu.size < 4) return SW_CONDITIONS_NOT_SATISFIED
            return when (commandApdu[1]) {
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

        val vCardId = getUserRepository().getUser()?.vCardId
        Log.d("MennuHostApduService", "Retrieved vCardId for emulation: $vCardId")
        if (vCardId.isNullOrEmpty()) {
            Log.w("MennuHostApduService", "vCardId is null or empty, returning SW_CONDITIONS_NOT_SATISFIED.")
            return SW_CONDITIONS_NOT_SATISFIED
        }

        val vCardBytes = vCardId.toByteArray(Charsets.UTF_8)
        val payload = if (vCardBytes.size > MAX_RESPONSE_SIZE) {
            Log.w("MennuHostApduService", "vCardId payload (${vCardBytes.size} bytes) exceeds max; truncating to $MAX_RESPONSE_SIZE bytes")
            vCardBytes.copyOfRange(0, MAX_RESPONSE_SIZE)
        } else vCardBytes

        isSelected = true
        getNfcTapEventBus().emitTap()
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

        val user = getUserRepository().getUser()
        val vCardId = user?.vCardId
        val tenantSalt = user?.tenantSalt
        if (vCardId.isNullOrEmpty() || tenantSalt.isNullOrEmpty()) {
            Log.w("MennuHostApduService", "vCardId ou tenantSalt ausente para INTERNAL AUTHENTICATE.")
            return SW_CONDITIONS_NOT_SATISFIED
        }

        return try {
            val crc = AntiCloneCrc.compute(hexStringToByteArray(vCardId), nonce, tenantSalt.toInt(16))
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
