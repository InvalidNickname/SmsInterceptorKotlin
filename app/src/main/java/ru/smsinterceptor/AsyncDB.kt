package ru.smsinterceptor

import android.content.Context
import android.os.AsyncTask
import androidx.room.Room
import ru.smsinterceptor.room.Database
import ru.smsinterceptor.room.Message

class AsyncDb(private var message: Message?) : AsyncTask<Context, Void, Void>() {
    private lateinit var context: Context

    override fun doInBackground(vararg context: Context): Void? {
        this.context = context[0]
        val db = Room.databaseBuilder(this.context, Database::class.java, "messages").build()
        db.messageDao()?.insertAll(message)
        db.close()
        return null
    }

    override fun onPostExecute(result: Void?) {
        AsyncSender().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, context)
        super.onPostExecute(result)
    }
}