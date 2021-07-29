package ru.smsinterceptor.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/**
 * Объект доступа к базе данных
 */
@Dao
interface MessageDao {
    /**
     * Список всех сообщений в базе
     */
    @get:Query("SELECT * FROM message")
    val all: List<Message?>?

    /**
     * Добавляет заданные сообщения в базу
     * @param messages сообщения для занесения в базу
     */
    @Insert
    suspend fun insertAll(vararg messages: Message?)

    /**
     * Удаляет заданные сообщения из базы
     * @param messages сообщения для удаления из базы
     */
    @Delete
    suspend fun delete(vararg messages: Message?)
}