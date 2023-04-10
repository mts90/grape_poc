package com.example.grape_v0

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android_serialport_api.SerialPort
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.grape_v0.network.PostService
import com.example.grape_v0.network.ocpp_j.OCPPService
import com.example.grape_v0.serial.RFID
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    val SERIAL_PORT_NAME = "ttyS9"
    val SERIAL_BAUDRATE = 115200
    var serialPort: SerialPort? = null
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    lateinit var serialThread: Thread
    lateinit var mytv: TextView
    val rfid = RFID(SERIAL_PORT_NAME, SERIAL_BAUDRATE)
    val testThreadRfid = Thread(rfid)

    companion object {
        var liveText: MutableLiveData<String> = MutableLiveData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        serviceStart()

        rfid.ser.openSerialPort()
        mytv = findViewById(R.id.dataView)
        testThreadRfid.start()

        liveText.observe(this, Observer {
            // it로 넘어오는 param은 LiveData의 value
            mytv.text = it
            println("observe")
        })

        val btn_event = findViewById<Button>(R.id.button)
        btn_event.setOnClickListener {
            rfid.sendData(0xD6u)
//            liveText.value = DataCenter.rfid.operation_stop.toString()
        }

        val btn_event2 = findViewById<Button>(R.id.button2)
        btn_event2.setOnClickListener {
            rfid.sendData(0xD7u)
//            liveText.value = DataCenter.rfid.version.toString()
        }

        val btn_event3 = findViewById<Button>(R.id.button3)
        btn_event3.setOnClickListener {
            rfid.sendData(0xD8u)
//            liveText.value = DataCenter.rfid.operation_check.toString()
        }

        val btn_event4 = findViewById<Button>(R.id.button4)
        btn_event4.setOnClickListener {
            rfid.sendData(0xEAu)
//            var strBuilder = StringBuilder()
//            for (i in 0 until DataCenter.rfid.card_num.size) {
//                strBuilder.append(String.format("%02X ", DataCenter.rfid.card_num[i].toByte()))
//            }
//            liveText.value = strBuilder.toString()
//            liveText.value = DataCenter.rfid.card_num.toString()
        }

        val btn_event5 = findViewById<Button>(R.id.button5)
        btn_event5.setOnClickListener {
            rfid.sendData(0xEBu)
//            liveText.value = DataCenter.rfid.card_num.toString()
        }
    }

    public fun serviceStart() {
        var intent = Intent(this, PostService::class.java)
        startService(intent)

        intent = Intent(this, OCPPService::class.java)
        startService(intent)
    }

    public fun serviceStop() {
        var intent = Intent(this, PostService::class.java)
        stopService(intent)

        intent = Intent(this, OCPPService::class.java)
        stopService(intent)
    }
}