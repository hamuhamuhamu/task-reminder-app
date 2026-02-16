package com.example.taskreminder.data

import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    fun observeTasks(): Flow<List<TaskEntity>> = db.taskDao().observeAll()

    fun observeItems(taskId: Long): Flow<List<ItemEntity>> = db.itemDao().observeByTask(taskId)

    fun observeSettings(): Flow<AppSettingsEntity?> = db.appSettingsDao().observe()
    suspend fun getSettings(): AppSettingsEntity? = db.appSettingsDao().get()

    fun observeItemStatusSummary(taskId: Long, today: LocalDate): Flow<List<ItemStatusSummary>> =
        db.dailyRecordDao().observeItemStatusSummary(taskId = taskId, today = today.toString())

    fun observeMonthlyRecords(
        taskId: Long,
        itemId: Long,
        monthStart: LocalDate,
        monthEnd: LocalDate
    ): Flow<List<DailyRecordEntity>> = db.dailyRecordDao().observeMonthlyRecords(
        taskId = taskId,
        itemId = itemId,
        fromDate = monthStart.toString(),
        toDate = monthEnd.toString()
    )

    suspend fun createTask(name: String, memo: String? = null, itemNames: List<String> = listOf("Bowel")) {
        val now = System.currentTimeMillis()
        val taskId = db.taskDao().insert(
            TaskEntity(
                name = name,
                memo = memo?.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now
            )
        )
        db.itemDao().insertAll(
            itemNames
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .ifEmpty { listOf("Bowel") }
                .mapIndexed { index, itemName ->
                    ItemEntity(
                        taskId = taskId,
                        name = itemName,
                        isDefault = index == 0,
                        sortOrder = index,
                        createdAt = now,
                        updatedAt = now
                    )
                }
        )
    }

    suspend fun updateTask(taskId: Long, name: String, memo: String?) {
        val current = db.taskDao().getById(taskId) ?: return
        db.taskDao().update(
            current.copy(
                name = name,
                memo = memo?.takeIf { it.isNotBlank() },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addItemToTask(taskId: Long, itemName: String) {
        val normalized = itemName.trim()
        if (normalized.isBlank()) return
        if (db.itemDao().findByName(taskId, normalized) != null) return
        val now = System.currentTimeMillis()
        val nextOrder = db.itemDao().getByTask(taskId).size
        db.itemDao().insert(
            ItemEntity(
                taskId = taskId,
                name = normalized,
                sortOrder = nextOrder,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun saveRecord(
        taskId: Long,
        itemId: Long,
        date: LocalDate,
        status: RecordStatus,
        source: RecordSource
    ) {
        db.dailyRecordDao().upsert(
            DailyRecordEntity(
                taskId = taskId,
                itemId = itemId,
                recordDate = date.toString(),
                status = status,
                source = source,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun ensureSettings() {
        if (db.appSettingsDao().get() == null) {
            db.appSettingsDao().upsert(
                AppSettingsEntity(
                    id = 1,
                    notificationEnabled = true,
                    notificationTime = "20:00",
                    timezone = ZoneId.systemDefault().id,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateSettings(enabled: Boolean, time: String, timezone: String = ZoneId.systemDefault().id) {
        ensureSettings()
        db.appSettingsDao().update(
            enabled = enabled,
            time = time,
            timezone = timezone,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun getReminderPayload(today: LocalDate): List<Pair<TaskEntity, List<ItemStatusSummary>>> {
        val taskList = db.taskDao().getAll()
        return taskList.map { task ->
            task to db.dailyRecordDao().getItemStatusSummary(task.id, today.toString())
        }
    }
}
