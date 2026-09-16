package com.aitasktracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitasktracker.data.TaskDatabase
import com.aitasktracker.model.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao = TaskDatabase.getDatabase(application).taskDao()
    
    private val _selectedDate = MutableStateFlow(getStartOfDay(Calendar.getInstance()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()
    
    val tasksForSelectedDate: Flow<List<Task>> = _selectedDate.flatMapLatest { date ->
        taskDao.getTasksForDate(date)
    }
    
    fun setSelectedDate(date: Long) {
        _selectedDate.value = getStartOfDay(date)
    }
    
    fun addTask(title: String, description: String, date: Long, time: Long?) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                date = getStartOfDay(date),
                time = time
            )
            taskDao.insertTask(task)
        }
    }
    
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }
    
    fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            taskDao.toggleTaskCompletion(taskId, isCompleted)
        }
    }
    
    private fun getStartOfDay(calendar: Calendar): Long {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return getStartOfDay(calendar)
    }
}
