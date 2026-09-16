package com.aitasktracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitasktracker.model.Task
import com.aitasktracker.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: TaskViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.tasksForSelectedDate.collectAsState(initial = emptyList())
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedTaskForEdit by remember { mutableStateOf<Task?>(null) }
    var showCalendarView by remember { mutableStateOf(false) }
    var slideDirection by remember { mutableStateOf(SlideDirection.Right) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Task Tracker")
                        IconButton(onClick = { showCalendarView = true }) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Календарь",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (!showCalendarView) {
                FloatingActionButton(
                    onClick = { showAddTaskDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = showCalendarView,
            transitionSpec = {
                if (targetState) {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                } else {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                }
            },
            label = "calendarViewToggle"
        ) { isCalendarVisible ->
            if (isCalendarVisible) {
                CalendarView(
                    allTasks = allTasks,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedMillis ->
                        slideDirection = if (selectedMillis > selectedDate) SlideDirection.Left else SlideDirection.Right
                        viewModel.setSelectedDate(selectedMillis)
                        showCalendarView = false
                    },
                    onClose = { showCalendarView = false }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Date selector
                    AnimatedContent(
                        targetState = selectedDate,
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { if (slideDirection == SlideDirection.Left) it else -it },
                                animationSpec = androidx.compose.animation.core.tween(300)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { if (slideDirection == SlideDirection.Left) -it else it },
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                        },
                        label = "dateChange"
                    ) { currentDate ->
                        DateSelector(
                            selectedDate = currentDate,
                            onPreviousDay = {
                                val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
                                calendar.add(Calendar.DAY_OF_YEAR, -1)
                                viewModel.setSelectedDate(calendar.timeInMillis)
                            },
                            onNextDay = {
                                val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
                                calendar.add(Calendar.DAY_OF_YEAR, 1)
                                viewModel.setSelectedDate(calendar.timeInMillis)
                            },
                            onGoToToday = {
                                viewModel.setSelectedDate(System.currentTimeMillis())
                            }
                        )
                    }
                    
                    // Task list
                    if (tasks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Нет задач на этот день",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                TaskItem(
                                    task = task,
                                    onToggleComplete = { viewModel.toggleTaskCompletion(task.id, !task.isCompleted) },
                                    onDelete = { viewModel.deleteTask(task) },
                                    onEdit = { selectedTaskForEdit = task }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add/Edit Task Dialog
    if (showAddTaskDialog || selectedTaskForEdit != null) {
        val initialTask = selectedTaskForEdit
        TaskDialog(
            date = selectedDate,
            existingTask = initialTask,
            onDismiss = { 
                showAddTaskDialog = false
                selectedTaskForEdit = null
            },
            onSave = { title, description, date, time ->
                if (initialTask != null) {
                    viewModel.updateTask(initialTask.copy(title = title, description = description, date = date, time = time))
                } else {
                    viewModel.addTask(title, description, date, time)
                }
                showAddTaskDialog = false
                selectedTaskForEdit = null
            }
        )
    }
}

enum class SlideDirection { Left, Right }

@Composable
fun DateSelector(
    selectedDate: Long,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToToday: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
    val shortDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val today = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
    val isToday = Calendar.getInstance().apply { timeInMillis = selectedDate }.let { cal ->
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
        cal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousDay) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий день")
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateFormat.format(Date(selectedDate)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isToday) {
                        TextButton(onClick = onGoToToday) {
                            Text("Показать сегодня", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                IconButton(onClick = onNextDay) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Следующий день")
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (task.isAllDay) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Весь день") }
                        )
                    } else {
                        task.time?.let { time ->
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            AssistChip(
                                onClick = { },
                                label = { Text(timeFormat.format(Date(time))) }
                            )
                        }
                    }
                }
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun TaskDialog(
    date: Long,
    existingTask: Task?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, date: Long, time: Long?) -> Unit
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var isAllDay by remember { mutableStateOf(existingTask?.isAllDay ?: true) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf(existingTask?.time) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingTask == null) "Новая задача" else "Редактировать задачу") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Весь день")
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }
                
                if (!isAllDay) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val timeText = selectedTime?.let {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                        } ?: "Выбрать время"
                        Text(timeText)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, description, date, if (isAllDay) null else selectedTime)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
    
    if (showTimePicker) {
        TimePickerDialog(
            currentTime = selectedTime ?: System.currentTimeMillis(),
            onDismiss = { showTimePicker = false },
            onConfirm = { selectedTime = it }
        )
    }
}

