package ru.smsinterceptor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.SystemClock
import androidx.room.Room
import ru.smsinterceptor.room.Database
import javax.mail.MessagingException


class AsyncSender : AsyncTask<Context, Void, Void>() {
    override fun doInBackground(vararg context: Context): Void? {
        val db = Room.databaseBuilder(context[0], Database::class.java, "messages").build()
        val messages = db.messageDao()?.all
        if (messages != null) {
            for (message in messages) {
                if (message != null) {
                    if (message.time <= System.currentTimeMillis()) {
                        val mailer = MailService(message.from, message.to, message.password, message.smsFrom, message.body)
                        try {
                            mailer.send()
                            db.messageDao()?.delete(message)
                        } catch (e: MessagingException) {
                            setUpDelayed(context[0], 10 * 60 * 1000) // через 10 минут
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        db.close()
        return null
    }

    private fun setUpDelayed(context: Context, delay: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        if (manager != null) {
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            alarmIntent.action = System.currentTimeMillis().toString()

            val pi = PendingIntent.getBroadcast(context, (Math.random() * 10000).toInt(), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val timeWoDelay = SystemClock.elapsedRealtime() + delay

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, timeWoDelay, pi);
            } else {
                manager.setExact(AlarmManager.ELAPSED_REALTIME, timeWoDelay, pi);
            }
        }
    }
}