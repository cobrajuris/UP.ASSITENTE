package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Calendar Event & Reminder Entity
 */
@Entity(tableName = "events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTimeMillis: Long,
    val endTimeMillis: Long = startTimeMillis + (60 * 60 * 1000), // Default 1 hour
    val location: String = "",
    val isMeeting: Boolean = false,
    val attendees: String = "",
    val isReminder: Boolean = false,
    val reminderMinutesBefore: Int = 15,
    val source: String = "VOICE" // VOICE, EMAIL_AUTO, MANUAL
)

/**
 * Task item with autonomous priority calculation
 */
@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long? = null,
    val priority: PriorityLevel = PriorityLevel.MEDIUM,
    val priorityScore: Int = 50, // 0 to 100 calculated autonomously by AI
    val isCompleted: Boolean = false,
    val category: String = "Geral",
    val aiReasoning: String = "Prioridade definida automaticamente com base no impacto e prazo.",
    val createdAt: Long = System.currentTimeMillis()
)

enum class PriorityLevel(val label: String, val weight: Int) {
    URGENT("Crítica / Urgente", 4),
    HIGH("Alta Prioridade", 3),
    MEDIUM("Média Prioridade", 2),
    LOW("Baixa Prioridade", 1)
}

/**
 * Expense item for budget tracking
 */
@Entity(tableName = "expenses")
data class ExpenseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String = "Outros", // Alimentação, Transporte, Moradia, Lazer, Saúde, Trabalho, Outros
    val dateMillis: Long = System.currentTimeMillis(),
    val paymentMethod: String = "PIX", // Cartão de Crédito, Débito, PIX, Dinheiro
    val notes: String = ""
)

/**
 * Email Message with auto-responder & meeting parsing
 */
@Entity(tableName = "emails")
data class EmailMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,
    val senderEmail: String,
    val subject: String,
    val body: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val isMeetingInvite: Boolean = false,
    val proposedMeetingTimeMillis: Long? = null,
    val meetingLocation: String? = null,
    val autoReplySent: Boolean = false,
    val autoReplyContent: String? = null,
    val status: EmailStatus = EmailStatus.UNREAD,
    val isImportant: Boolean = false
)

enum class EmailStatus {
    UNREAD, READ, REPLIED, DECLINED, ACCEPTED
}

/**
 * Activity log of autonomous assistant actions
 */
@Entity(tableName = "assistant_logs")
data class AssistantLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // CALENDAR, TASK_PRIORITIZATION, EXPENSE, EMAIL_AUTOREPLY, VOICE_PROMPT
    val title: String,
    val details: String
)
