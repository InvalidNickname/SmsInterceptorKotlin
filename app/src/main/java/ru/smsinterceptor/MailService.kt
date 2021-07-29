package ru.smsinterceptor

import java.io.FileNotFoundException
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


/**
 * Класс, пересылающий e-mail сообщения на почтовый сервер goolge
 * @property from почта отправителя
 * @property to почта получателя
 * @property password пароль отправителя
 * @property subject тема письма
 * @property text текст письма
 */
class MailService(
    private var from: String,
    private var to: String,
    private var password: String,
    private var subject: String,
    private var text: String
) {
    /**
     * Отправить выбранное сообщение
     */
    @Throws(MessagingException::class, FileNotFoundException::class)
    fun send() {
        val props = Properties()
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.user"] = from
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.port"] = "587"
        props["mail.mime.charset"] = "utf-8"
        val auth = SMTPAuthenticator(from, password)
        val session = Session.getInstance(props, auth).apply { debug = true }
        // собираем сообщение
        val msg: Message = MimeMessage(session)
        try {
            msg.setFrom(InternetAddress("sms.interceptor", "sms.interceptor"))
        } catch (e: Exception) {
            msg.setFrom(InternetAddress("sms.interceptor"))
        }
        msg.sentDate = Calendar.getInstance().time
        val addressTo = InternetAddress.parse(to)
        msg.setRecipients(Message.RecipientType.TO, addressTo)
        msg.subject = subject
        val bodyMsg = MimeBodyPart().apply { setText(text) }
        val mp = MimeMultipart("related").apply { addBodyPart(bodyMsg) }
        msg.setContent(mp)
        // отправляем
        Transport.send(msg)
    }

    /**
     * Простой аутентификатор по логину/паролю
     * @property from почта от Google аккаунта
     * @property password пароль от Google аккаунта
     */
    private class SMTPAuthenticator(var from: String?, var password: String) : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(from, password)
        }
    }
}