@Composable
fun TimePickerDialog(
    currentTime: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }
    var hour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }
    var showHourDropdown by remember { mutableStateOf(false) }
    var showMinuteDropdown by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите время") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour selector with dropdown
                    Box {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { showHourDropdown = true }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Выбрать час")
                            }
                            Text(
                                text = String.format("%02d", hour),
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.clickable { showHourDropdown = true }
                            )
                            IconButton(onClick = { hour = (hour - 1 + 24) % 24 }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Час-")
                            }
                        }
                        
                        // Hour dropdown menu
                        if (showHourDropdown) {
                            Card(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(200.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                LazyColumn {
                                    items(24) { h ->
                                        ListItem(
                                            headlineContent = { Text(String.format("%02d", h)) },
                                            modifier = Modifier
                                                .clickable {
                                                    hour = h
                                                    showHourDropdown = false
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    
                    // Minute selector with dropdown
                    Box {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { showMinuteDropdown = true }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Выбрать минуты")
                            }
                            Text(
                                text = String.format("%02d", minute),
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.clickable { showMinuteDropdown = true }
                            )
                            IconButton(onClick = { minute = (minute - 1 + 60) % 60 }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Минута-")
                            }
                        }
                        
                        // Minute dropdown menu
                        if (showMinuteDropdown) {
                            Card(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(200.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                LazyColumn {
                                    items(60) { m ->
                                        ListItem(
                                            headlineContent = { Text(String.format("%02d", m)) },
                                            modifier = Modifier
                                                .clickable {
                                                    minute = m
                                                    showMinuteDropdown = false
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onConfirm(selectedCalendar.timeInMillis)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun CalendarView(
    allTasks: List<Task>,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onClose: () -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    
    // Group tasks by date
    val tasksByDate = remember(allTasks) {
        allTasks.groupBy { it.date }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with month navigation and close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    add(Calendar.MONTH, -1)
                    currentMonth = get(Calendar.MONTH)
                    currentYear = get(Calendar.YEAR)
                }
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий месяц")
            }
            Text(
                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(Calendar.getInstance().apply {
                        set(Calendar.YEAR, currentYear)
                        set(Calendar.MONTH, currentMonth)
                    }.timeInMillis),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    add(Calendar.MONTH, 1)
                    currentMonth = get(Calendar.MONTH)
                    currentYear = get(Calendar.YEAR)
                }
            }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Следующий месяц")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        val daysInMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val firstDayOfWeek = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK).let { 
            if (it == Calendar.SUNDAY) 7 else it - 1 
        }
        
        var dayCounter = 1
        for (week in 0..5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayOfWeek in 0..6) {
                    if (week == 0 && dayOfWeek < firstDayOfWeek) {
                        Spacer(modifier = Modifier.width(40.dp))
                    } else if (dayCounter > daysInMonth) {
                        break
                    } else {
                        val currentDayMillis = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentYear)
                            set(Calendar.MONTH, currentMonth)
                            set(Calendar.DAY_OF_MONTH, dayCounter)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        val hasTasks = tasksByDate.containsKey(currentDayMillis)
                        val isSelected = currentDayMillis == selectedDate
                        
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { onDateSelected(currentDayMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayCounter.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasTasks) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.Green)
                                    )
                                }
                            }
                        }
                        dayCounter++
                    }
                }
            }
            if (dayCounter > daysInMonth) break
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Close button
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Закрыть")
        }
    }
}
