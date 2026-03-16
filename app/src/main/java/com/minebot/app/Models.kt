package com.minebot.app

enum class AppTab {
    BOT, STATUS, SETTINGS
}

enum class ConnectionType(val apiValue: String) {
    ONLINE("online"),
    OFFLINE("offline");

    val title: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class SnackbarKind {
    SUCCESS, ERROR, INFO
}

data class SnackbarEvent(
    val message: String,
    val kind: SnackbarKind
)

data class ServerRecord(
    val id: String,
    val ip: String,
    val port: Int
) {
    val label: String get() = "$ip:$port"
}

data class LinkedAccount(
    val id: String,
    val label: String,
    val legacy: Boolean = false
)

data class PendingLink(
    val status: String = "none",
    val verificationUri: String? = null,
    val userCode: String? = null,
    val accountId: String? = null,
    val error: String? = null,
    val expiresAt: Long? = null
)

data class BotStatus(
    val sessionId: String? = null,
    val status: String = "offline",
    val connected: Boolean = false,
    val isReconnecting: Boolean = false,
    val reconnectAttempt: Int = 0,
    val server: String? = null,
    val startedAt: Long? = null,
    val uptimeMs: Long? = null,
    val lastConnectedAt: Long? = null,
    val lastError: String? = null,
    val lastDisconnectReason: String? = null,
    val connectionType: String? = null,
    val accountId: String? = null
)

data class HealthStatus(
    val status: String = "ok",
    val uptimeSec: Long = 0,
    val bots: Int = 0,
    val memoryMb: Int = 0,
    val maxBots: Int = 20
)

data class MeInfo(
    val userId: String = "",
    val createdAt: Long? = null,
    val lastActive: Long? = null,
    val connectionType: String = "online",
    val bedrockVersion: String = "auto"
)

data class AuthRedeemResult(
    val token: String,
    val userId: String
)

data class MineBotUiState(
    val tutorialSeen: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isBusy: Boolean = false,
    val selectedTab: AppTab = AppTab.BOT,
    val accessCode: String = "",
    val me: MeInfo? = null,
    val linkedAccounts: List<LinkedAccount> = emptyList(),
    val pendingLink: PendingLink? = null,
    val botStatus: BotStatus = BotStatus(),
    val health: HealthStatus? = null,
    val serverLatencyMs: Long? = null,
    val servers: List<ServerRecord> = emptyList(),
    val selectedServerId: String? = null,
    val connectionType: ConnectionType = ConnectionType.ONLINE,
    val offlineUsername: String = "",
    val addServerIp: String = "",
    val addServerPort: String = "19132",
    val showAddServerDialog: Boolean = false,
    val snackbarEvent: SnackbarEvent? = null
) {
    val selectedServer: ServerRecord?
        get() = servers.firstOrNull { it.id == selectedServerId } ?: servers.firstOrNull()
}
