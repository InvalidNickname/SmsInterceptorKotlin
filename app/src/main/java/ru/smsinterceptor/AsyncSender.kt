package ru.smsinterceptor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.preference.PreferenceManager
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.smsinterceptor.room.Database
import java.io.FileNotFoundException
import javax.mail.MessagingException


class AsyncSender {
    fun execute(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            // если уже запущен, ставим флаг, что появилась новая инфа и вырубаемся
            if (preferences.getBoolean("sender_status", false)) {
                preferences.edit().putBoolean("sender_more", true).apply()
                return@launch
            }
            // ставим флаг, что таск запущен и обрабатывает инфу
            preferences.edit().putBoolean("sender_status", true).apply()
            // установлен таймер на следующий вызов таска
            var timerSet = false
            do {
                // ставим флаг, что доп инфы нет
                preferences.edit().putBoolean("sender_more", false).apply()
                val db = Room.databaseBuilder(context, Database::class.java, "messages").build()
                val messages = db.messageDao()?.all?.reversed()
                if (messages != null) {
                    for (message in messages) {
                        if (message != null) {
                            if (message.time <= System.currentTimeMillis()) {
                                val mailer = MailService(message.from, message.to, message.password, message.smsFrom, message.body)
                                try {
                                    mailer.send()
                                    db.messageDao()?.delete(message)
                                    val count = preferences.getInt("database_size", 1) - 1
                                    preferences.edit().putInt("database_size", count).apply()
                                } catch (e: MessagingException) {
                                    val error = e.toString()
                                    if (error.contains("AuthenticationFailedException")) {
                                        db.messageDao()?.delete(message)
                                        val count = preferences.getInt("database_size", 1) - 1
                                        preferences.edit().putInt("database_size", count).apply()
                                    } else {
                                        if (!timerSet) {
                                            setUpDelayed(context, 10 * 60 * 1000) // через 10 минут
                                            timerSet = true
                                        }
                                    }
                                    e.printStackTrace()
                                } catch (e: FileNotFoundException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
                db.close()
            } while (preferences.getBoolean("sender_more", false)) // обрабатываем, пока есть еще инфа
            // заканчиваем обработку
            preferences.edit().putBoolean("sender_status", false).apply()
        }
    }

    private fun setUpDelayed(context: Context, delay: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        if (manager != null) {
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            alarmIntent.action = System.currentTimeMillis().toString()

            val pi = PendingIntent.getBroadcast(context, (Math.random() * 10000).toInt(), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val timeWoDelay = SystemClock.elapsedRealtime() + delay

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, timeWoDelay, pi)
            } else {
                manager.setExact(AlarmManager.ELAPSED_REALTIME, timeWoDelay, pi)
            }
        }
    }
}