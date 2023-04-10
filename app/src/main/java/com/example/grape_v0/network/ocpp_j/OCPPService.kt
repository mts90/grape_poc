package com.example.grape_v0.network.ocpp_j

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Base64
import android.util.Log
import com.example.grape_v0.DataCenter
import com.example.grape_v0.network.ocpp_j.WebSocketManager
import eu.chargetime.ocpp.feature.profile.ClientCoreEventHandler
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile
import eu.chargetime.ocpp.model.core.*
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.timer


class OCPPService : Service() {
    // OCPP 전용 라이브러리 다운로드 필요함.
    // https://github.com/ChargeTimeEU/Java-OCA-OCPP
    // keystore 생성방법
    // https://developer.android.com/studio/publish/app-signing?hl=ko#sign-apk
    var lastPosting: Long = 0
    val POST_INTERVAL: Long = 60

    var myClient = OkHttpClient()
    lateinit var myCore: ClientCoreProfile
    // config로 받아야함.
//    var url: String = "wss://ocpp.evsquare.io/ocpp/"
    // 메니페스트에 android:usesCleartextTraffic="true" 해야지 http를 사용할 수 있음.
    // 위에 wss 주소 사용하려면, 메니페스트에 꼭 위 세팅값 삭제해줄것.

    var url: String = "ws://dev.evarcall.me.uk:11503/ocpp/"
    val myListener : webSocketListener = webSocketListener()

    //    lateinit var myRequest: Request
    val ws : WebSocketManager = WebSocketManager

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
        Log.d("ocpp", "OCPP!!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Log", "ocpp Service Start")
        setOcppCore()
        setWebSocket()

        timer(period = 1000, initialDelay = 1000)
        {
            jobAction()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d("Log", "Service Stop")
        WebSocketManager.close()

        super.onDestroy()
    }

    private fun nowstr():String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val formatted = current.format(formatter)

