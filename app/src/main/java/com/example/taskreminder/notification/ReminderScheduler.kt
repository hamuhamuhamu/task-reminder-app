package com.example.taskreminder.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val UNIQUE_WORK_NAME = "daily_task_reminder"

    fun scheduleDaily(context: Context, hour: Int = 20, minute: Int = 0) {
        val now = LocalDateTime.now()
        val nextTime = now.with(LocalTime.of(hour, minute)).let {
            if (it.isAfter(now)) it else it.plusDays(1)
        }
        val initialDelay = Duration.between(now, nextTime)

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
