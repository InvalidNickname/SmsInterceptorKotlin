package ru.smsinterceptor.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Класс сообщения для базы данных
 * @property from почта отправителя
 * @property to почта получателя
 * @property password пароль отправителя
 * @property smsFrom номер или имя отправителя SMS
 * @property body текст сообщения
 * @property time время, когда надо отправить это сообщение
 */
@Entity
class Message(
    @field:ColumnInfo(name = "from") var from: String,
    @field:ColumnInfo(name = "to") var to: String,
    @field:ColumnInfo(name = "password") var password: String,
    @field:ColumnInfo(name = "sms_from") var smsFrom: String,
    @field:ColumnInfo(name = "body") var body: String,
    @field:ColumnInfo(name = "time") var time: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0
}