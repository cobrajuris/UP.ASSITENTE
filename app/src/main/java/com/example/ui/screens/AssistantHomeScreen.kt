package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AssistantLog
import com.example.data.model.PriorityLevel
import com.example.speech.VoiceState
import com.example.ui.AssistantViewModel
import com.example.ui.components.VoiceInputDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssistantHomeScreen(
    viewModel: AssistantViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val context = LocalContext.current
    val isProcessing by viewModel.isProcessing.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val totalSpent by viewModel.totalSpentThisMonth.collectAsState()
    val budgetLimit by viewModel.monthlyBudgetLimit.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()

    var showVoiceDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.voiceHelper.startListening()
            showVoiceDialog = true
        }
    }

    if (showVoiceDialog) {
        VoiceInputDialog(
            voiceState = voiceState,
            onStopListening = { viewModel.voiceHelper.stopListening() },
            onStartListening = { viewModel.voiceHelper.startListening() },
            onDismiss = {
                viewModel.voiceHelper.reset()
                showVoiceDialog = false
            },
            onSubmitText = { text ->
                viewModel.processInput(text)
            }
        )
    }

    val urgentTasksCount = tasks.count { !it.isCompleted && (it.priority == PriorityLevel.URGENT || it.priority == PriorityLevel.HIGH) }
    val meetingsCount = events.count { it.isMeeting }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("assistant_home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00C853))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AURA ASSISTENTE ATIVO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Olá! Como posso ajudar hoje?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Autonomous AI Indicator Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Autônomo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hero Image Banner
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.assistant_hero_1790105431308),
                            contentDescription = "Assistente Aura Ilustração",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Command Bar (Text + Voice button)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        text = "Fale ou digite qualquer pedido...",
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_command_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent
                                )
                            )

                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(4.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                if (inputText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            viewModel.processInput(inputText)
                                            inputText = ""
                                        },
                                        modifier = Modifier.testTag("send_command_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Enviar comando",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val permissionCheck = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                            viewModel.voiceHelper.startListening()
                                            showVoiceDialog = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .testTag("home_mic_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Falar com Aura",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Voice Command Suggestion Chips
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Exemplos de comandos por voz:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    "📅 Agendar reunião amanhã às 15h com diretoria",
                    "💰 Gastei R$ 42,50 no almoço no débito",
                    "✓ Preciso entregar balancete fiscal urgente",
                    "⏰ Lembrete: pagar fatura do cartão sexta às 10h",
                    "✉️ Checar convites de reunião e responder"
                )
                items(suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = {
                            viewModel.processInput(suggestion.substring(2).trim())
                        },
                        label = { Text(suggestion, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Dashboard Summary Metrics
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Resumo do seu dia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Calendar Metric Card
                MetricCard(
                    title = "Reuniões",
                    value = "$meetingsCount",
                    subtitle = "Marcadas",
                    icon = Icons.Default.CalendarMonth,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(1) }
                )

                // Tasks Metric Card
                MetricCard(
                    title = "Tarefas",
                    value = "$urgentTasksCount",
                    subtitle = "Urgentes",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFFE65100),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(2) }
                )

                // Expenses Metric Card
                MetricCard(
                    title = "Gastos Mês",
                    value = "R$ ${String.format(Locale("pt", "BR"), "%.0f", totalSpent ?: 0.0)}",
                    subtitle = "de R$ ${String.format(Locale("pt", "BR"), "%.0f", budgetLimit)}",
                    icon = Icons.Default.AttachMoney,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable { onNavigateToTab(3) }
                )
            }
        }

        // Autonomous AI Action Center
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ações Autônomas Inteligentes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "O Aura gerencia reuniões importantes, responde e-mails avaliando sua agenda e prioriza tarefas automaticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.autonomouslyPrioritizeTasks() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("quick_prioritize_button")
                        ) {
                            Text("Priorizar Tarefas", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onNavigateToTab(4) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("quick_emails_button")
                        ) {
                            Text("Gerenciar E-mails", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Live Feed of Recent Autonomous Logs
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Atividades Autônomas Recentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${recentLogs.size} registros",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (recentLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Nenhuma atividade registrada ainda. Use o microfone ou digite acima para começar!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentLogs) { log ->
                LogItemRow(log = log)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun LogItemRow(log: AssistantLog) {
    val (icon, color) = when (log.actionType) {
        "CALENDAR" -> Icons.Default.CalendarMonth to MaterialTheme.colorScheme.primary
        "TASK_PRIORITIZATION" -> Icons.Default.AutoAwesome to Color(0xFFE65100)
        "EXPENSE" -> Icons.Default.AttachMoney to Color(0xFF2E7D32)
        "EMAIL_AUTOREPLY", "EMAIL" -> Icons.Default.Email to Color(0xFF6200EE)
        else -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.secondary
    }

    val timeFormatted = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(log.timestamp))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
