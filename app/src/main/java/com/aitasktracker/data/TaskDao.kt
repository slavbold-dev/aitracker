package com.aitasktracker.data

import androidx.room.*
import com.aitasktracker.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY time ASC, id ASC")
    fun getTasksForDate(date: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    fun getTasksBetweenDates(startDate: Long, endDate: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY date ASC, time ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean)
}
