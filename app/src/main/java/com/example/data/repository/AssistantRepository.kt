package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.AssistantLog
import com.example.data.model.CalendarEvent
import com.example.data.model.EmailMessage
import com.example.data.model.EmailStatus
import com.example.data.model.ExpenseItem
import com.example.data.model.PriorityLevel
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class AssistantRepository(private val db: AppDatabase) {

    // Events
    val allEvents: Flow<List<CalendarEvent>> = db.eventDao().getAllEvents()
    val allMeetings: Flow<List<CalendarEvent>> = db.eventDao().getAllMeetings()

    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<CalendarEvent>> {
        return db.eventDao().getEventsForDay(startOfDay, endOfDay)
    }

    suspend fun insertEvent(event: CalendarEvent): Long {
        val id = db.eventDao().insertEvent(event)
        logAction(
            actionType = "CALENDAR",
            title = if (event.isReminder) "Lembrete criado" else "Evento agendado",
            details = "${event.title} (${event.source})"
        )
        return id
    }

    suspend fun deleteEvent(event: CalendarEvent) = db.eventDao().deleteEvent(event)

    // Tasks
    val allTasks: Flow<List<TaskItem>> = db.taskDao().getAllTasks()
    val pendingTasks: Flow<List<TaskItem>> = db.taskDao().getPendingTasks()

    suspend fun insertTask(task: TaskItem): Long {
        val id = db.taskDao().insertTask(task)
        logAction(
            actionType = "TASK_PRIORITIZATION",
            title = "Nova tarefa registrada",
            details = "${task.title} • Prioridade: ${task.priority.label}"
        )
        return id
    }

    suspend fun updateTask(task: TaskItem) = db.taskDao().updateTask(task)

    suspend fun updateTasks(tasks: List<TaskItem>) = db.taskDao().updateTasks(tasks)

    suspend fun deleteTask(task: TaskItem) = db.taskDao().deleteTask(task)

    // Expenses
    val allExpenses: Flow<List<ExpenseItem>> = db.expenseDao().getAllExpenses()

    fun getExpensesForCurrentMonth(): Flow<List<ExpenseItem>> {
        val (start, end) = getMonthRange()
        return db.expenseDao().getExpensesForMonth(start, end)
    }

    fun getTotalSpentThisMonth(): Flow<Double?> {
        val (start, end) = getMonthRange()
        return db.expenseDao().getTotalSpentForMonth(start, end)
    }

    suspend fun insertExpense(expense: ExpenseItem): Long {
        val id = db.expenseDao().insertExpense(expense)
        logAction(
            actionType = "EXPENSE",
            title = "Gasto registrado",
            details = "${expense.title}: R$ ${String.format("%.2f", expense.amount)} (${expense.category})"
        )
        return id
    }

    suspend fun deleteExpense(expense: ExpenseItem) = db.expenseDao().deleteExpense(expense)

    // Emails
    val allEmails: Flow<List<EmailMessage>> = db.emailDao().getAllEmails()
    val meetingInvites: Flow<List<EmailMessage>> = db.emailDao().getMeetingInvites()

    suspend fun insertEmail(email: EmailMessage): Long = db.emailDao().insertEmail(email)

    suspend fun updateEmail(email: EmailMessage) = db.emailDao().updateEmail(email)

    suspend fun deleteEmail(email: EmailMessage) = db.emailDao().deleteEmail(email)

    // Assistant Logs
    val recentLogs: Flow<List<AssistantLog>> = db.assistantLogDao().getRecentLogs()

    suspend fun logAction(actionType: String, title: String, details: String) {
        db.assistantLogDao().insertLog(
            AssistantLog(
                actionType = actionType,
                title = title,
                details = details
            )
        )
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}
