package com.example.taskreminder.ui

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.content.Context
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
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppRepository(AppDatabase.get(app))
    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSettings()
            applySchedule()
        }
        checkForUpdate()
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

    private fun checkForUpdate() {
        viewModelScope.launch {
            _updateInfo.value = withContext(Dispatchers.IO) {
                runCatching {
                    val current = currentVersionName()
                    val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/vnd.github+json")
                    }
                    conn.inputStream.bufferedReader().use { reader ->
                        val json = JSONObject(reader.readText())
                        val tag = json.optString("tag_name", "")
                        val assets = json.optJSONArray("assets")
                        var apkUrl: String? = null
                        if (assets != null) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.optJSONObject(i) ?: continue
                                val name = asset.optString("name")
                                if (name.endsWith(".apk", ignoreCase = true)) {
                                    apkUrl = asset.optString("browser_download_url")
                                    break
                                }
                            }
                        }
                        if (apkUrl.isNullOrBlank()) return@runCatching null
                        val ignored = ignoredVersionTag()
                        if (ignored == tag) return@runCatching null
                        if (isNewerVersion(tag, current)) {
                            UpdateInfo(latestVersion = tag, apkUrl = apkUrl)
                        } else {
                            null
                        }
                    }
                }.getOrNull()
            }
        }
    }

    fun markUpdateNoticeOpened(versionTag: String) {
        prefs.edit().putString(KEY_IGNORED_VERSION_TAG, versionTag).apply()
        _updateInfo.value = null
    }

    private fun ignoredVersionTag(): String? = prefs.getString(KEY_IGNORED_VERSION_TAG, null)

    private fun currentVersionName(): String {
        val app = getApplication<Application>()
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.packageManager.getPackageInfo(
                app.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            app.packageManager.getPackageInfo(app.packageName, 0)
        }
        return info.versionName ?: "0"
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.trim().removePrefix("v")
        val l = local.trim().removePrefix("v")
        val rParts = r.split('.').map { it.toIntOrNull() ?: 0 }
        val lParts = l.split('.').map { it.toIntOrNull() ?: 0 }
        val max = maxOf(rParts.size, lParts.size)
        for (i in 0 until max) {
            val rv = rParts.getOrElse(i) { 0 }
            val lv = lParts.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }

    data class UpdateInfo(
        val latestVersion: String,
        val apkUrl: String
    )

    companion object {
        private const val LATEST_RELEASE_API = "https://api.github.com/repos/hamuhamuhamu/task-reminder-app/releases/latest"
        private const val PREFS_NAME = "app_ui_prefs"
        private const val KEY_IGNORED_VERSION_TAG = "ignored_version_tag"
    }
}
