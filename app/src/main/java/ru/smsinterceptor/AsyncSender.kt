package ru.smsinterceptor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.smsinterceptor.room.Database
import java.io.FileNotFoundException
import javax.mail.MessagingException

/**
 * Отвечает за пересылку сообщений по e-mail
 */
class AsyncSender {

    /**
     * Worker для пересылки. Т.к. в одно время может пересылаться только одно сообщение, пересылать его может только
     * один (первый запущенный) экземпляр AsyncSender/SenderWorker. Этот SenderWorker проверяет базу и достает из нее
     * все доступные сообщения для пересылки. Если во время пересылки появляются новые сообщения в базе, ставится флаг
     * sender_more, при котором после окончания пересылки всех имеющихся в базе сообщений SenderWorker повторяет запрос.
     * Все AsyncSender, запущенные при работающем первом экземпляре, ставят флаг sender_more и останавливаются.
     */
    class SenderWorker(private var context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
        override fun doWork(): Result {
            GlobalScope.launch(Dispatchers.IO) {
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                // если уже запущен, ставим флаг, что появились новые сообщения и вырубаемся
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
                    // запрос к базе, отправляем сообщения, начиная с самых старых
                    val db = Room.databaseBuilder(context, Database::class.java, "messages").build()
                    val messages = db.messageDao()?.all?.reversed()
                    if (messages != null) {
                        for (message in messages) {
                            if (message != null) {
                                // проверяем, надо ли отправлять сообщение с учетом задержки
                                if (message.time <= System.currentTimeMillis()) {
                                    val mailer = MailService(message.from, message.to, message.password, message.smsFrom, message.body)
                                    try {
                                        // отправляем
                                        mailer.send()
                                        db.messageDao()?.delete(message)
                                        val count = preferences.getInt("database_size", 1) - 1
                                        preferences.edit().putInt("database_size", count).apply()
                                    } catch (e: MessagingException) {
                                        val error = e.toString()
                                        if (error.contains("AuthenticationFailedException")) {
                                            // если ошибка аутентификации - удаляем сообщение, все равно его отправить не получится
                                            db.messageDao()?.delete(message)
                                            val count = preferences.getInt("database_size", 1) - 1
                                            preferences.edit().putInt("database_size", count).apply()
                                        } else {
                                            // в другом случае просто ставим таймер на 10 минут и потом пытаемся снова
                                            if (!timerSet) {
                                                setUpDelayed(context, 10 * 60 * 1000) // через 10 минут
                                                timerSet = true
                                            }
                                        }
                                        e.printStackTrace()
                                    } catch (e: FileNotFoundException) {
                                        // редкая ошибка с несовместимостью библиотек, если выпала - ничего не поделать
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                    db.close()
                } while (preferences.getBoolean("sender_more", false)) // обрабатываем, пока есть еще сообщения
                // заканчиваем обработку
                preferences.edit().putBoolean("sender_status", false).apply()
            }
            return Result.success()
        }

        /**
         * Устанавливает таймер на delay для повторного вызова AsyncSender
         * @param context контекст, необходим для запуска таймера
         * @param delay время задержки таймера
         */
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

    /**
     * Запускает пересылку всех имеющихся в базе сообщений
     * @param context контекст приложения
     */
    fun execute(context: Context) {
        val sender = OneTimeWorkRequestBuilder<SenderWorker>().build()
        WorkManager.getInstance(context).enqueue(sender)
    }
}