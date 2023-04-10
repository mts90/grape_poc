package com.example.grape_v0.network

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.timer


class PostService : Service() {
    // data Center에서 가져와야함.
    val db_ess = mutableMapOf<String, Any>(
        "checkTime" to 1,
        Pair("packVoltage", 543.2),
        "ESSSOC" to 48.5,
        "packAmphere" to 70.6,
        "coolerStatus" to true,
        "heaterStatus" to false,
    )

    // 보내는 POST 데이터가 추가될 수 있으므로 어레이나 리스트 형태트 받아야함.
    val post_ess_data = arrayListOf<Any>()
    val post_pmodule_data = arrayListOf<Any>()
    var EvarURL = "https://ocpp.evsquare.io/vmc/"
    val Token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiJldmFyY2Fycm90IiwiaWF0IjoxNjQzMzUxMzIzLCJleHAiOjE4MDExMzkzMjMsImlzcyI6ImV2YXIuaW5jIn0.QD5uart0l0Rkd2qbukx3P1pKSq8fc4qZPpjCV532KsU"

    var lastPosting: Long = 0
    val POST_INTERVAL: Long = 60

    // config는 나중에 config 파일에서 가져올거임.
    val config: List<Boolean> = listOf(true, true, false)

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Log", "Service Start")

        timer(period = 1000, initialDelay = 1000)
        {
            jobAction()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d("Log", "Service Stop")

        super.onDestroy()
    }

    private fun nowstr():String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val formatted = current.format(formatter)

        return formatted
    }

    private fun doPostData(name: String, content: Any?){
        val httpBuilder = Uri.Builder()
        val client = OkHttpClient()
        val request = Request.Builder()
        val params = HashMap<String, Any>()
        var url: String = ""

        request.addHeader(
            "Content-Type",
            "application/json;charset=utf-8"
        ) // [헤더 추가]
        request.addHeader(
            "evardata-access-token",
            Token
        ) // [헤더 추가]

        // config 파일에서 가져와야함.
        params["sid"] = "EVATEST"
        params["cid"] = "001EVATEST014"

        when(name){
            "ess_data" -> {
                url = EvarURL + "ess"
                params[name] = post_ess_data


//                Log.d("Lyan data","$myBody, \nlist: $post_ess_data")
            }
            "pmodule_data" -> {
                url = EvarURL + "pmodule"
                params[name] = post_pmodule_data

//                Log.d("Lyan data","$myBody, \nlist: $post_pmodule_data")
            }
        }
        var myBody = JSONObject(params as Map<*, *>?).toString()

        request.url(url + httpBuilder.toString())
        request.post(myBody.toRequestBody())

        client.newCall(request.build()).enqueue(object : Callback {
            // TODO [응답을 받은 경우]
            override fun onResponse(call: okhttp3.Call, response: Response) {
                val responseBody = response.body?.string()
                Log.i("","\n"+"[Test_Kotlin > testMain() 메소드 : OK HTTP 응답 확인]")
//                Log.i("","\n"+"[responseStatusCode : "+ response.code +"]")
//                //Log.i("","\n"+"[responseHeader : "+ response.headers +"]")
//                Log.i("","\n"+"[responseBodyData : "+ responseBody +"]")

                post_ess_data.clear()
                post_pmodule_data.clear()
            }

            // TODO [응답을 받지 못한 경우]
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.i("","\n"+"[Test_Kotlin > testMain() 메소드 : OK HTTP 요청 실패]")
                Log.i("","\n"+"[error : "+ e.message +"]")
            }
        })
    }

    private fun getSoftwareName():String {
        val boardVersion = "TEST_Harware"
        val protocolVersion = "TEST_Firmware"
        return ('b' + boardVersion + 'p' + protocolVersion)
    }

    private fun doCollectPmoduleData(){
        // Map 데이터 JSON 오브젝트화
        val jsonObject = JSONObject()
        // 겹치지 않는건 직접 넣어도 된다.
        jsonObject.put("checkTime", nowstr())
        jsonObject.put("relayStatus", 1)
        jsonObject.put("firmVer", getSoftwareName())
        jsonObject.put("firmVer", getSoftwareName())


        post_pmodule_data.add(jsonObject)
    }

    private fun doCollectEssData(){
        // Map 데이터 JSON 오브젝트화
        val jsonObject = JSONObject()
        // 겹치지 않는건 직접 넣어도 된다.

//        for (key in db_ess.keys){
//            jsonObject.put(key, db_ess[key])
//        }
        jsonObject.put("checkTime", nowstr())
        jsonObject.put("packVoltage", db_ess["packVoltage"])
        jsonObject.put("packAmphere", db_ess["packAmphere"])
        jsonObject.put("coolerStatus", db_ess["coolerStatus"])
        jsonObject.put("heaterStatus", db_ess["heaterStatus"])
        jsonObject.put("ESSSOC", db_ess["ESSSOC"])


        post_ess_data.add(jsonObject)
    }

    private fun doPost() {
        if (config[0]) {
            doPostData("ess_data", post_ess_data)
        }
        if (config[1]) {
            doPostData("pmodule_data", post_pmodule_data)
        }
//        if (config[2]) {
//            doPostData("cart_data", post_cart_data)
//        }

    }

    private fun collectData(){
        if (config[0]) {
            doCollectEssData()
        }
        if (config[1]) {
            doCollectPmoduleData()
        }
//        if (config[2]) {
//            doPostData("cart_data", post_cart_data)
//        }

    }

    private fun needCollect():Boolean {
        // fault 있는지 체크

        // 시간 지났는지 확인
        if(Date().time - lastPosting >= POST_INTERVAL * 1000) return true

        return false
    }

    private fun jobAction(){
        if (needCollect())
        {
            collectData()
        }

        val now = Date().time
        if(now - lastPosting >= POST_INTERVAL * 1000){
            lastPosting = now
            doPost()

        }
    }

    inner class NetworkThread: Thread() {
        // 요놈을 서비스로 등록을 해야 의미가 있는데.....
        // 쓰레드 끊기는 이유? 인터넷 연결 함수(isNetworkAvailable)에서 뭐가 문제가 있는 것 같음.
        override fun run() {
            var i: Int = 0
            while (true) {
                try {
                    sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                i = (i + 1) % 65536

//                db_ess["checkTime"] = i

                jobAction()
            }
        }
    }
}