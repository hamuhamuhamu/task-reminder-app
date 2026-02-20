package com.example.taskreminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.taskreminder.data.AppDatabase
import com.example.taskreminder.data.AppRepository
import com.example.taskreminder.data.RecordSource
import com.example.taskreminder.data.RecordStatus
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        val statusName = intent.getStringExtra(EXTRA_STATUS)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val status = statusName?.let { RecordStatus.valueOf(it) } ?: return
        val normalizedStatus = if (status == RecordStatus.NO) RecordStatus.UNSET else status

        if (taskId <= 0 || itemId <= 0) return
        if (notificationId >= 0) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val repository = AppRepository(AppDatabase.get(context))
            repository.saveRecord(
                taskId = taskId,
                itemId = itemId,
                date = LocalDate.now(),
                status = normalizedStatus,
                source = RecordSource.NOTIFICATION
            )
        }
    }

    companion object {
        private const val EXTRA_TASK_ID = "extra_task_id"
        private const val EXTRA_ITEM_ID = "extra_item_id"
        private const val EXTRA_STATUS = "extra_status"
        private const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun intent(
            appContext: Context,
            taskId: Long,
            itemId: Long,
            status: RecordStatus,
            notificationId: Int
        ): Intent = Intent(appContext, NotificationActionReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_STATUS, status.name)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
    }
}
