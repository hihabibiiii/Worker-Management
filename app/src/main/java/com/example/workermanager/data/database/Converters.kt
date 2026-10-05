package com.example.workermanager.data.database

import androidx.room.TypeConverter
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.data.entity.PaymentMethod
import com.example.workermanager.data.entity.SiteStatus
import com.example.workermanager.data.entity.WorkerType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, formatter) }
    }

    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus): String {
        return value.name
    }

    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus {
        return AttendanceStatus.valueOf(value)
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String {
        return value.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod {
        return PaymentMethod.valueOf(value)
    }

    @TypeConverter
    fun fromSiteStatus(value: SiteStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSiteStatus(value: String): SiteStatus {
        return SiteStatus.valueOf(value)
    }

    @TypeConverter
    fun fromWorkerType(value: WorkerType): String {
        return value.name
    }

    @TypeConverter
    fun toWorkerType(value: String): WorkerType {
        return WorkerType.valueOf(value)
    }
}
