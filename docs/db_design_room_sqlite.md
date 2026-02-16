# DB物理設計（Room / SQLite）

## tables

### tasks
- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `name TEXT NOT NULL`
- `memo TEXT NULL`
- `created_at INTEGER NOT NULL`
- `updated_at INTEGER NOT NULL`

### items
- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `task_id INTEGER NOT NULL`
- `name TEXT NOT NULL`
- `is_default INTEGER NOT NULL DEFAULT 0`
- `sort_order INTEGER NOT NULL DEFAULT 0`
- `created_at INTEGER NOT NULL`
- `updated_at INTEGER NOT NULL`
- FK: `task_id -> tasks.id ON DELETE CASCADE`

### daily_records
- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `task_id INTEGER NOT NULL`
- `item_id INTEGER NOT NULL`
- `record_date TEXT NOT NULL (YYYY-MM-DD)`
- `status TEXT NOT NULL (YES/NO/UNSET)`
- `source TEXT NOT NULL (APP/NOTIFICATION)`
- `updated_at INTEGER NOT NULL`
- UNIQUE: `(task_id, item_id, record_date)`
- FK: `task_id -> tasks.id ON DELETE CASCADE`
- FK: `item_id -> items.id ON DELETE CASCADE`

### app_settings
- `id INTEGER PRIMARY KEY`（固定値1）
- `notification_enabled INTEGER NOT NULL DEFAULT 1`
- `notification_time TEXT NOT NULL (HH:mm)`
- `timezone TEXT NOT NULL`
- `updated_at INTEGER NOT NULL`

## index
- `daily_records(task_id, item_id, record_date)` unique
- `daily_records(record_date)`
- `items(task_id)`

## N日算出SQL
```sql
SELECT
  i.id AS item_id,
  i.name AS item_name,
  MAX(CASE WHEN r.status = 'YES' THEN r.record_date END) AS last_yes_date,
  CAST(
      julianday(:today) - julianday(MAX(CASE WHEN r.status = 'YES' THEN r.record_date END))
      AS INTEGER
  ) AS days_since_last_yes
FROM items i
LEFT JOIN daily_records r
  ON r.task_id = i.task_id
 AND r.item_id = i.id
WHERE i.task_id = :taskId
GROUP BY i.id, i.name, i.sort_order
ORDER BY i.sort_order, i.id;
```
