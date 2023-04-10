package com.example.grape_v0.serial

@ExperimentalUnsignedTypes
class CRC16(val polynomial: UShort = 0x1021.toUShort()) : CRC<UShort> {
    override val lookupTable: List<UShort> = (0 until 256).map { crc16(it.toUByte(), polynomial) }

    override var value: UShort = 0.toUShort()
        private set

    override fun update(inputs: UByteArray) {
        value = crc16(inputs, value)
    }

    override fun reset() {
        value = 0.toUShort()
    }

    private fun crc16(inputs: UByteArray, initialValue: UShort = 0.toUShort()): UShort {
        return inputs.fold(initialValue) { remainder, byte ->
            val bigEndianInput = byte.toBigEndianUShort()
            val index = (bigEndianInput xor remainder) shr 8
            lookupTable[index.toInt()] xor (remainder shl 8)
        }
    }

    private fun crc16(input: UByte, polynomial: UShort): UShort {
        val bigEndianInput = input.toBigEndianUShort()

        return (0 until 8).fold(bigEndianInput) { result, _ ->
            val isMostSignificantBitOne = result and 0x8000.toUShort() != 0.toUShort()
            val shiftedResult = result shl 1

            when (isMostSignificantBitOne) {
                true -> shiftedResult xor polynomial
                false -> shiftedResult
            }
        }
    }

    fun ushort2ByteArray(): ByteArray {
        val value = value.toInt()
        val bytes = ByteArray(2)
        bytes[0] = (value and 0xFF).toByte()
        bytes[1] = ((value ushr 8) and 0xFF).toByte()
        return bytes
    }
}

fun main() {
    val crc16 = CRC16(0x8005u)
    val data = ubyteArrayOf(0x02u,0x0eu,0x01u,0x0bu,0x01u,0xd6u,0x00u,0x01u,0xe3u)
    var strBuilder = StringBuilder()

    crc16.update(data)
    var crc_data = crc16.ushort2ByteArray()
    // get the current crc value
    for(i in 0 until 2){
        strBuilder.append(String.format("%02X ", crc_data[i]))
    }
    println("CRC: ${crc_data}, hex: " + strBuilder.toString())

    // append more
    crc16.update(5.6)
    crc16.update(225)
    crc16.update(ubyteArrayOf(1.toUByte()))

    // get the current crc value
    println("CRC: ${crc16.value}")

    // reset the crc value
    crc16.reset()

    crc16.value  // value is 0 after reset

    println("CRC: ${crc16.value}")

    // CRC16's lookup table
    println("Lookup Table: ${crc16.lookupTable}")

    // CRC16's lookup table (hexadecimal representation)
    val lookupTableHexa = crc16.lookupTable.map { it.toString(radix = 16) }
    println("Lookup Table(Hexa): $lookupTableHexa")
}