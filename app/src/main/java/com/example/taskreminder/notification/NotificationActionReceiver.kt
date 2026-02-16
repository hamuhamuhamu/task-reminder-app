package com.example.taskreminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        val status = statusName?.let { RecordStatus.valueOf(it) } ?: return

        if (taskId <= 0 || itemId <= 0) return

        CoroutineScope(Dispatchers.IO).launch {
            val repository = AppRepository(AppDatabase.get(context))
            repository.saveRecord(
                taskId = taskId,
                itemId = itemId,
                date = LocalDate.now(),
                status = status,
                source = RecordSource.NOTIFICATION
            )
        }
    }

    companion object {
        private const val EXTRA_TASK_ID = "extra_task_id"
        private const val EXTRA_ITEM_ID = "extra_item_id"
        private const val EXTRA_STATUS = "extra_status"

        fun intent(
            appContext: Context,
            taskId: Long,
            itemId: Long,
            status: RecordStatus
        ): Intent = Intent(appContext, NotificationActionReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_STATUS, status.name)
        }
    }
}
