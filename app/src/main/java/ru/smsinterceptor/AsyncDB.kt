package ru.smsinterceptor

import android.content.Context
import android.os.AsyncTask
import androidx.preference.PreferenceManager
import androidx.room.Room
import ru.smsinterceptor.room.Database
import ru.smsinterceptor.room.Message

class AsyncDb(private val message: Message?) : AsyncTask<Context, Void, Void>() {
    private lateinit var context: Context

    override fun doInBackground(vararg context: Context): Void? {
        this.context = context[0]
        val db = Room.databaseBuilder(this.context, Database::class.java, "messages").build()
        db.messageDao()?.insertAll(message)
        // увеличиваем счетчик размера базы
        val preferences = PreferenceManager.getDefaultSharedPreferences(context[0])
        val count = preferences.getInt("database_size", 0) + 1;
        preferences.edit().putInt("database_size", count).apply()
        db.close()
        return null
    }

    override fun onPostExecute(result: Void?) {
        AsyncSender().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, context)
        super.onPostExecute(result)
    }
}