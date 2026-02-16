package com.example.taskreminder.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromRecordStatus(value: RecordStatus?): String? = value?.name

    @TypeConverter
    fun toRecordStatus(value: String?): RecordStatus? = value?.let { RecordStatus.valueOf(it) }

    @TypeConverter
    fun fromRecordSource(value: RecordSource?): String? = value?.name

    @TypeConverter
    fun toRecordSource(value: String?): RecordSource? = value?.let { RecordSource.valueOf(it) }
}
