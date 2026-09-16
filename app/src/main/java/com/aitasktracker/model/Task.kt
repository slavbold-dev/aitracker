package com.aitasktracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: Long, // Timestamp for the task date
    val time: Long? = null, // Timestamp for specific time, null for all-day tasks
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Helper property to check if task is for a specific time or all day
    val isAllDay: Boolean
        get() = time == null
}
