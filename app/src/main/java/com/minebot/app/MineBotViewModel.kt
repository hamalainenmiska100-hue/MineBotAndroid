package com.minebot.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MineBotViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = SecurePrefs(application)
    private val api = ApiClient()

    var uiState = androidx.compose.runtime.mutableStateOf(
        MineBotUiState(
            tutorialSeen = prefs.getTutorialSeen(),
            isLoggedIn = !prefs.getToken().isNullOrBlank(),
            servers = prefs.getServers(),
            selectedServerId = prefs.getSelectedServerId()
        )
    )
        private set

    private var pollJob: Job? = null

    init {
        if (uiState.value.isLoggedIn) {
            refreshAll()
            startPolling()
        }
    }

    fun updateAccessCode(value: String) {
        val normalized = value.uppercase()
            .replace(" ", "")
            .take(14)
        uiState.value = uiState.value.copy(accessCode = normalized)
    }

    fun completeTutorial() {
        prefs.setTutorialSeen(true)
        uiState.value = uiState.value.copy(tutorialSeen = true)
    }

    fun dismissSnackbar() {
        uiState.value = uiState.value.copy(snackbarEvent = null)
    }

    fun selectTab(tab: AppTab) {
        uiState.value = uiState.value.copy(selectedTab = tab)
    }

    fun selectServer(id: String) {
        prefs.setSelectedServerId(id)
        uiState.value = uiState.value.copy(selectedServerId = id)
    }

    fun setConnectionType(type: ConnectionType) {
        uiState.value = uiState.value.copy(connectionType = type)
    }

    fun setOfflineUsername(value: String) {
        uiState.value = uiState.value.copy(offlineUsername = value)
    }

    fun showAddServerDialog(show: Boolean) {
        uiState.value = uiState.value.copy(showAddServerDialog = show)
    }

    fun setAddServerIp(value: String) {
        uiState.value = uiState.value.copy(addServerIp = value)
    }

    fun setAddServerPort(value: String) {
        uiState.value = uiState.value.copy(addServerPort = value.filter { it.isDigit() }.take(5))
    }

    fun addServer() {
        val state = uiState.value
        val ip = state.addServerIp.trim()
        val port = state.addServerPort.toIntOrNull() ?: 19132

        if (ip.isBlank()) {
            postSnackbar("Enter a server IP first.", SnackbarKind.ERROR)
            return
        }

        val server = ServerRecord(
            id = UUID.randomUUID().toString(),
            ip = ip,
            port = port
        )

        val updated = state.servers + server
        prefs.setServers(updated)
        prefs.setSelectedServerId(server.id)
        uiState.value = state.copy(
            servers = updated,
            selectedServerId = server.id,
            addServerIp = "",
            addServerPort = "19132",
            showAddServerDialog = false,
            snackbarEvent = SnackbarEvent("Server added.", SnackbarKind.SUCCESS)
        )
    }

    fun deleteServer(id: String) {
        val updated = uiState.value.servers.filterNot { it.id == id }
        prefs.setServers(updated)
        val selected = uiState.value.selectedServerId.takeIf { sid -> updated.any { it.id == sid } } ?: updated.firstOrNull()?.id
        prefs.setSelectedServerId(selected)
        uiState.value = uiState.value.copy(
            servers = updated,
            selectedServerId = selected,
            snackbarEvent = SnackbarEvent("Server removed.", SnackbarKind.INFO)
        )
    }

    fun logout() {
        pollJob?.cancel()
        prefs.setToken(null)
        uiState.value = MineBotUiState(
            tutorialSeen = prefs.getTutorialSeen(),
            isLoggedIn = false,
            servers = prefs.getServers(),
            selectedServerId = prefs.getSelectedServerId(),
            snackbarEvent = SnackbarEvent("Logged out.", SnackbarKind.INFO)
        )
    }

    fun login() {
        val code = uiState.value.accessCode.trim()
        if (code.length < 8) {
            postSnackbar("Enter a valid access code.", SnackbarKind.ERROR)
            return
        }

        viewModelScope.launch {
            setBusy(true)
            runCatching { api.redeemCode(code) }
                .onSuccess { result ->
                    prefs.setToken(result.token)
                    uiState.value = uiState.value.copy(
                        isLoggedIn = true,
                        accessCode = "",
                        snackbarEvent = SnackbarEvent("Logged in.", SnackbarKind.SUCCESS)
                    )
                    refreshAll()
                    startPolling()
                }
                .onFailure {
                    postSnackbar(it.message ?: "Login failed.", SnackbarKind.ERROR)
                }
            setBusy(false)
        }
    }

    fun startBot() {
        val token = prefs.getToken() ?: return
        val server = uiState.value.selectedServer
        if (server == null) {
            postSnackbar("Add a server first.", SnackbarKind.ERROR)
            selectTab(AppTab.SETTINGS)
            return
        }

        viewModelScope.launch {
            setBusy(true)
            runCatching {
                api.startBot(
                    token = token,
                    server = server,
                    connectionType = uiState.value.connectionType,
                    offlineUsername = uiState.value.offlineUsername
                )
            }
                .onSuccess { status ->
                    uiState.value = uiState.value.copy(
                        botStatus = status.copy(server = server.label),
                        snackbarEvent = SnackbarEvent("Bot starting…", SnackbarKind.SUCCESS)
                    )
                    refreshAll()
                    refreshBurstAfterStart()
                }
                .onFailure {
                    postSnackbar(it.message ?: "Failed to start bot.", SnackbarKind.ERROR)
                }
            setBusy(false)
        }
    }

    fun stopBot() {
        val token = prefs.getToken() ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { api.stopBot(token) }
                .onSuccess {
                    uiState.value = uiState.value.copy(
                        botStatus = BotStatus(server = uiState.value.selectedServer?.label),
                        snackbarEvent = SnackbarEvent("Bot stopped.", SnackbarKind.INFO)
                    )
                    refreshAll()
                }
                .onFailure {
                    postSnackbar(it.message ?: "Failed to stop bot.", SnackbarKind.ERROR)
                }
            setBusy(false)
        }
    }

    fun reconnectBot() {
        val token = prefs.getToken() ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { api.reconnectBot(token) }
                .onSuccess {
                    postSnackbar("Reconnect requested.", SnackbarKind.SUCCESS)
                    refreshAll()
                }
                .onFailure {
                    postSnackbar(it.message ?: "Failed to reconnect bot.", SnackbarKind.ERROR)
                }
            setBusy(false)
        }
    }

    fun startMicrosoftLink() {
        val token = prefs.getToken() ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { api.startMicrosoftLink(token) }
                .onSuccess {
                    uiState.value = uiState.value.copy(
                        pendingLink = it,
                        snackbarEvent = SnackbarEvent("Microsoft link started.", SnackbarKind.SUCCESS)
                    )
                }
                .onFailure {
                    postSnackbar(it.message ?: "Failed to start link.", SnackbarKind.ERROR)
                }
            setBusy(false)
        }
    }

    fun refreshLinkStatus() {
        val token = prefs.getToken() ?: return
        viewModelScope.launch {
            runCatching { api.fetchMicrosoftLinkStatus(token) }
                .onSuccess { uiState.value = uiState.value.copy(pendingLink = it) }
                .onFailure { postSnackbar(it.message ?: "Link status failed.", SnackbarKind.ERROR) }
        }
    }

    fun unlinkFirstAccount() {
        val token = prefs.getToken() ?: return
        val accountId = uiState.value.linkedAccounts.firstOrNull()?.id
        viewModelScope.launch {
            setBusy(true)
            runCatching { api.unlinkAccount(token, accountId) }
                .onSuccess {
                    postSnackbar("Account unlinked.", SnackbarKind.INFO)
                    refreshAccountsOnly()
                }
                .onFailure { postSnackbar(it.message ?: "Failed to unlink.", SnackbarKind.ERROR) }
            setBusy(false)
        }
    }

    fun refreshAll() {
        val token = prefs.getToken()
        if (token.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { api.fetchMe(token) }.onSuccess { me ->
                uiState.value = uiState.value.copy(me = me)
            }
            refreshAccountsOnly()
            runCatching { api.fetchBotStatus(token) }.onSuccess { bot ->
                uiState.value = uiState.value.copy(botStatus = bot)
            }
            runCatching { api.fetchHealth() }.onSuccess { health ->
                uiState.value = uiState.value.copy(health = health)
            }
            runCatching { api.pingHealth() }.onSuccess { latency ->
                uiState.value = uiState.value.copy(serverLatencyMs = latency)
            }
        }
    }

    private fun refreshAccountsOnly() {
        val token = prefs.getToken()
        if (token.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { api.fetchAccounts(token) }
                .onSuccess { (accounts, pending) ->
                    uiState.value = uiState.value.copy(
                        linkedAccounts = accounts,
                        pendingLink = pending
                    )
                }
        }
    }

    private fun refreshBurstAfterStart() {
        val token = prefs.getToken() ?: return
        viewModelScope.launch {
            repeat(8) {
                runCatching { api.fetchBotStatus(token) }
                    .onSuccess { bot -> uiState.value = uiState.value.copy(botStatus = bot) }
                delay(2000)
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                refreshAll()
                delay(30_000)
            }
        }
    }

    private fun postSnackbar(message: String, kind: SnackbarKind) {
        uiState.value = uiState.value.copy(snackbarEvent = SnackbarEvent(message, kind))
    }

    private fun setBusy(value: Boolean) {
        uiState.value = uiState.value.copy(isBusy = value)
    }
}
