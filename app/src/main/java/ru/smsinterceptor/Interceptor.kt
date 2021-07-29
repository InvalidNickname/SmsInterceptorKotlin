package ru.smsinterceptor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * BroadcastReceiver, перехватывающий приходящие SMS. По документации BroadcastReceiver не должен выполнять тяжелых операций,
 * поэтому он просто перехватывает сообщения, а вся обработка происходит в SendService
 */
class Interceptor : BroadcastReceiver() {
    private val action = "android.provider.Telephony.SMS_RECEIVED"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action != null && action.compareTo(intent.action!!, true) == 0) {
            val extras = intent.extras
            if (extras != null) {
                // извлекаем SMS в формате PDU
                val pduArray: Array<*>? = intent.extras?.get("pdus") as Array<*>?
                if (pduArray == null) {
                    Log.e("SmsInterceptor", "Null PDU received")
                    return
                }
                // конвертируем PDU в объект сообщений
                val messages = arrayOfNulls<SmsMessage>(pduArray.size)
                for (i in pduArray.indices) {
                    messages[i] = SmsMessage.createFromPdu(pduArray[i] as ByteArray)
                }
                if (messages.isNotEmpty()) {
                    val from = messages[0]?.displayOriginatingAddress

                    // определяем время сообщения
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = messages[0]?.timestampMillis!!
                    val simpleDateFormat = SimpleDateFormat("HH:mm:ss yyyy.MM.dd", Locale.US)
                    val timestamp = simpleDateFormat.format(calendar.time)

                    // собираем текст сообщения
                    val text = StringBuilder()
                    for (message in messages) {
                        text.append(message?.messageBody)
                    }
                    val body = text.toString()

                    // подготавливаем сервис для пересылки
                    val serviceIntent = Intent(context, SendService::class.java)
                    serviceIntent.putExtra("sms_body", body)
                    serviceIntent.putExtra("sms_from", from)
                    serviceIntent.putExtra("sms_time", timestamp)

                    // запускаем пересылку
                    context?.startService(serviceIntent)
                }
            }
        }
        abortBroadcast()
    }
}