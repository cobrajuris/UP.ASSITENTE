package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.EmailStatus
import com.example.data.model.PriorityLevel

class Converters {
    @TypeConverter
    fun fromPriority(value: PriorityLevel): String = value.name

    @TypeConverter
    fun toPriority(value: String): PriorityLevel = runCatching {
        PriorityLevel.valueOf(value)
    }.getOrDefault(PriorityLevel.MEDIUM)

    @TypeConverter
    fun fromEmailStatus(value: EmailStatus): String = value.name

    @TypeConverter
    fun toEmailStatus(value: String): EmailStatus = runCatching {
        EmailStatus.valueOf(value)
    }.getOrDefault(EmailStatus.UNREAD)
}
