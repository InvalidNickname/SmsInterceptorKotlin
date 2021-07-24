package ru.smsinterceptor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class MonitorService : Service() {
    private lateinit var smsReceiver: BroadcastReceiver

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val intentFilter = IntentFilter()
        intentFilter.priority = 2147483647
        intentFilter.addAction("android.intent.action.PHONE_STATE")
        smsReceiver = Interceptor()
        registerReceiver(smsReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundNew()
        } else {
            val notificationBuilder = NotificationCompat.Builder(this, "ru.smsinterceptor")
            val notification = notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Перехватчик СМС работает в фоновом режиме")
                .build()
            startForeground(1, notification)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun startForegroundNew() {
        val chan = NotificationChannel("ru.smsinterceptor", "interceptor-background", NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        if (manager != null) {
            manager.createNotificationChannel(chan)
            val notificationBuilder = NotificationCompat.Builder(this, "ru.smsinterceptor")
            val notification = notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Перехватчик СМС работает в фоновом режиме")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            startForeground(2, notification)
        }
    }
}