package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.AssistantViewModel
import com.example.ui.components.AiBannerFeedback
import com.example.ui.screens.AssistantHomeScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.EmailsScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAssistantApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAssistantApp(viewModel: AssistantViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val lastFeedback by viewModel.lastAiFeedback.collectAsState()

    val navItems = listOf(
        NavigationItem("Assistente", Icons.Default.AutoAwesome, "nav_assistant"),
        NavigationItem("Agenda", Icons.Default.CalendarMonth, "nav_calendar"),
        NavigationItem("Tarefas", Icons.Default.CheckCircle, "nav_tasks"),
        NavigationItem("Gastos", Icons.Default.AttachMoney, "nav_expenses"),
        NavigationItem("E-mails", Icons.Default.Email, "nav_emails")
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("aura_main_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> AssistantHomeScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { selectedTab = it }
                )
                1 -> CalendarScreen(viewModel = viewModel)
                2 -> TasksScreen(viewModel = viewModel)
                3 -> ExpensesScreen(viewModel = viewModel)
                4 -> EmailsScreen(viewModel = viewModel)
            }

            // Top AI Activity Feedback Banner
            AiBannerFeedback(
                feedback = lastFeedback,
                onDismiss = { viewModel.clearFeedback() }
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
