package ru.smsinterceptor

import android.content.Context
import android.os.AsyncTask
import androidx.room.Room
import ru.smsinterceptor.room.Database
import ru.smsinterceptor.room.Message

class AsyncDb(private var context: Context?, private var message: Message?) : AsyncTask<Void, Void, Void>() {
    override fun doInBackground(vararg p0: Void): Void? {
        val db = Room.databaseBuilder(context!!, Database::class.java, "messages").build()
        db.messageDao()?.insertAll(message)
        db.close()
        return null
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        AsyncSender().execute(context)
    }
}