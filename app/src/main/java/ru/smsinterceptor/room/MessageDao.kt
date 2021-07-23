package ru.smsinterceptor.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @get:Query("SELECT * FROM message")
    val all: List<Message?>?

    @Insert
    fun insertAll(vararg messages: Message?)

    @Delete
    fun delete(vararg messages: Message?)
}