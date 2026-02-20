package com.example.taskreminder.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.taskreminder.data.DailyRecordEntity
import com.example.taskreminder.data.ItemEntity
import com.example.taskreminder.data.RecordStatus
import com.example.taskreminder.data.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val TASK_DETAIL = "task_detail"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    navController: NavHostController
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<TaskEntity?>(null) }

    if (showCreateDialog) {
        TaskEditorDialog(
            title = "タスク作成",
            initialName = "",
            initialMemo = "",
            initialItems = "項目1",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, memo, items ->
                viewModel.createTask(name = name, memo = memo, itemNamesCsv = items)
                showCreateDialog = false
            }
        )
    }

    editTarget?.let { target ->
        TaskEditorDialog(
            title = "タスク編集",
            initialName = target.name,
            initialMemo = target.memo.orEmpty(),
            initialItems = "",
            showItemsField = false,
            onDismiss = { editTarget = null },
            onConfirm = { name, memo, _ ->
                viewModel.updateTask(taskId = target.id, name = name, memo = memo)
                editTarget = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ホーム / タスク一覧") },
                actions = {
                    TextButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Text("設定")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Text("+")
            }
        }
    ) { paddingValues ->
        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                updateInfo?.let { info ->
                    UpdateAvailableCard(
                        latestVersion = info.latestVersion,
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
                            )
                        }
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋を押して最初のタスクを作成してください。")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                updateInfo?.let { info ->
                    item {
                        UpdateAvailableCard(
                            latestVersion = info.latestVersion,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
                                )
                            }
                        )
                    }
                }
                items(tasks) { task ->
                    TaskCard(
                        task = task,
                        onOpen = { navController.navigate("${Routes.TASK_DETAIL}/${task.id}") },
                        onEdit = { editTarget = task }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateAvailableCard(
    latestVersion: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("最新版が利用できます", fontWeight = FontWeight.Bold)
            Text("最新バージョン: $latestVersion")
            Text("タップしてAPKをダウンロード")
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(task.name, style = MaterialTheme.typography.titleMedium)
            task.memo?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Text("開く") }
                TextButton(onClick = onEdit) { Text("編集") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    viewModel: AppViewModel
) {
    val items by viewModel.observeItems(taskId).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddItemDialog by remember { mutableStateOf(false) }

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAdd = { itemName ->
                viewModel.addItem(taskId = taskId, itemName = itemName)
                showAddItemDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("タスク詳細") },
                actions = {
                    TextButton(onClick = { showAddItemDialog = true }) {
                        Text("項目追加")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CalendarTabContent(
                taskId = taskId,
                items = items,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun CalendarTabContent(
    taskId: Long,
    items: List<ItemEntity>,
    viewModel: AppViewModel
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedItemId by remember { mutableLongStateOf(0L) }
    if (selectedItemId == 0L && items.isNotEmpty()) {
        selectedItemId = items.first().id
    }

    val monthStart = remember(month) { month.atDay(1) }
    val records by if (selectedItemId > 0) {
        viewModel.observeMonthRecords(taskId, selectedItemId, monthStart)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList<DailyRecordEntity>()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("前月") }
            Text("${month.year}年${month.monthValue}月", fontWeight = FontWeight.Bold)
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("次月") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                TextButton(
                    onClick = { selectedItemId = item.id },
                    modifier = Modifier.background(
                        if (selectedItemId == item.id) Color.LightGray else Color.Transparent
                    )
                ) {
                    Text(item.name)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        CalendarGrid(
            month = month,
            records = records,
            onClickDate = { date ->
                val current = records.firstOrNull { it.recordDate == date.toString() }?.status
                    ?: RecordStatus.UNSET
                val next = if (current == RecordStatus.YES) {
                    RecordStatus.UNSET
                } else {
                    // NO and UNSET are treated the same, so tap toggles only YES/UNSET.
                    RecordStatus.YES
                }
                if (selectedItemId > 0) {
                    viewModel.saveDate(taskId, selectedItemId, date, next)
                }
            }
        )
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    records: List<DailyRecordEntity>,
    onClickDate: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val leadingBlank = firstDay.dayOfWeek.shiftedForWeekStart()
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells: List<LocalDate?> = List(leadingBlank) { null } + days
    val recordMap = records.associateBy { it.recordDate }

    val weekTitles = listOf("日", "月", "火", "水", "木", "金", "土")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        weekTitles.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(cells) { date ->
            if (date == null) {
                Box(modifier = Modifier.size(40.dp))
            } else {
                val status = recordMap[date.toString()]?.status ?: RecordStatus.UNSET
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(statusColor(status))
                        .clickable { onClickDate(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("${date.dayOfMonth}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var enabled by remember(settings?.notificationEnabled) {
        mutableStateOf(settings?.notificationEnabled ?: true)
    }
    var time by remember(settings?.notificationTime) {
        mutableStateOf(settings?.notificationTime ?: "20:00")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                actions = {
                    TextButton(onClick = onBack) { Text("戻る") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("毎日通知を有効にする")
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                singleLine = true,
                label = { Text("通知時刻 (HH:mm)") }
            )
            Button(
                onClick = { viewModel.saveSettings(enabled = enabled, time = time) }
            ) {
                Text("設定を保存")
            }
        }
    }
}

@Composable
private fun TaskEditorDialog(
    title: String,
    initialName: String,
    initialMemo: String,
    initialItems: String,
    showItemsField: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (name: String, memo: String, itemCsv: String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var memo by remember(initialMemo) { mutableStateOf(initialMemo) }
    var items by remember(initialItems) { mutableStateOf(initialItems) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("タスク名") }
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ（任意）") }
                )
                if (showItemsField) {
                    OutlinedTextField(
                        value = items,
                        onValueChange = { items = it },
                        label = { Text("項目（カンマ区切り）") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim(), memo.trim(), items)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("項目追加") },
        text = {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                singleLine = true,
                label = { Text("項目名") }
            )
        },
        confirmButton = {
            Button(onClick = { if (itemName.isNotBlank()) onAdd(itemName.trim()) }) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

private fun DayOfWeek.shiftedForWeekStart(): Int = when (this) {
    DayOfWeek.SUNDAY -> 0
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
}

private fun statusColor(status: RecordStatus): Color = when (status) {
    RecordStatus.YES -> Color(0xFFA5D6A7)
    RecordStatus.NO -> Color(0xFFE0E0E0)
    RecordStatus.UNSET -> Color(0xFFE0E0E0)
}
