package com.example.taskreminder.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.taskreminder.MainActivity
import com.example.taskreminder.data.AppDatabase
import com.example.taskreminder.data.AppRepository
import com.example.taskreminder.data.RecordStatus
import java.time.LocalDate

class DailyReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = AppRepository(AppDatabase.get(appContext))
        repository.ensureSettings()
        val settings = repository.getSettings() ?: return Result.success()
        if (!settings.notificationEnabled) return Result.success()

        createChannel()
        val today = LocalDate.now()
        val payload = repository.getReminderPayload(today)

        payload.forEach { (task, summaries) ->
            summaries.forEach { summary ->
                val days = summary.daysSinceLastYes?.toString() ?: "N/A"
                val message = "(${summary.itemName}) Today is day $days since last YES record."

                val openIntent = Intent(appContext, MainActivity::class.java).apply {
                    putExtra("taskId", task.id)
                }
                val openPendingIntent = PendingIntent.getActivity(
                    appContext,
                    (task.id * 100 + summary.itemId).toInt(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val yesPendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    (task.id * 1000 + summary.itemId * 2 + 1).toInt(),
                    NotificationActionReceiver.intent(
                        appContext = appContext,
                        taskId = task.id,
                        itemId = summary.itemId,
                        status = RecordStatus.YES
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val noPendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    (task.id * 1000 + summary.itemId * 2 + 2).toInt(),
                    NotificationActionReceiver.intent(
                        appContext = appContext,
                        taskId = task.id,
                        itemId = summary.itemId,
                        status = RecordStatus.NO
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setContentTitle(task.name)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(openPendingIntent)
                    .setAutoCancel(true)
                    .addAction(0, "Yes", yesPendingIntent)
                    .addAction(0, "No", noPendingIntent)
                    .build()

                NotificationManagerCompat.from(appContext).notify(
                    (task.id * 1000 + summary.itemId).toInt(),
                    notification
                )
            }
        }

        return Result.success()
    }

    private fun createChannel() {
        val manager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Reminder",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily reminder for task records"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "task_reminder_channel"
    }
}
