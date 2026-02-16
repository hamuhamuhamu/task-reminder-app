package com.example.taskreminder.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RecordStatus {
    YES,
    NO,
    UNSET
}

enum class RecordSource {
    APP,
    NOTIFICATION
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val memo: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("task_id")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    val name: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "daily_records",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["task_id", "item_id", "record_date"], unique = true),
        Index(value = ["item_id"]),
        Index(value = ["record_date"])
    ]
)
data class DailyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "record_date") val recordDate: String,
    val status: RecordStatus,
    val source: RecordSource,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "notification_enabled") val notificationEnabled: Boolean = true,
    @ColumnInfo(name = "notification_time") val notificationTime: String = "20:00",
    val timezone: String = "Asia/Tokyo",
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

data class ItemStatusSummary(
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "item_name") val itemName: String,
    @ColumnInfo(name = "last_yes_date") val lastYesDate: String?,
    @ColumnInfo(name = "days_since_last_yes") val daysSinceLastYes: Int?,
    @ColumnInfo(name = "today_status") val todayStatus: RecordStatus?
)
