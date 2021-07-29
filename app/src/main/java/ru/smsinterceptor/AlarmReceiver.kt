package ru.smsinterceptor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver, созданный для перезапуска AsyncSender. При вызове запускает AsyncSender
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            AsyncSender().execute(context!!)
        }
    }
}