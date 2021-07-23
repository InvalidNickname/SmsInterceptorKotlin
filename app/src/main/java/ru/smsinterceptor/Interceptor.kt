package ru.smsinterceptor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class Interceptor : BroadcastReceiver() {
    val ACTION = "android.provider.Telephony.SMS_RECEIVED"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action != null && ACTION.compareTo(intent.action!!, true) == 0) {
            val extras = intent.extras
            if (extras != null) {
                val pduArray: Array<*>? = intent.extras?.get("pdus") as Array<*>?
                if (pduArray == null) {
                    Log.e("SmsInterceptor", "Null PDU received");
                    return
                }
                val messages = arrayOfNulls<SmsMessage>(pduArray.size)
                for (i in pduArray.indices) {
                    messages[i] = SmsMessage.createFromPdu(pduArray[i] as ByteArray)
                }
                if (messages.isNotEmpty()) {
                    val from = messages[0]?.displayOriginatingAddress

                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = messages[0]?.timestampMillis!!
                    val simpleDateFormat = SimpleDateFormat("HH:mm:ss yyyy.MM.dd", Locale.US)
                    val timestamp = simpleDateFormat.format(calendar.time)

                    val text = StringBuilder()
                    for (message in messages) {
                        text.append(message?.messageBody)
                    }
                    val body = text.toString()

                    val serviceIntent = Intent(context, SendService::class.java)
                    serviceIntent.putExtra("sms_body", body)
                    serviceIntent.putExtra("sms_from", from)
                    serviceIntent.putExtra("sms_time", timestamp)

                    context?.startService(serviceIntent)
                }
            }
        }
        abortBroadcast()
    }
}