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


/**
 * Сервис для регистрации Interceptor. Т.к. он ловит действие android.provider.Telephony.SMS_RECEIVED, ему необходимв постоянная
 * регистрация в сервисе или активности. Цель этого сервиса - оставаться запущенным все время для регистрации Interceptor
 */
class MonitorService : Service() {
    private lateinit var smsReceiver: BroadcastReceiver

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // создаем фильтр для регистрации
        val intentFilter = IntentFilter()
        // максимальный приоритет
        intentFilter.priority = 2147483647
        intentFilter.addAction("android.intent.action.PHONE_STATE")
        smsReceiver = Interceptor()
        // регистрируем Interceptor
        registerReceiver(smsReceiver, intentFilter)
        // создаем уведомление, необходимое для перевода сервиса в foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // уведомления нового типа
            startForegroundNew()
        } else {
            // уведомления старого типа
            val notificationBuilder = NotificationCompat.Builder(this, "ru.smsinterceptor")
            val notification = notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_title))
                .build()
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }

    /**
     * Создание уведомления нового типа
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun startForegroundNew() {
        val chan = NotificationChannel("ru.smsinterceptor", "interceptor-background", NotificationManager.IMPORTANCE_NONE)
        // синенький светодиодик :3
        chan.lightColor = Color.BLUE
        // уведомление не должно отображатьсян на экране блокировки
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        if (manager != null) {
            manager.createNotificationChannel(chan)
            val notificationBuilder = NotificationCompat.Builder(this, "ru.smsinterceptor")
            val notification = notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_title))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            startForeground(2, notification)
        }
    }
}