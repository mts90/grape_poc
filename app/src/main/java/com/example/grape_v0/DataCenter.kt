package com.example.grape_v0

object DataCenter {
    data class RFID(
        var card_num: UByteArray,
        var version: UByteArray,
        var operation_stop:Boolean,
        var operation_check:Boolean,
    )

    var rfid = RFID(card_num = UByteArray(0), version = UByteArray(0),
        operation_stop = false, operation_check = false)

}