        return formatted
    }

    private fun jobAction(){
        val now = Date().time
        if(now - lastPosting >= POST_INTERVAL * 1000) {
            lastPosting = now
            sendBootNotification()
//            sendStatusNoticication()
//            sendHearbeat()
//            sendAuthorize()
        }
    }

    private fun setWebSocket(){
        // config로 받아야함.
        val chargerID = "001GRAPE_A001"
        // config로 받아야함.
        val TOK = "3c9b90d7b54a11ec903f"
        url += chargerID

        val cvtBase64 = "${chargerID}:" + "$TOK"
        val authHeader = "basic " +  Base64.encodeToString(cvtBase64.toByteArray(), Base64.DEFAULT).replace("\n", "")

        WebSocketManager.init(url, authHeader, myListener)

        WebSocketManager.connect()
    }

    private fun setOcppCore(){
        // The core profile is mandatory
        myCore = ClientCoreProfile(object : ClientCoreEventHandler {
            override fun handleChangeAvailabilityRequest(request: ChangeAvailabilityRequest): ChangeAvailabilityConfirmation {
                println(request)
                // ... handle event
                return ChangeAvailabilityConfirmation(AvailabilityStatus.Accepted)
            }

            override fun handleGetConfigurationRequest(request: GetConfigurationRequest): GetConfigurationConfirmation? {
                println(request)
                // ... handle event
                return null // returning null means unsupported feature
            }

            override fun handleChangeConfigurationRequest(request: ChangeConfigurationRequest): ChangeConfigurationConfirmation? {
                println(request)
                // ... handle event
                return null // returning null means unsupported feature
            }

            override fun handleClearCacheRequest(request: ClearCacheRequest): ClearCacheConfirmation? {
                println(request)
                // ... handle event
                return null // returning null means unsupported feature
            }

            override fun handleDataTransferRequest(request: DataTransferRequest): DataTransferConfirmation? {
                println(request)
                // ... handle event
                return null // returning null means unsupported feature
            }

            override fun handleRemoteStartTransactionRequest(request: RemoteStartTransactionRequest): RemoteStartTransactionConfirmation? {
                println(request)
                // ... handle event
                return null // returning null means unsupported feature
            }

            override fun handleRemoteStopTransactionRequest(request: RemoteStopTransactionRequest): RemoteStopTransactionConfirmation? {
                println(request)
                // ... handle event
                return null // returning null means unsupported feature
            }

            override fun handleResetRequest(request: ResetRequest): ResetConfirmation? {
                println(request)
                // ... handle event
                return null // returning null means unsupported feature
            }

            override fun handleUnlockConnectorRequest(request: UnlockConnectorRequest): UnlockConnectorConfirmation? {
                println(request)
                // ... handle event
                return null // returning null means unsupported feature
            }
        })
    }

    private fun setPayLoad(action_name: String, send_data: JSONObject):String{
        val ID: Int = 2
        val random_uuid = UUID.randomUUID()
        var pay_load = JSONArray()
        pay_load.put(ID)
        pay_load.put(random_uuid)
        pay_load.put(action_name)
        pay_load.put(send_data)

        return pay_load.toString()

    }

    private fun sendBootNotification(){
        // Use the feature profile to help create event
        // config로 받아야함.
        var retVal = myCore.createBootNotificationRequest("EVAR", "ELA007C01")
        val params = HashMap<String, Any>()
        val action_name = "BootNotification"

        // using like this that setting an any Values
        // config로 받아야함.
        val strB = StringBuilder()
        for (i in 0 until DataCenter.rfid.card_num.size) {
            strB.append(String.format("%02X ", DataCenter.rfid.card_num[i].toByte()))
        }
        retVal.firmwareVersion = strB.toString()

        params["chargePointVendor"] = retVal.chargePointVendor
        params["chargePointModel"] = retVal.chargePointModel
        params["firmwareVersion"] = retVal.firmwareVersion

        lateinit var payLoad: String

        payLoad = setPayLoad(action_name, JSONObject(params as Map<*, *>?))

        doSendOcppPayload(payLoad)
    }

    private fun sendStatusNoticication(){
        // Use the feature profile to help create event
        // config로 받아야함.
        val _connector_id = 1
        val _error_code = ChargePointErrorCode.NoError
        val _status = ChargePointStatus.Available

        var retVal = myCore.createStatusNotificationRequest(_connector_id, _error_code, _status)
        val params = HashMap<String, Any>()
        val action_name = "StatusNotification"

        Log.d("retVal", "$retVal")

        params["connectorId"] = retVal.connectorId
        params["errorCode"] = retVal.errorCode.toString()
        params["info"] = ""
        params["status"] = retVal.status.toString()
        params["timeStamp"] = nowstr()
        params["vendorErrorCode"] = ""
        params["vendorId"] = ""

        lateinit var payLoad: String

        payLoad = setPayLoad(action_name, JSONObject(params as Map<*, *>?))

        doSendOcppPayload(payLoad)
    }

    private fun sendAuthorize(){
        // Use the feature profile to help create event
        // config로 받아야함.
        val _id_tag = "lyan"

        var retVal = myCore.createAuthorizeRequest(_id_tag)
        val params = HashMap<String, Any>()
        val action_name = "Authorize"

        Log.d("retVal", "$retVal")

        params["idTag"] = retVal.idTag

        lateinit var payLoad: String

        payLoad = setPayLoad(action_name, JSONObject(params as Map<*, *>?))

        doSendOcppPayload(payLoad)
    }

    private fun sendHearbeat(){
        // Use the feature profile to help create event
        // config로 받아야함.

        var retVal = myCore.createHeartbeatRequest()
        val params = HashMap<String, Any>()
        val action_name = "Heartbeat"

        Log.d("retVal", "$retVal")

        lateinit var payLoad: String

        payLoad = setPayLoad(action_name, JSONObject(params as Map<*, *>?))

        doSendOcppPayload(payLoad)
    }

    private fun doSendOcppPayload(payLoad: String){
        Log.d("ocpp Send Data Payload", "$payLoad")
        WebSocketManager.sendMessage(payLoad)
    }


    class webSocketListener : WebSocketListener() {
        fun onConnectSuccess(){
            Log.d("My WebSocketListener","Connect Success!!")
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)

        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d("My WebSocketListener","Receiving bytes: $bytes")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("My WebSocketListener", "Receiving : $text")
            // 보낸 액션별 해야하는 시나리오 정의
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("My WebSocketListener","Closing : $code / $reason")
            webSocket.close(NORMAL_CLOSURE_STATUS, null)
            webSocket.cancel()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d("My WebSocketListener","Error : " + t.message)
        }

        companion object {
            private const val NORMAL_CLOSURE_STATUS = 1000
        }
    }

}