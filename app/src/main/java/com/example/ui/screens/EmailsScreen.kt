package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmailMessage
import com.example.data.model.EmailStatus
import com.example.ui.AssistantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmailsScreen(viewModel: AssistantViewModel) {
    val emails by viewModel.allEmails.collectAsState()
    val autoResponderEnabled by viewModel.autoResponderEnabled.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var showSimulateOptions by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("emails_screen"),
        contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "E-mails & Reuniões",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Monitoramento e respostas automáticas inteligentes para reuniões",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Auto-responder Master Switch Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (autoResponderEnabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (autoResponderEnabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Respostas Automáticas de Reuniões",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (autoResponderEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (autoResponderEnabled) "Ativado • IA analisa agenda em tempo real"
                                    else "Desativado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (autoResponderEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Switch(
                            checked = autoResponderEnabled,
                            onCheckedChange = { viewModel.toggleAutoResponder(it) },
                            modifier = Modifier.testTag("toggle_auto_responder")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Quando ativado, ao receber convites de reuniões importantes, o assistente verifica seus compromissos no calendário. Se houver vaga, confirma presença e adiciona à agenda. Se houver conflito, propõe educadamente um novo horário.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Simulation / Test Button Panel
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Testar Fluxo Autônomo com IA",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(onClick = { showSimulateOptions = !showSimulateOptions }) {
                            Text(if (showSimulateOptions) "Ocultar" else "Ver Testes")
                            Icon(
                                imageVector = if (showSimulateOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = showSimulateOptions) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Simule a chegada de um novo e-mail para ver a resposta automática e agendamento em ação:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.simulateIncomingMeeting(
                                            subject = "Alinhamento de Parceria Estratégica",
                                            sender = "Carlos Mendes (VP de Operações)",
                                            hoursInFuture = 28,
                                            hasConflict = false
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("simulate_free_meeting_button")
                                ) {
                                    Text("Reunião (Agenda Livre)", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.simulateIncomingMeeting(
                                            subject = "Call Urgente de Planejamento",
                                            sender = "Fernanda Lima (Gerente de Projetos)",
                                            hoursInFuture = 30,
                                            hasConflict = true
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("simulate_conflict_meeting_button")
                                ) {
                                    Text("Reunião (Com Conflito)", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Email List Header
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Caixa de Entrada & Convites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${emails.size} mensagens",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (emails.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum e-mail recebido no momento.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(emails, key = { it.id }) { email ->
                EmailCard(
                    email = email,
                    isProcessing = isProcessing,
                    onTriggerAutoReply = { viewModel.autonomouslyReplyToEmailMeeting(email) }
                )
            }
        }
    }
}

@Composable
fun EmailCard(
    email: EmailMessage,
    isProcessing: Boolean,
    onTriggerAutoReply: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val receivedDate = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(email.receivedAt))

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("email_item_${email.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Sender and Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (email.isMeetingInvite) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (email.isMeetingInvite) Icons.Default.Videocam else Icons.Default.Email,
                            contentDescription = null,
                            tint = if (email.isMeetingInvite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = email.senderName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = email.senderEmail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Text(
                    text = receivedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = email.subject,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Meeting proposal details
            if (email.isMeetingInvite && email.proposedMeetingTimeMillis != null) {
                val meetingDateStr = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
                    .format(Date(email.proposedMeetingTimeMillis))
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Horário proposto: $meetingDateStr",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-reply Status Badge or Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (email.autoReplySent) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (email.status) {
                            EmailStatus.ACCEPTED -> Color(0xFFE8F5E9)
                            EmailStatus.DECLINED -> Color(0xFFFFF3E0)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = when (email.status) {
                                    EmailStatus.ACCEPTED -> Color(0xFF2E7D32)
                                    EmailStatus.DECLINED -> Color(0xFFE65100)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (email.status) {
                                    EmailStatus.ACCEPTED -> "Auto-Resposta: Aceito & Agendado"
                                    EmailStatus.DECLINED -> "Auto-Resposta: Conflito / Reagendamento"
                                    else -> "Respondido pela IA"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (email.status) {
                                    EmailStatus.ACCEPTED -> Color(0xFF2E7D32)
                                    EmailStatus.DECLINED -> Color(0xFFE65100)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }

                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Ocultar resposta" else "Ver resposta")
                    }
                } else if (email.isMeetingInvite) {
                    Button(
                        onClick = onTriggerAutoReply,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("reply_now_button_${email.id}")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Responder com IA", fontSize = 12.sp)
                    }
                }
            }

            // Expanded Auto-reply transcript
            AnimatedVisibility(visible = expanded && email.autoReplyContent != null) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Transcrição da resposta enviada automaticamente:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = email.autoReplyContent ?: "",
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
