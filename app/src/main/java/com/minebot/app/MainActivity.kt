package com.minebot.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MineBotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MineBotTheme {
                MineBotRoot(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MineBotRoot(viewModel: MineBotViewModel) {
    val state = viewModel.uiState.value
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.snackbarEvent) {
        val event = state.snackbarEvent ?: return@LaunchedEffect
        when (event.kind) {
            SnackbarKind.SUCCESS -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            SnackbarKind.ERROR -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            SnackbarKind.INFO -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
        viewModel.dismissSnackbar()
    }

    if (!state.tutorialSeen) {
        TutorialScreen(onContinue = viewModel::completeTutorial)
        return
    }

    if (!state.isLoggedIn) {
        LoginScreen(
            accessCode = state.accessCode,
            onAccessCodeChange = viewModel::updateAccessCode,
            onLogin = viewModel::login,
            busy = state.isBusy
        )
        return
    }

    val topTitle = when (state.selectedTab) {
        AppTab.BOT -> "Bot"
        AppTab.STATUS -> "Status"
        AppTab.SETTINGS -> "Settings"
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = topTitle,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                NavigationBarItem(
                    selected = state.selectedTab == AppTab.BOT,
                    onClick = { viewModel.selectTab(AppTab.BOT) },
                    icon = {
                        Icon(
                            imageVector = if (state.selectedTab == AppTab.BOT) Icons.Filled.SmartToy else Icons.Outlined.SmartToy,
                            contentDescription = "Bot"
                        )
                    },
                    label = { Text("Bot") },
                    colors = NavigationBarItemDefaults.colors()
                )
                NavigationBarItem(
                    selected = state.selectedTab == AppTab.STATUS,
                    onClick = { viewModel.selectTab(AppTab.STATUS) },
                    icon = {
                        Icon(
                            imageVector = if (state.selectedTab == AppTab.STATUS) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                            contentDescription = "Status"
                        )
                    },
                    label = { Text("Status") }
                )
                NavigationBarItem(
                    selected = state.selectedTab == AppTab.SETTINGS,
                    onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (state.selectedTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = state.selectedTab,
                label = "tabs",
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { tab ->
                when (tab) {
                    AppTab.BOT -> BotTab(state = state, viewModel = viewModel)
                    AppTab.STATUS -> StatusTab(state = state)
                    AppTab.SETTINGS -> SettingsTab(state = state, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun TutorialScreen(onContinue: () -> Unit) {
    val contentPadding = WindowInsets.statusBars.asPaddingValues()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Spacer(Modifier.width(1.dp))
        Text("MineBot", style = MaterialTheme.typography.displaySmall)
        Text("Welcome.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    "This app lets you run a Minecraft AFK bot remotely from your phone.",
                    style = MaterialTheme.typography.titleMedium
                )
                TutorialStep(number = "1", text = "Enter your access code")
                TutorialStep(number = "2", text = "Link your Microsoft account")
                TutorialStep(number = "3", text = "Start your bot")
                Text(
                    "Your bot will stay online even when the app is closed.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        PrimaryButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TutorialStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            ) {
                Text(
                    text = number,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LoginScreen(
    accessCode: String,
    onAccessCodeChange: (String) -> Unit,
    onLogin: () -> Unit,
    busy: Boolean
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.width(1.dp))
        Text("MineBot", style = MaterialTheme.typography.displaySmall)
        Text("Enter your access code", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                MineBotTextField(
                    value = accessCode,
                    onValueChange = onAccessCodeChange,
                    label = "Access code",
                    placeholder = "XXXX-XXXX-XXXX"
                )
                PrimaryButton(
                    text = if (busy) "Signing in…" else "Login",
                    onClick = onLogin,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BotTab(state: MineBotUiState, viewModel: MineBotViewModel) {
    val scroll = rememberScrollState()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        StatusCard(state.botStatus)

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Server", style = MaterialTheme.typography.titleLarge)

                ServerPicker(
                    servers = state.servers,
                    selectedServerId = state.selectedServer?.id,
                    onServerSelected = viewModel::selectServer
                )

                LabeledValue("IP Address", state.selectedServer?.ip ?: "No server")
                LabeledValue("Port", state.selectedServer?.port?.toString() ?: "—")

                Text("Connection Type", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = state.connectionType == ConnectionType.ONLINE,
                        onClick = { viewModel.setConnectionType(ConnectionType.ONLINE) },
                        label = { Text("Online") }
                    )
                    FilterChip(
                        selected = state.connectionType == ConnectionType.OFFLINE,
                        onClick = { viewModel.setConnectionType(ConnectionType.OFFLINE) },
                        label = { Text("Offline") }
                    )
                }

                if (state.connectionType == ConnectionType.OFFLINE) {
                    MineBotTextField(
                        value = state.offlineUsername,
                        onValueChange = viewModel::setOfflineUsername,
                        label = "Offline username",
                        placeholder = "Steve"
                    )
                }

                PrimaryButton(
                    text = "Start Bot",
                    onClick = viewModel::startBot,
                    enabled = !state.isBusy && state.selectedServer != null,
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Start
                )
                PrimaryButton(
                    text = "Reconnect",
                    onClick = viewModel::reconnectBot,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Primary
                )
                PrimaryButton(
                    text = "Stop Bot",
                    onClick = viewModel::stopBot,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Stop
                )
            }
        }

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Microsoft", style = MaterialTheme.typography.titleLarge)

                if (state.linkedAccounts.isEmpty()) {
                    Text("No linked account yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.linkedAccounts.forEach {
                        LabeledValue("Account", it.label)
                    }
                }

                if (state.pendingLink != null && state.pendingLink.status != "none") {
                    LabeledValue("Link status", state.pendingLink.status)
                    state.pendingLink.verificationUri?.let { LabeledValue("Open", it) }
                    state.pendingLink.userCode?.let { LabeledValue("Code", it) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(
                        text = "Link Microsoft",
                        onClick = viewModel::startMicrosoftLink,
                        modifier = Modifier.weight(1f),
                        kind = ButtonKind.Primary
                    )
                    OutlinedButton(
                        onClick = viewModel::unlinkFirstAccount,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unlink")
                    }
                }
            }
        }

        Spacer(Modifier.padding(bottom = bottomPadding + 8.dp))
    }
}

@Composable
private fun StatusTab(state: MineBotUiState) {
    val scroll = rememberScrollState()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        StatusCard(state.botStatus)

        MetricCard(
            title = "Server Latency",
            value = state.serverLatencyMs?.let { "$it ms" } ?: "—"
        )
        MetricCard(
            title = "Memory Usage",
            value = state.health?.memoryMb?.let { "$it MB" } ?: "—",
            subtitle = "Current backend memory use"
        )
        MetricCard(
            title = "Global Memory Usage",
            value = state.health?.let { "${it.memoryMb} MB / ${it.maxBots} bots max" } ?: "—",
            subtitle = "Shared backend status"
        )
        MetricCard(
            title = "Active Bots",
            value = state.health?.let { "${it.bots} / ${it.maxBots}" } ?: "—"
        )

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Account", style = MaterialTheme.typography.titleLarge)
                LabeledValue("User ID", state.me?.userId ?: "—")
                LabeledValue("Connection Type", state.me?.connectionType ?: "—")
                LabeledValue("Bedrock Version", state.me?.bedrockVersion ?: "auto")
            }
        }

        Spacer(Modifier.padding(bottom = bottomPadding + 8.dp))
    }
}

@Composable
private fun SettingsTab(state: MineBotUiState, viewModel: MineBotViewModel) {
    val uriHandler = LocalUriHandler.current
    val scroll = rememberScrollState()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (state.showAddServerDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showAddServerDialog(false) },
            confirmButton = {
                PrimaryButton(text = "Save", onClick = viewModel::addServer, kind = ButtonKind.Primary)
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.showAddServerDialog(false) }) {
                    Text("Cancel")
                }
            },
            title = { Text("Add Server") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MineBotTextField(
                        value = state.addServerIp,
                        onValueChange = viewModel::setAddServerIp,
                        label = "IP Address",
                        placeholder = "play.example.net"
                    )
                    MineBotTextField(
                        value = state.addServerPort,
                        onValueChange = viewModel::setAddServerPort,
                        label = "Port",
                        placeholder = "19132"
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Servers", style = MaterialTheme.typography.titleLarge)

                if (state.servers.isEmpty()) {
                    Text("No saved servers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.servers.forEach { server ->
                        Card(
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(server.label, style = MaterialTheme.typography.titleMedium)
                                    if (state.selectedServer?.id == server.id) {
                                        Text("Selected", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { viewModel.selectServer(server.id) }) {
                                        Text("Use")
                                    }
                                    OutlinedButton(onClick = { viewModel.deleteServer(server.id) }) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }

                PrimaryButton(
                    text = "Add Server",
                    onClick = { viewModel.showAddServerDialog(true) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Community", style = MaterialTheme.typography.titleLarge)
                PrimaryButton(
                    text = "Join our Discord",
                    onClick = { uriHandler.openUri("https://discord.gg/CNZsQDBYvw") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("About", style = MaterialTheme.typography.titleLarge)
                Text("Made with love ❤️", style = MaterialTheme.typography.titleMedium)
                Text("Developer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("@ilovecatssm2", style = MaterialTheme.typography.titleMedium)

                OutlinedButton(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log out")
                }
            }
        }

        Spacer(Modifier.padding(bottom = bottomPadding + 8.dp))
    }
}
