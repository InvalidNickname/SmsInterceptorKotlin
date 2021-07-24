package ru.smsinterceptor.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Message::class], version = 1, exportSchema = false)
abstract class Database : RoomDatabase() {
    abstract fun messageDao(): MessageDao?
}