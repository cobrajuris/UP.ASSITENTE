package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AssistantAiEngine
import com.example.ai.ParsedIntentResult
import com.example.data.local.AppDatabase
import com.example.data.model.AssistantLog
import com.example.data.model.CalendarEvent
import com.example.data.model.EmailMessage
import com.example.data.model.EmailStatus
import com.example.data.model.ExpenseItem
import com.example.data.model.PriorityLevel
import com.example.data.model.TaskItem
import com.example.data.repository.AssistantRepository
import com.example.speech.VoiceInputHelper
import com.example.speech.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AssistantRepository(db)
    private val aiEngine = AssistantAiEngine()
    val voiceHelper = VoiceInputHelper(application)

    val voiceState: StateFlow<VoiceState> = voiceHelper.voiceState

    val allEvents: StateFlow<List<CalendarEvent>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskItem>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseItem>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyExpenses: StateFlow<List<ExpenseItem>> = repository.getExpensesForCurrentMonth()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpentThisMonth: StateFlow<Double?> = repository.getTotalSpentThisMonth()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allEmails: StateFlow<List<EmailMessage>> = repository.allEmails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<AssistantLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _lastAiFeedback = MutableStateFlow<String?>(null)
    val lastAiFeedback: StateFlow<String?> = _lastAiFeedback.asStateFlow()

    private val _autoResponderEnabled = MutableStateFlow(true)
    val autoResponderEnabled: StateFlow<Boolean> = _autoResponderEnabled.asStateFlow()

    private val _monthlyBudgetLimit = MutableStateFlow(3500.0)
    val monthlyBudgetLimit: StateFlow<Double> = _monthlyBudgetLimit.asStateFlow()

    init {
        seedInitialDataIfEmpty()
    }

    fun setMonthlyBudgetLimit(limit: Double) {
        _monthlyBudgetLimit.value = limit
    }

    fun toggleAutoResponder(enabled: Boolean) {
        _autoResponderEnabled.value = enabled
        viewModelScope.launch {
            repository.logAction(
                actionType = "SETTINGS",
                title = if (enabled) "Respostas automáticas ativadas" else "Respostas automáticas desativadas",
                details = "Configuração de IA autônoma atualizada pelo usuário."
            )
        }
    }

    fun clearFeedback() {
        _lastAiFeedback.value = null
    }

    fun processInput(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val currentEvents = allEvents.value
                val result = aiEngine.processNaturalLanguage(text, currentEvents)

                when (result) {
                    is ParsedIntentResult.EventCreated -> {
                        repository.insertEvent(result.event)
                        _lastAiFeedback.value = "📅 " + result.feedback
                    }
                    is ParsedIntentResult.TaskCreated -> {
                        repository.insertTask(result.task)
                        _lastAiFeedback.value = "✓ " + result.feedback
                    }
                    is ParsedIntentResult.ExpenseCreated -> {
                        repository.insertExpense(result.expense)
                        _lastAiFeedback.value = "💰 " + result.feedback
                    }
                    is ParsedIntentResult.AssistantResponse -> {
                        repository.logAction("VOICE_PROMPT", "Comando de voz processado", text)
                        _lastAiFeedback.value = "🤖 " + result.text
                    }
                }
            } catch (e: Exception) {
                _lastAiFeedback.value = "Não consegui processar: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun autonomouslyPrioritizeTasks() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val pending = allTasks.value.filter { !it.isCompleted }
                if (pending.isEmpty()) {
                    _lastAiFeedback.value = "Nenhuma tarefa pendente para priorizar."
                    return@launch
                }
                val reordered = aiEngine.autonomouslyPrioritizeAllTasks(pending)
                repository.updateTasks(reordered)
                repository.logAction(
                    actionType = "TASK_PRIORITIZATION",
                    title = "Tarefas priorizadas autonomamente",
                    details = "${reordered.size} tarefas reorganizadas por relevância e prazos."
                )
                _lastAiFeedback.value = "⚡ ${reordered.size} tarefas organizadas com sucesso pela IA autônoma!"
            } catch (e: Exception) {
                _lastAiFeedback.value = "Erro ao priorizar tarefas: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun addEvent(title: String, startTime: Long, endTime: Long, isReminder: Boolean, isMeeting: Boolean, location: String) {
        viewModelScope.launch {
            repository.insertEvent(
                CalendarEvent(
                    title = title,
                    startTimeMillis = startTime,
                    endTimeMillis = endTime,
                    isReminder = isReminder,
                    isMeeting = isMeeting,
                    location = location,
                    source = "MANUAL"
                )
            )
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun addTask(title: String, description: String, dueDateMillis: Long?, category: String) {
        viewModelScope.launch {
            val initial = TaskItem(
                title = title,
                description = description,
                dueDateMillis = dueDateMillis,
                category = category
            )
            val (level, score) = aiEngine.calculateAutonomousPriority(initial)
            val task = initial.copy(priority = level, priorityScore = score)
            repository.insertTask(task)
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun addExpense(title: String, amount: Double, category: String, paymentMethod: String) {
        viewModelScope.launch {
            repository.insertExpense(
                ExpenseItem(
                    title = title,
                    amount = amount,
                    category = category,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseItem) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun autonomouslyReplyToEmailMeeting(email: EmailMessage) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val currentEvents = allEvents.value
                val (accepted, reply) = aiEngine.generateAutonomousMeetingReply(email, currentEvents)

                val updatedEmail = email.copy(
                    autoReplySent = true,
                    autoReplyContent = reply,
                    status = if (accepted) EmailStatus.ACCEPTED else EmailStatus.DECLINED
                )
                repository.updateEmail(updatedEmail)

                // If accepted, also automatically schedule on the user's calendar!
                if (accepted && email.proposedMeetingTimeMillis != null) {
                    val newEvent = CalendarEvent(
                        title = "Reunião: ${email.subject}",
                        description = "Reunião confirmada autonomamente pela IA Aura. Remetente: ${email.senderName} (${email.senderEmail})",
                        startTimeMillis = email.proposedMeetingTimeMillis,
                        endTimeMillis = email.proposedMeetingTimeMillis + (60 * 60 * 1000L),
                        location = email.meetingLocation ?: "Google Meet",
                        isMeeting = true,
                        attendees = email.senderEmail,
                        source = "EMAIL_AUTO"
                    )
                    repository.insertEvent(newEvent)
                }

                repository.logAction(
                    actionType = "EMAIL_AUTOREPLY",
                    title = if (accepted) "Resposta enviada: Reunião Aceita" else "Resposta enviada: Proposta de Reagendamento",
                    details = "E-mail para ${email.senderName} referente a \"${email.subject}\""
                )

                _lastAiFeedback.value = if (accepted)
                    "✉️ Resposta automática enviada aceitando o convite de ${email.senderName} e adicionada ao calendário!"
                else
                    "✉️ Conflito detectado na agenda: resposta enviada a ${email.senderName} propondo novo horário."
            } catch (e: Exception) {
                _lastAiFeedback.value = "Erro na resposta automática: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun simulateIncomingMeeting(subject: String, sender: String, hoursInFuture: Int = 24, hasConflict: Boolean = false) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, hoursInFuture)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val meetingTime = cal.timeInMillis

            // If requested conflict, ensure an event exists at that time
            if (hasConflict) {
                repository.insertEvent(
                    CalendarEvent(
                        title = "Compromisso Pré-Agendado",
                        startTimeMillis = meetingTime - (15 * 60 * 1000L),
                        endTimeMillis = meetingTime + (45 * 60 * 1000L),
                        isMeeting = true,
                        source = "MANUAL"
                    )
                )
            }

            val email = EmailMessage(
                senderName = sender,
                senderEmail = "${sender.lowercase().replace(" ", ".")}@empresa.com.br",
                subject = subject,
                body = "Olá! Gostaria de alinhar nosso projeto estratégico na data proposta. Você teria disponibilidade para esse horário?",
                isMeetingInvite = true,
                proposedMeetingTimeMillis = meetingTime,
                meetingLocation = "Google Meet",
                isImportant = true
            )
            repository.insertEmail(email)

            repository.logAction(
                actionType = "EMAIL",
                title = "Novo convite de reunião recebido",
                details = "De $sender: $subject"
            )

            // If auto-responder is active, execute autonomous evaluation!
            if (_autoResponderEnabled.value) {
                autonomouslyReplyToEmailMeeting(email)
            }
        }
    }

    private fun seedInitialDataIfEmpty() {
        viewModelScope.launch {
            val existingEvents = repository.allEvents.first()
            if (existingEvents.isEmpty()) {
                val now = System.currentTimeMillis()
                val hour = 3600000L

                // Events
                repository.insertEvent(
                    CalendarEvent(
                        title = "Alinhamento com Diretoria",
                        description = "Apresentação dos resultados do trimestre",
                        startTimeMillis = now + (2 * hour),
                        endTimeMillis = now + (3 * hour),
                        isMeeting = true,
                        location = "Sala de Reuniões 1",
                        source = "MANUAL"
                    )
                )
                repository.insertEvent(
                    CalendarEvent(
                        title = "Lembrete: Tomar Vitaminas",
                        startTimeMillis = now + (5 * hour),
                        endTimeMillis = now + (5 * hour) + (15 * 60 * 1000L),
                        isReminder = true,
                        source = "VOICE"
                    )
                )

                // Tasks
                repository.insertTask(
                    TaskItem(
                        title = "Entregar relatório fiscal e tributário",
                        description = "Enviar balancete final para a contabilidade",
                        dueDateMillis = now + (24 * hour),
                        priority = PriorityLevel.URGENT,
                        priorityScore = 95,
                        category = "Finanças",
                        aiReasoning = "Prazo iminente em 24h e impacto fiscal crítico detectados."
                    )
                )
                repository.insertTask(
                    TaskItem(
                        title = "Preparar slides de apresentação para cliente",
                        description = "Revisar metas e estimativas de entrega",
                        dueDateMillis = now + (48 * hour),
                        priority = PriorityLevel.HIGH,
                        priorityScore = 78,
                        category = "Trabalho",
                        aiReasoning = "Envolve diretoria e cliente externo; alta visibilidade."
                    )
                )
                repository.insertTask(
                    TaskItem(
                        title = "Comprar novo adaptador HDMI",
                        dueDateMillis = now + (7 * 24 * hour),
                        priority = PriorityLevel.LOW,
                        priorityScore = 25,
                        category = "Pessoal",
                        aiReasoning = "Baixo impacto no fluxo do dia; sem urgência de prazo."
                    )
                )

                // Expenses
                repository.insertExpense(
                    ExpenseItem(
                        title = "Almoço Executivo",
                        amount = 48.50,
                        category = "Alimentação",
                        paymentMethod = "Cartão de Crédito"
                    )
                )
                repository.insertExpense(
                    ExpenseItem(
                        title = "Combustível Posto Ipiranga",
                        amount = 180.00,
                        category = "Transporte",
                        paymentMethod = "PIX"
                    )
                )
                repository.insertExpense(
                    ExpenseItem(
                        title = "Supermercado Semanal",
                        amount = 320.40,
                        category = "Alimentação",
                        paymentMethod = "Débito"
                    )
                )

                // Sample Email
                val emailInvite = EmailMessage(
                    senderName = "Mariana Costa (Diretora Comercial)",
                    senderEmail = "mariana.costa@empresa.com",
                    subject = "Reunião de Alinhamento de Metas 2026",
                    body = "Olá! Gostaria de agendar nossa reunião de alinhamento estratégico para amanhã às 15:00 via Google Meet. Aguardo sua confirmação.",
                    isMeetingInvite = true,
                    proposedMeetingTimeMillis = now + (26 * hour),
                    meetingLocation = "Google Meet",
                    autoReplySent = true,
                    autoReplyContent = "Olá Mariana Costa,\n\nVerifiquei minha agenda e estou disponível no horário proposto. Confirmo presença na reunião de metas!\n\nAtenciosamente,\nAssistente Aura",
                    status = EmailStatus.ACCEPTED,
                    isImportant = true
                )
                repository.insertEmail(emailInvite)

                repository.logAction(
                    actionType = "SYSTEM",
                    title = "Assistente Aura Inicializado",
                    details = "Agenda sincronizada, priorização ativada e monitoramento de e-mails em execução."
                )
            }
        }
    }
}
