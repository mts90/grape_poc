package com.example.grape_v0.serial

import android.util.Log
import android_serialport_api.SerialPort
import android_serialport_api.SerialPortFinder
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class Serial(dev: String, baud_rate: Int, endian: Boolean) {
    init {
        println("dev : $dev, baudrate : $baud_rate")
    }

    val port_name = dev
    val seria_baud_rate = baud_rate
    var serial_port: SerialPort? = null
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null

    fun openSerialPort() {
        val serialPortFinder: SerialPortFinder = SerialPortFinder()
        val devices: Array<String> = serialPortFinder.allDevices
        val devicesPath: Array<String> = serialPortFinder.allDevicesPath

        for (device in devices) {
            if (device.contains(port_name, true)) {
                val index = devices.indexOf(device)
                serial_port = SerialPort(File(devicesPath.get(index)), seria_baud_rate, 0)
                break
            }
        }
        serial_port?.let {
            inputStream = it.inputStream
            outputStream = it.outputStream
        }
    }

    fun readData(): UByte? {
        if (inputStream == null) {
            Log.e("Serial", "Can't open inputstream")
            return null
        }
        try {
            var buffer: Int? = null
            buffer = inputStream?.read()
//            var strBuilder = StringBuilder()
//            strBuilder.append(String.format("%02X ", buffer))
//            Log.d("Serial", "rx: ${strBuilder.toString()}")
            return buffer?.toUByte()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun readData(len: Int): ByteArray? {
        if (inputStream == null) {
            Log.e("Serial", "Can't open inputstream")
            return null
        }
        try {
            val buffer: ByteArray = ByteArray(len)
            val index: Int = inputStream!!.read(buffer, 0, len)
//            var strBuilder = StringBuilder()
//            for (i in 0 until index) {
//                strBuilder.append(String.format("%02X ", buffer[i]))
//            }
//            Log.d("Serial", "rx: ${strBuilder.toString()}")
            return buffer

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun writeData(data: UByteArray) {
        try {
            outputStream?.write(data.toByteArray())
//            var strBuilder = StringBuilder()
//            for (i in 0 until data.size) {
//                strBuilder.append(String.format("%02X ", data[i].toByte()))
//            }
//            Log.d("Serial", "tx: ${strBuilder.toString()}")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}