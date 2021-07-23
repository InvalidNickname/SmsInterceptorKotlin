package ru.smsinterceptor

import android.content.Context
import android.os.AsyncTask
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
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        db.close()
        return null
    }
}