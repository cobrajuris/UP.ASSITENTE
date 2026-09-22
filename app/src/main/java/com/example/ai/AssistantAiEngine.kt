package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CalendarEvent
import com.example.data.model.EmailMessage
import com.example.data.model.ExpenseItem
import com.example.data.model.PriorityLevel
import com.example.data.model.TaskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

sealed class ParsedIntentResult {
    data class EventCreated(val event: CalendarEvent, val feedback: String) : ParsedIntentResult()
    data class TaskCreated(val task: TaskItem, val feedback: String) : ParsedIntentResult()
    data class ExpenseCreated(val expense: ExpenseItem, val feedback: String) : ParsedIntentResult()
    data class AssistantResponse(val text: String) : ParsedIntentResult()
}

class AssistantAiEngine {

    private val tag = "AssistantAiEngine"

    suspend fun processNaturalLanguage(
        input: String,
        existingEvents: List<CalendarEvent> = emptyList()
    ): ParsedIntentResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val geminiResult = callGeminiForIntent(trimmed, apiKey)
                if (geminiResult != null) {
                    return@withContext geminiResult
                }
            } catch (e: Exception) {
                Log.w(tag, "Gemini API failed or timed out, falling back to local NLP", e)
            }
        }

        // Reliable Local NLP parser fallback
        return@withContext fallbackLocalIntentParser(trimmed)
    }

    private suspend fun callGeminiForIntent(input: String, apiKey: String): ParsedIntentResult? {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val systemPrompt = """
            Você é o cérebro do Aura, um assistente pessoal inteligente.
            O usuário disse/escreveu: "$input"
            Data e hora atual: $now
            
            Analise a intenção e responda estritamente em formato JSON com uma das seguintes chaves:
            1. Se for agendar evento, reunião ou lembrete:
            {
               "type": "EVENT",
               "title": "título curto e claro",
               "isReminder": false ou true,
               "isMeeting": false ou true,
               "startTimeMillis": timestamp_millisegundos,
               "endTimeMillis": timestamp_millisegundos,
               "location": "local ou link se mencionado",
               "feedback": "mensagem de confirmação em português"
            }
            2. Se for registrar uma tarefa:
            {
               "type": "TASK",
               "title": "título da tarefa",
               "priority": "URGENT" | "HIGH" | "MEDIUM" | "LOW",
               "priorityScore": número de 1 a 100,
               "dueDateMillis": timestamp_millisegundos_ou_null,
               "category": "categoria (ex: Trabalho, Pessoal, Finanças)",
               "aiReasoning": "por que essa prioridade foi escolhida",
               "feedback": "mensagem de confirmação"
            }
            3. Se for despesa ou gasto financeiro:
            {
               "type": "EXPENSE",
               "title": "descrição do gasto",
               "amount": valor_numerico,
               "category": "Alimentação" | "Transporte" | "Moradia" | "Lazer" | "Saúde" | "Trabalho" | "Outros",
               "paymentMethod": "PIX" | "Cartão de Crédito" | "Débito" | "Dinheiro",
               "feedback": "mensagem de confirmação"
            }
            4. Se for apenas uma dúvida ou conversa:
            {
               "type": "CONVERSATION",
               "reply": "resposta útil, prestativa e sucinta do assistente"
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = input))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            )
        )

        val response = GeminiClient.service.generateContent(apiKey, request)
        val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return null

        return parseGeminiJsonResponse(rawJson)
    }

    private fun parseGeminiJsonResponse(jsonStr: String): ParsedIntentResult? {
        return try {
            val json = JSONObject(jsonStr)
            when (json.optString("type")) {
                "EVENT" -> {
                    val start = json.optLong("startTimeMillis", System.currentTimeMillis() + 3600000)
                    val end = json.optLong("endTimeMillis", start + 3600000)
                    val event = CalendarEvent(
                        title = json.optString("title", "Novo Evento"),
                        startTimeMillis = start,
                        endTimeMillis = end,
                        isReminder = json.optBoolean("isReminder", false),
                        isMeeting = json.optBoolean("isMeeting", false),
                        location = json.optString("location", ""),
                        source = "VOICE"
                    )
                    ParsedIntentResult.EventCreated(
                        event = event,
                        feedback = json.optString("feedback", "Evento registrado no calendário.")
                    )
                }
                "TASK" -> {
                    val priorityStr = json.optString("priority", "MEDIUM")
                    val priority = runCatching { PriorityLevel.valueOf(priorityStr) }.getOrDefault(PriorityLevel.MEDIUM)
                    val task = TaskItem(
                        title = json.optString("title", "Nova Tarefa"),
                        priority = priority,
                        priorityScore = json.optInt("priorityScore", 60),
                        dueDateMillis = if (json.has("dueDateMillis") && !json.isNull("dueDateMillis")) json.optLong("dueDateMillis") else null,
                        category = json.optString("category", "Geral"),
                        aiReasoning = json.optString("aiReasoning", "Priorizada automaticamente pela IA.")
                    )
                    ParsedIntentResult.TaskCreated(
                        task = task,
                        feedback = json.optString("feedback", "Tarefa adicionada à lista com prioridade.")
                    )
                }
                "EXPENSE" -> {
                    val expense = ExpenseItem(
                        title = json.optString("title", "Despesa"),
                        amount = json.optDouble("amount", 0.0),
                        category = json.optString("category", "Outros"),
                        paymentMethod = json.optString("paymentMethod", "PIX")
                    )
                    ParsedIntentResult.ExpenseCreated(
                        expense = expense,
                        feedback = json.optString("feedback", "Gasto computado com sucesso.")
                    )
                }
                "CONVERSATION" -> {
                    ParsedIntentResult.AssistantResponse(
                        text = json.optString("reply", "Como posso ajudar mais você?")
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing Gemini response JSON", e)
            null
        }
    }

    /**
     * Local autonomous priority calculation based on urgency, deadlines and keywords.
     */
    fun calculateAutonomousPriority(task: TaskItem): Pair<PriorityLevel, Int> {
        val lower = (task.title + " " + task.description).lowercase(Locale.ROOT)
        var score = 50

        // Urgency keywords
        if (lower.contains("urgente") || lower.contains("asap") || lower.contains("imediat") || lower.contains("crítico")) {
            score += 35
        }
        if (lower.contains("diretoria") || lower.contains("chefe") || lower.contains("cliente") || lower.contains("fiscal") || lower.contains("imposto") || lower.contains("pagar")) {
            score += 25
        }
        if (lower.contains("reunião") || lower.contains("apresentação") || lower.contains("entrega") || lower.contains("deadline")) {
            score += 20
        }
        if (lower.contains("estudar") || lower.contains("ler") || lower.contains("quando der") || lower.contains("depois")) {
            score -= 20
        }

        // Due date evaluation
        task.dueDateMillis?.let { due ->
            val diffHours = (due - System.currentTimeMillis()) / (1000 * 60 * 60)
            when {
                diffHours <= 0 -> score += 40 // Overdue!
                diffHours <= 24 -> score += 30 // Due today
                diffHours <= 48 -> score += 20 // Due tomorrow
                diffHours <= 168 -> score += 10 // Due this week
                else -> score -= 10
            }
        }

        score = score.coerceIn(5, 99)

        val level = when {
            score >= 80 -> PriorityLevel.URGENT
            score >= 65 -> PriorityLevel.HIGH
            score >= 40 -> PriorityLevel.MEDIUM
            else -> PriorityLevel.LOW
        }
        return Pair(level, score)
    }

    /**
     * Autonomously reorders all pending tasks by recalculated priority score and returns updated tasks.
     */
    suspend fun autonomouslyPrioritizeAllTasks(tasks: List<TaskItem>): List<TaskItem> = withContext(Dispatchers.Default) {
        tasks.map { task ->
            val (level, score) = calculateAutonomousPriority(task)
            val reasoning = when (level) {
                PriorityLevel.URGENT -> "Alta criticidade ou prazo iminente detectado. Requer ação prioritária imediata."
                PriorityLevel.HIGH -> "Impacto significativo identificado em objetivos de trabalho/prazos próximos."
                PriorityLevel.MEDIUM -> "Importância moderada. Pode ser executada no fluxo normal do dia."
                PriorityLevel.LOW -> "Atividade de baixo impacto imediato ou prazo estendido."
            }
            task.copy(
                priority = level,
                priorityScore = score,
                aiReasoning = reasoning
            )
        }.sortedByDescending { it.priorityScore }
    }

    /**
     * Autonomously evaluates a meeting invitation email, verifies calendar availability,
     * and drafts/sends an intelligent auto-response.
     */
    suspend fun generateAutonomousMeetingReply(
        email: EmailMessage,
        existingEvents: List<CalendarEvent>,
        apiKey: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val meetingTime = email.proposedMeetingTimeMillis ?: (System.currentTimeMillis() + 86400000)
        val meetingDuration = 60 * 60 * 1000L // 1 hour
        val meetingEnd = meetingTime + meetingDuration

        // Check for conflicts in existing calendar events
        val hasConflict = existingEvents.any { event ->
            (meetingTime < event.endTimeMillis) && (meetingEnd > event.startTimeMillis)
        }

        val df = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
        val formattedDate = df.format(Date(meetingTime))

        if (!hasConflict) {
            val response = "Olá ${email.senderName},\n\nVerifiquei minha agenda e estou disponível no horário proposto ($formattedDate). Confirmo minha presença na reunião \"${email.subject}\".\n\nAtenciosamente,\nAssistente Aura (Em nome do usuário)"
            return@withContext Pair(true, response)
        } else {
            // Propose alternative (+2 hours or next slot)
            val cal = Calendar.getInstance().apply {
                timeInMillis = meetingTime
                add(Calendar.HOUR_OF_DAY, 2)
            }
            val alternativeFormatted = df.format(cal.time)
            val response = "Olá ${email.senderName},\n\nObrigado pelo convite para \"${email.subject}\". Identifiquei um compromisso conflitante em minha agenda para $formattedDate. Seria possível reagendarmos para $alternativeFormatted?\n\nAtenciosamente,\nAssistente Aura (Em nome do usuário)"
            return@withContext Pair(false, response)
        }
    }

    /**
     * Local heuristic fallback parser for Portuguese voice/text commands.
     */
    private fun fallbackLocalIntentParser(input: String): ParsedIntentResult {
        val lower = input.lowercase(Locale("pt", "BR"))

        // 1. Expense Intent: "gastei 50 reais", "comprar 30 no mercado", "R$ 45,50 almoço", "almoço 35 no pix"
        val expenseMatch = extractExpense(input)
        if (expenseMatch != null) {
            return ParsedIntentResult.ExpenseCreated(
                expense = expenseMatch,
                feedback = "Gasto registrado: ${expenseMatch.title} - R$ ${String.format("%.2f", expenseMatch.amount)}"
            )
        }

        // 2. Calendar Event / Reminder / Meeting: "agendar reunião amanhã às 14h", "lembrete comprar café às 16h", "evento alinhamento sexta"
        if (lower.contains("agend") || lower.contains("lembr") || lower.contains("reuni") || lower.contains("compromisso") || lower.contains("marcar")) {
            val event = extractCalendarEvent(input)
            val actionType = if (event.isReminder) "Lembrete" else if (event.isMeeting) "Reunião" else "Evento"
            val df = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
            return ParsedIntentResult.EventCreated(
                event = event,
                feedback = "$actionType agendado: ${event.title} para ${df.format(Date(event.startTimeMillis))}"
            )
        }

        // 3. Task Intent: "tarefa", "fazer", "preciso", "entregar", "urgente"
        if (lower.contains("tarefa") || lower.contains("preciso") || lower.contains("fazer") || lower.contains("entregar") || lower.contains("pendência")) {
            val cleanTitle = input.replace(Regex("(?i)^(criar tarefa|nova tarefa|adicionar tarefa|preciso|lembrar de)\\s*"), "").trim()
            val task = TaskItem(
                title = if (cleanTitle.isNotBlank()) cleanTitle else "Nova Tarefa",
                category = if (lower.contains("trabalho") || lower.contains("relat") || lower.contains("cliente")) "Trabalho" else "Pessoal"
            )
            val (level, score) = calculateAutonomousPriority(task)
            val prioritizedTask = task.copy(priority = level, priorityScore = score)
            return ParsedIntentResult.TaskCreated(
                task = prioritizedTask,
                feedback = "Tarefa criada com prioridade ${level.label} (Score: $score)"
            )
        }

        // 4. Default Assistant Response
        return ParsedIntentResult.AssistantResponse(
            text = "Entendido! Você disse: \"$input\". Você pode pedir para eu agendar reuniões, criar lembretes, registrar gastos ou organizar suas tarefas por prioridade."
        )
    }

    private fun extractExpense(input: String): ExpenseItem? {
        val lower = input.lowercase(Locale("pt", "BR"))
        // Match numbers like "35,50", "50 reais", "R$ 45", "120.00"
        val regex = Pattern.compile("(?:r\\$|reais|valor de|custou|paguei|gastei)?\\s*(\\d+(?:[.,]\\d{1,2})?)\\s*(?:reais|r\\$)?", Pattern.CASE_INSENSITIVE)
        val matcher = regex.matcher(input)
        if (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", ".") ?: return null
            val amount = numStr.toDoubleOrNull() ?: return null

            var category = "Outros"
            when {
                lower.contains("almoço") || lower.contains("jantar") || lower.contains("lanche") || lower.contains("restaurante") || lower.contains("comida") || lower.contains("mercado") || lower.contains("café") -> category = "Alimentação"
                lower.contains("uber") || lower.contains("táxi") || lower.contains("gasolina") || lower.contains("combustível") || lower.contains("ônibus") || lower.contains("metrô") -> category = "Transporte"
                lower.contains("aluguel") || lower.contains("luz") || lower.contains("água") || lower.contains("internet") || lower.contains("condomínio") -> category = "Moradia"
                lower.contains("cinema") || lower.contains("jogo") || lower.contains("cerveja") || lower.contains("festa") -> category = "Lazer"
                lower.contains("farmácia") || lower.contains("remédio") || lower.contains("médico") || lower.contains("consulta") -> category = "Saúde"
                lower.contains("trabalho") || lower.contains("software") || lower.contains("servidor") || lower.contains("computador") -> category = "Trabalho"
            }

            var paymentMethod = "PIX"
            when {
                lower.contains("crédito") -> paymentMethod = "Cartão de Crédito"
                lower.contains("débito") -> paymentMethod = "Débito"
                lower.contains("dinheiro") || lower.contains("espécie") -> paymentMethod = "Dinheiro"
                lower.contains("pix") -> paymentMethod = "PIX"
            }

            val title = input.replace(Regex("(?i)(gastei|paguei|comprei|valor de|reais|r\\$|no pix|no débito|no crédito|em dinheiro|\\d+(?:[.,]\\d{1,2})?)"), "").trim()
            val finalTitle = if (title.isNotBlank()) title else "Gasto em $category"

            return ExpenseItem(
                title = finalTitle.capitalizeFirstLetter(),
                amount = amount,
                category = category,
                paymentMethod = paymentMethod
            )
        }
        return null
    }

    private fun extractCalendarEvent(input: String): CalendarEvent {
        val lower = input.lowercase(Locale("pt", "BR"))
        val isReminder = lower.contains("lembr")
        val isMeeting = lower.contains("reuni") || lower.contains("call") || lower.contains("alinhamento")

        val cal = Calendar.getInstance()
        if (lower.contains("amanhã")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else if (lower.contains("depois de amanhã")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
        }

        // Look for time like "às 14h", "às 15:30", "16 horas"
        val timeRegex = Pattern.compile("(?:às|as)?\\s*(\\d{1,2})(?:h|:(\\d{2})|\\s*horas)", Pattern.CASE_INSENSITIVE)
        val matcher = timeRegex.matcher(input)
        if (matcher.find()) {
            val hour = matcher.group(1)?.toIntOrNull() ?: 10
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
        } else {
            // default to next round hour
            cal.add(Calendar.HOUR_OF_DAY, 1)
            cal.set(Calendar.MINUTE, 0)
        }

        val start = cal.timeInMillis
        val end = start + (60 * 60 * 1000)

        val cleanTitle = input.replace(Regex("(?i)(agendar|marcar|lembrete|reunião de|reunião com|amanhã|às\\s*\\d+.*|as\\s*\\d+.*)"), "").trim()
        val title = if (cleanTitle.isNotBlank()) cleanTitle.capitalizeFirstLetter() else (if (isMeeting) "Reunião Importante" else "Compromisso")

        return CalendarEvent(
            title = title,
            startTimeMillis = start,
            endTimeMillis = end,
            isReminder = isReminder,
            isMeeting = isMeeting,
            source = "VOICE"
        )
    }

    private fun String.capitalizeFirstLetter(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
