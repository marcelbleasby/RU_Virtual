package com.bmo.mennu.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
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
    }

    private var userRepository: UserRepository? = null

    private val SELECT_APDU_HEADER = "00A40400"
    private val APP_AID = "F0010203040506"
    private val SELECT_APDU_HEADER_BYTES by lazy { hexStringToByteArray(SELECT_APDU_HEADER) }
    private val APP_AID_BYTES by lazy { hexStringToByteArray(APP_AID) }
    private val MAX_RESPONSE_SIZE = 240
    private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
    private val SW_CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())

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

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        try {
            if (commandApdu.size < 5) return SW_CONDITIONS_NOT_SATISFIED

            val header = commandApdu.copyOfRange(0, 4)
            if (Arrays.equals(header, SELECT_APDU_HEADER_BYTES)) {
                val aidLength = commandApdu[4].toInt() and 0xFF
                if (aidLength < 0 || commandApdu.size < 5 + aidLength) return SW_CONDITIONS_NOT_SATISFIED

                val aid = commandApdu.copyOfRange(5, 5 + aidLength)
                if (Arrays.equals(aid, APP_AID_BYTES)) {
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

                    return payload + SW_OK
                }
            }
            return SW_CONDITIONS_NOT_SATISFIED
        } catch (e: Exception) {
            Log.e("MennuHostApduService", "Error processing APDU", e)
            return SW_CONDITIONS_NOT_SATISFIED
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d("MennuHostApduService", "Service deactivated. Reason: $reason")
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
