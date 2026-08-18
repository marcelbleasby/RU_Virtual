package com.bmo.mennu.nfc

import org.junit.Assert.assertEquals
import org.junit.Test

class AntiCloneCrcTest {

    // Vetor de teste verificado cruzado contra as outras duas portas do mesmo algoritmo:
    // core/utils/crypto.py::calcular_assinatura_backend (mennu-api) e
    // AntiCloneCrc.Compute (mennu-terminal-service-1, C#) — os três devem bater bit a bit.
    @Test
    fun `matches the Python and C# reference implementations for a known vector`() {
        val uid = hexStringToBytes("045253E31FF6")
        val payload = hexStringToBytes("0102030405060708")
        val salt = 0xABCD

        val crc = AntiCloneCrc.compute(uid, payload, salt)

        assertEquals(0x3980, crc)
    }

    @Test
    fun `different salts produce different signatures for the same uid and payload`() {
        val uid = hexStringToBytes("045253E31FF6")
        val payload = hexStringToBytes("0102030405060708")

        val crcA = AntiCloneCrc.compute(uid, payload, 0xABCD)
        val crcB = AntiCloneCrc.compute(uid, payload, 0x1234)

        assert(crcA != crcB)
    }

    private fun hexStringToBytes(hex: String): ByteArray {
        val data = ByteArray(hex.length / 2)
        for (i in data.indices) {
            data[i] = ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
        return data
    }
}
