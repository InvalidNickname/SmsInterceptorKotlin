package ru.smsinterceptor

import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


class MailService {
    private var to: String? = null
    private var subject: String? = null
    private var from: String? = null
    private var password: String? = null
    private var text: String? = null

    constructor(from: String?, to: String?, password: String?, subject: String?, text: String?) {
        this.text = text
        this.subject = subject
        this.from = from
        this.password = password
        this.to = to
    }

    @Throws(MessagingException::class)
    fun send() {
        val props = Properties()
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.user"] = from
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.port"] = "587"
        props["mail.mime.charset"] = "utf-8"
        val session: Session
        val auth: Authenticator = SMTPAuthenticator(from, password!!)
        session = Session.getDefaultInstance(props, auth)
        session.debug = true
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
        val mp: Multipart = MimeMultipart("related")
        val bodyMsg = MimeBodyPart()
        bodyMsg.setText(text)
        mp.addBodyPart(bodyMsg)
        msg.setContent(mp)
        Transport.send(msg)
    }

    private class SMTPAuthenticator(var from: String?, var password: String) : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(from, password)
        }
    }
}