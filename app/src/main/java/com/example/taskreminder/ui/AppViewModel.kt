package com.example.taskreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskreminder.data.AppDatabase
import com.example.taskreminder.data.AppRepository
import com.example.taskreminder.data.AppSettingsEntity
import com.example.taskreminder.data.DailyRecordEntity
import com.example.taskreminder.data.ItemEntity
import com.example.taskreminder.data.ItemStatusSummary
import com.example.taskreminder.data.RecordSource
import com.example.taskreminder.data.RecordStatus
import com.example.taskreminder.data.TaskEntity
import com.example.taskreminder.notification.ReminderScheduler
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppRepository(AppDatabase.get(app))

    val tasks: StateFlow<List<TaskEntity>> = repository.observeTasks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val settings: StateFlow<AppSettingsEntity?> = repository.observeSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            repository.ensureSettings()
            applySchedule()
        }
    }

    fun observeItems(taskId: Long): Flow<List<ItemEntity>> = repository.observeItems(taskId)

    fun observeItemSummary(taskId: Long, today: LocalDate): Flow<List<ItemStatusSummary>> =
        repository.observeItemStatusSummary(taskId, today)

    fun observeMonthRecords(
        taskId: Long,
        itemId: Long,
        month: LocalDate
    ): Flow<List<DailyRecordEntity>> {
        val monthStart = month.withDayOfMonth(1)
        val monthEnd = month.withDayOfMonth(month.lengthOfMonth())
        return repository.observeMonthlyRecords(taskId, itemId, monthStart, monthEnd)
    }

    fun createTask(name: String, memo: String?, itemNamesCsv: String) {
        viewModelScope.launch {
            val itemNames = itemNamesCsv.split(",")
            repository.createTask(name = name, memo = memo, itemNames = itemNames)
        }
    }

    fun updateTask(taskId: Long, name: String, memo: String?) {
        viewModelScope.launch {
            repository.updateTask(taskId, name, memo)
        }
    }

    fun addItem(taskId: Long, itemName: String) {
        viewModelScope.launch {
            repository.addItemToTask(taskId, itemName)
        }
    }

    fun saveToday(taskId: Long, itemId: Long, status: RecordStatus) {
        saveDate(taskId, itemId, LocalDate.now(), status)
    }

    fun saveDate(taskId: Long, itemId: Long, date: LocalDate, status: RecordStatus) {
        viewModelScope.launch {
            repository.saveRecord(
                taskId = taskId,
                itemId = itemId,
                date = date,
                status = status,
                source = RecordSource.APP
            )
        }
    }

    fun saveSettings(enabled: Boolean, time: String) {
        viewModelScope.launch {
            val (hour, minute) = parseTime(time)
            val normalized = "%02d:%02d".format(hour, minute)
            repository.updateSettings(enabled = enabled, time = normalized)
            applySchedule()
        }
    }

    private suspend fun applySchedule() {
        val current = repository.getSettings()
        val context = getApplication<Application>().applicationContext
        if (current == null || !current.notificationEnabled) {
            ReminderScheduler.cancel(context)
            return
        }
        val (hour, minute) = parseTime(current.notificationTime)
        ReminderScheduler.scheduleDaily(context, hour, minute)
    }

    private fun parseTime(value: String): Pair<Int, Int> {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return hour to minute
    }
}
