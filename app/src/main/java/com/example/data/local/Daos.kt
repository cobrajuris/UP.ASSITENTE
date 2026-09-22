package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AssistantLog
import com.example.data.model.CalendarEvent
import com.example.data.model.EmailMessage
import com.example.data.model.ExpenseItem
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startTimeMillis ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM events WHERE startTimeMillis >= :startOfDay AND startTimeMillis < :endOfDay ORDER BY startTimeMillis ASC")
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM events WHERE isMeeting = 1 ORDER BY startTimeMillis ASC")
    fun getAllMeetings(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent): Long

    @Update
    suspend fun updateEvent(event: CalendarEvent)

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priorityScore DESC, dueDateMillis ASC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priorityScore DESC, dueDateMillis ASC")
    fun getPendingTasks(): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Update
    suspend fun updateTask(task: TaskItem)

    @Update
    suspend fun updateTasks(tasks: List<TaskItem>)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpenses(): Flow<List<ExpenseItem>>

    @Query("SELECT * FROM expenses WHERE dateMillis >= :startOfMonth AND dateMillis <= :endOfMonth ORDER BY dateMillis DESC")
    fun getExpensesForMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<ExpenseItem>>

    @Query("SELECT SUM(amount) FROM expenses WHERE dateMillis >= :startOfMonth AND dateMillis <= :endOfMonth")
    fun getTotalSpentForMonth(startOfMonth: Long, endOfMonth: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseItem): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseItem)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)
}

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails ORDER BY receivedAt DESC")
    fun getAllEmails(): Flow<List<EmailMessage>>

    @Query("SELECT * FROM emails WHERE isMeetingInvite = 1 ORDER BY receivedAt DESC")
    fun getMeetingInvites(): Flow<List<EmailMessage>>

    @Query("SELECT * FROM emails WHERE isMeetingInvite = 1 AND autoReplySent = 0")
    suspend fun getPendingMeetingInvites(): List<EmailMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: EmailMessage): Long

    @Update
    suspend fun updateEmail(email: EmailMessage)

    @Delete
    suspend fun deleteEmail(email: EmailMessage)
}

@Dao
interface AssistantLogDao {
    @Query("SELECT * FROM assistant_logs ORDER BY timestamp DESC LIMIT 30")
    fun getRecentLogs(): Flow<List<AssistantLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AssistantLog): Long
}
