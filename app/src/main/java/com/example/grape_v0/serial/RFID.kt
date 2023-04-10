package com.example.grape_v0.serial

import android.util.Log
import com.example.grape_v0.DataCenter
import com.example.grape_v0.MainActivity
import kotlinx.coroutines.sync.Mutex

class RFID(
    dev: String, baud_rate: Int,
) : Runnable {
    val ser = Serial(dev, baud_rate, true)
    val mutex = Mutex()
    private val stx: UByte = 0x02u
    private val etx: UByte = 0x03u
    private val sidx = ubyteArrayOf(0x01u, 0x0Bu)
    private val ridx = ubyteArrayOf(0x01u, 0x0Eu)
    private val seq: UByte = 0x01u
    private val recv_seq: UByte = 0x02u
    private val crc16 = CRC16(0x8005u)
    private var _cmd: UByte? = null
    private val cmd_map = mapOf<UByte, Int>(
        0xD6.toUByte() to 1,
        0xD7.toUByte() to 9,
        0xD8.toUByte() to 1,
        0xEA.toUByte() to 15,
        0xEB.toUByte() to 15
    )
    private val length: Int = 9 // Data 이전까지의 길이
    private var read_data = mutableListOf<UByte>()
    private var data_signal: Boolean = false
    private var crc_signal: Boolean = false
    private var crc_data: ByteArray = ByteArray(0)
    private var _cnt: Int = 0

    override fun run() {
        println("${Thread.currentThread().name}")
        while (!Thread.currentThread().isInterrupted) {
            var get_data: UByte? = ser.readData()
            var finish_signal = get_data?.let { confirmData(it) }
            if (finish_signal == true) {
//                Log.d("RFID", "main recv: ${strBuilder.toString()}")
                getDataCenter()
                var strBuilder = StringBuilder()
                for (i in 0 until DataCenter.rfid.card_num.size) {
                    strBuilder.append(String.format("%02X ", DataCenter.rfid.card_num[i].toByte()))
                }
                MainActivity.liveText.postValue(strBuilder.toString())
            } else {
//                Log.d("RFID", "confirmData Error")
            }
        }
    }

    private fun confirmData(data: UByte): Boolean? {
        val len: Int = cmd_map[this._cmd]!!

        try {
            if (this._cmd != null) {
                if (!data_signal && !crc_signal) {
                    if (data == stx) {
                        read_data.add(data)
                    } else if (read_data.last() == stx && data == recv_seq) {
                        read_data.add(data)
                    } else if (read_data.last() == recv_seq && data == ridx[0]) {
                        read_data.add(data)
                    } else if (read_data.last() == ridx[0] && data == ridx[1]) {
                        read_data.add(data)
                    } else if (read_data.last() == ridx[1] && data == sidx[0]) {
                        read_data.add(data)
                    } else if (read_data.last() == sidx[0] && data == sidx[1]) {
                        read_data.add(data)
                    } else if (read_data.last() == sidx[1] && data == _cmd) {
                        read_data.add(data)
                    } else if (read_data.last() == _cmd && data == 0.toUByte()) {
                        read_data.add(data)
                    } else if (read_data.last() == 0.toUByte() && data == len.toUByte()) {
                        read_data.add(data)
                        data_signal = true
                    } else {
                        read_data.clear()
                        data_signal = false
                        _cnt = 0
                        crc_signal = false
                    }
                } else if (data_signal == true && crc_signal == false) {
                    if (_cnt < len) {
                        read_data.add(data)
                        _cnt++
                    } else {
                        crc16.update(read_data.toUByteArray())
                        crc_data = crc16.ushort2ByteArray()
                        crc16.reset()
                        _cnt = 0
                        this.crc_signal = true
                    }
                } else if (data_signal == true && crc_signal == true) {
                    if (data == crc_data[0].toUByte()) {
                        read_data.add(data)
                    } else if (read_data.last() == crc_data[0].toUByte()
                        && data == crc_data[1].toUByte()
                    ) {
                        read_data.add(data)
                    } else {
                        if (data == etx) {
                            read_data.add(data)
                            data_signal = false
                            _cnt = 0
                            crc_signal = false
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RFID", e.printStackTrace().toString())
            return null
        }
        return false
    }

//    private fun confirmData(data: ByteArray): UByteArray? {
//        var strBuilder = StringBuilder()
//        val read_data: UByteArray = data.toUByteArray()
//
//        if (read_data[0] == stx && read_data.last() == etx) {
//            val list: List<UByte> = read_data.toList()
//            val crc_list: UByteArray = list.subList(1, list.size - 3).toUByteArray()
//
//            crc16.update(crc_list)
//            var crc_data: ByteArray = crc16.ushort2ByteArray()
//            crc16.reset()
//            if (read_data[read_data.lastIndex - 2] == crc_data[1].toUByte() &&
//                read_data[read_data.lastIndex - 1] == crc_data[0].toUByte()
//            ) {
//                var get_data = list.subList(data_start_index, list.size - 3)
//
//                for (i in 0 until get_data.size!!) {
//                    strBuilder.append(String.format("%02X ", get_data[i].toByte()))
//                }
//                Log.d("Serial", "data: ${strBuilder.toString()}")
//
//            }
//        }
//        return null
//    }

    fun sendData(cmd: UByte) {
        var send_data = mutableListOf<UByte>()
        _cmd = cmd

        send_data.add(seq)
        send_data.add(sidx[0])
        send_data.add(sidx[1])
        send_data.add(ridx[0])
        send_data.add(ridx[1])
        send_data.add(cmd)
        send_data.add(0x00u)
        send_data.add(0x00u)

        crc16.update(send_data.toUByteArray())
        var crc_data = crc16.ushort2ByteArray()
        crc16.reset() // crc 초기화
        send_data.add(crc_data[1].toUByte())
        send_data.add(crc_data[0].toUByte())
        send_data.add(etx)
        send_data.add(0, stx)

        ser.writeData(send_data.toUByteArray())
    }

    private fun getDataCenter() {
        when (_cmd) {
            0xD6.toUByte() -> {
                DataCenter.rfid = DataCenter.RFID(
                    card_num = UByteArray(0), version = UByteArray(0),
                    operation_stop = true, operation_check = false
                )
            }
            0xD7.toUByte() -> {
                val version: UByteArray = read_data.subList(length + 1, length + 3).toUByteArray()
                DataCenter.rfid = DataCenter.RFID(
                    card_num = UByteArray(0), version = version,
                    operation_stop = false, operation_check = false
                )
            }
            0xD8.toUByte() -> {
                DataCenter.rfid = DataCenter.RFID(
                    card_num = UByteArray(0), version = UByteArray(0),
                    operation_stop = false, operation_check = true
                )
            }
            0xEA.toUByte() -> {
                val card_num: UByteArray = read_data.subList(length + 5, length + 13).toUByteArray()
                DataCenter.rfid = DataCenter.RFID(
                    card_num = card_num, version = UByteArray(0),
                    operation_stop = true, operation_check = false
                )
            }
            0xEB.toUByte() -> {
                val card_num: UByteArray = read_data.subList(length + 5, length + 13).toUByteArray()
                DataCenter.rfid = DataCenter.RFID(
                    card_num = card_num, version = UByteArray(0),
                    operation_stop = true, operation_check = false
                )
            }
        }
        read_data.clear()
    }
}