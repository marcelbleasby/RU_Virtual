package com.bmo.mennu.nfc

/**
 * Porta exata de core/utils/crypto.py::calcular_assinatura_backend (mennu-api).
 * CRC-16/CCITT (poly 0x1021) com o salt como seed inicial. Mascara a cada iteração
 * (como o port C# em AntiCloneCrc.cs no mennu-terminal-service) em vez de só no final
 * (como o Python) — mais seguro em tipo de largura fixa, já provado equivalente.
 */
object AntiCloneCrc {
    fun compute(uid: ByteArray, payload: ByteArray, salt: Int): Int {
        var crc = salt and 0xFFFF

        fun feed(b: Byte) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x1021) else (crc shl 1)
                crc = crc and 0xFFFF
            }
        }

        uid.forEach { feed(it) }
        payload.forEach { feed(it) }
        return crc and 0xFFFF
    }
}
