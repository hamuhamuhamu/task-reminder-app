package com.example.taskreminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY updated_at DESC")
    suspend fun getAll(): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: Long): TaskEntity?
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE task_id = :taskId ORDER BY sort_order, id")
    fun observeByTask(taskId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE task_id = :taskId ORDER BY sort_order, id")
    suspend fun getByTask(taskId: Long): List<ItemEntity>

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("SELECT * FROM items WHERE task_id = :taskId AND name = :name LIMIT 1")
    suspend fun findByName(taskId: Long, name: String): ItemEntity?
}

@Dao
interface DailyRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DailyRecordEntity)

    @Query(
        """
        SELECT
            i.id AS item_id,
            i.name AS item_name,
            MAX(CASE WHEN r.status = 'YES' THEN r.record_date END) AS last_yes_date,
            CAST(
                julianday(:today) - julianday(MAX(CASE WHEN r.status = 'YES' THEN r.record_date END))
                AS INTEGER
            ) AS days_since_last_yes,
            MAX(CASE WHEN r.record_date = :today THEN r.status END) AS today_status
        FROM items i
        LEFT JOIN daily_records r
            ON r.task_id = i.task_id
           AND r.item_id = i.id
        WHERE i.task_id = :taskId
        GROUP BY i.id, i.name, i.sort_order
        ORDER BY i.sort_order, i.id
        """
    )
    fun observeItemStatusSummary(taskId: Long, today: String): Flow<List<ItemStatusSummary>>

    @Query(
        """
        SELECT
            i.id AS item_id,
            i.name AS item_name,
            MAX(CASE WHEN r.status = 'YES' THEN r.record_date END) AS last_yes_date,
            CAST(
                julianday(:today) - julianday(MAX(CASE WHEN r.status = 'YES' THEN r.record_date END))
                AS INTEGER
            ) AS days_since_last_yes,
            MAX(CASE WHEN r.record_date = :today THEN r.status END) AS today_status
        FROM items i
        LEFT JOIN daily_records r
            ON r.task_id = i.task_id
           AND r.item_id = i.id
        WHERE i.task_id = :taskId
        GROUP BY i.id, i.name, i.sort_order
        ORDER BY i.sort_order, i.id
        """
    )
    suspend fun getItemStatusSummary(taskId: Long, today: String): List<ItemStatusSummary>

    @Query(
        """
        SELECT * FROM daily_records
        WHERE task_id = :taskId
          AND item_id = :itemId
          AND record_date BETWEEN :fromDate AND :toDate
        ORDER BY record_date
        """
    )
    fun observeMonthlyRecords(
        taskId: Long,
        itemId: Long,
        fromDate: String,
        toDate: String
    ): Flow<List<DailyRecordEntity>>
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)

    @Query(
        """
        UPDATE app_settings
           SET notification_enabled = :enabled,
               notification_time = :time,
               timezone = :timezone,
               updated_at = :updatedAt
         WHERE id = 1
        """
    )
    suspend fun update(
        enabled: Boolean,
        time: String,
        timezone: String,
        updatedAt: Long
    )
}
