package ru.smsinterceptor

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.room.Room
import kotlinx.coroutines.*
import ru.smsinterceptor.room.Database
import ru.smsinterceptor.room.Message

/**
 * Класс для асинронного добавления записей-сообщений в базу данных
 * @property message сообщение, которое необходимо добавить
 */
class AsyncDb(private val message: Message?) {
    /**
     * Добавить выбранное сообщение в базу
     * @param context контект приложения
     */
    fun execute(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val job = GlobalScope.async {
                val db = Room.databaseBuilder(context, Database::class.java, "messages").build()
                db.messageDao()?.insertAll(message)
                // увеличиваем счетчик размера базы
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                val count = preferences.getInt("database_size", 0) + 1
                preferences.edit().putInt("database_size", count).apply()
                db.close()
            }
            job.await()
            // имитация executeOnExecutor из AsyncTask-а
            withContext(Dispatchers.Main) {
                AsyncSender().execute(context)
            }
        }
    }
}