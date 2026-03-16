package com.minebot.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(
    private val baseUrl: String = "https://afkbotb.fly.dev"
) {
    private suspend fun request(
        path: String,
        method: String = "GET",
        token: String? = null,
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val url = URL(baseUrl + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            doInput = true
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toString().toByteArray()) }
            }
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
        val json = runCatching { JSONObject(text) }.getOrElse { JSONObject().put("success", false).put("error", text) }

        if (code !in 200..299) {
            throw IllegalStateException(json.optString("error", "Request failed with status $code"))
        }

        json
    }

    suspend fun redeemCode(code: String): AuthRedeemResult {
        val body = JSONObject().put("code", code)
        val data = request("/auth/redeem", method = "POST", body = body).optJSONObject("data") ?: JSONObject()
        return AuthRedeemResult(
            token = data.optString("token"),
            userId = data.optString("userId")
        )
    }

    suspend fun fetchMe(token: String): MeInfo {
        val data = request("/auth/me", token = token).optJSONObject("data") ?: JSONObject()
        return MeInfo(
            userId = data.optString("userId"),
            createdAt = data.optLongOrNull("createdAt"),
            lastActive = data.optLongOrNull("lastActive"),
            connectionType = data.optString("connectionType", "online"),
            bedrockVersion = data.optString("bedrockVersion", "auto")
        )
    }

    suspend fun fetchAccounts(token: String): Pair<List<LinkedAccount>, PendingLink?> {
        val data = request("/accounts", token = token).optJSONObject("data") ?: JSONObject()
        val linked = data.optJSONArray("linked") ?: JSONArray()
        val accounts = buildList {
            for (i in 0 until linked.length()) {
                val obj = linked.optJSONObject(i) ?: continue
                add(
                    LinkedAccount(
                        id = obj.optString("id"),
                        label = obj.optString("label", "Account $${i + 1}"),
                        legacy = obj.optBoolean("legacy", false)
                    )
                )
            }
        }

        val pendingObj = data.optJSONObject("pendingLink")
        val pending = pendingObj?.let {
            PendingLink(
                status = it.optString("status", "none"),
                verificationUri = it.optStringOrNull("verificationUri"),
                userCode = it.optStringOrNull("userCode"),
                accountId = it.optStringOrNull("accountId"),
                error = it.optStringOrNull("error"),
                expiresAt = it.optLongOrNull("expiresAt")
            )
        }

        return accounts to pending
    }

    suspend fun startMicrosoftLink(token: String): PendingLink {
        val data = request("/accounts/link/start", method = "POST", token = token).optJSONObject("data") ?: JSONObject()
        return PendingLink(
            status = data.optString("status", "pending"),
            verificationUri = data.optStringOrNull("verificationUri"),
            userCode = data.optStringOrNull("userCode"),
            accountId = data.optStringOrNull("accountId")
        )
    }

    suspend fun fetchMicrosoftLinkStatus(token: String): PendingLink {
        val data = request("/accounts/link/status", token = token).optJSONObject("data") ?: JSONObject()
        return PendingLink(
            status = data.optString("status", "none"),
            verificationUri = data.optStringOrNull("verificationUri"),
            userCode = data.optStringOrNull("userCode"),
            accountId = data.optStringOrNull("accountId"),
            error = data.optStringOrNull("error"),
            expiresAt = data.optLongOrNull("expiresAt")
        )
    }

    suspend fun unlinkAccount(token: String, accountId: String? = null) {
        val body = JSONObject()
        if (!accountId.isNullOrBlank()) body.put("accountId", accountId)
        request("/accounts/unlink", method = "POST", token = token, body = body)
    }

    suspend fun startBot(
        token: String,
        server: ServerRecord,
        connectionType: ConnectionType,
        offlineUsername: String
    ): BotStatus {
        val body = JSONObject()
            .put("ip", server.ip)
            .put("port", server.port)
            .put("connectionType", connectionType.apiValue)

        if (connectionType == ConnectionType.OFFLINE && offlineUsername.isNotBlank()) {
            body.put("offlineUsername", offlineUsername)
        }

        val data = request("/bots/start", method = "POST", token = token, body = body).optJSONObject("data") ?: JSONObject()
        return BotStatus(
            sessionId = data.optStringOrNull("sessionId"),
            status = data.optString("status", "starting"),
            connected = data.optBoolean("connected", false),
            server = data.optStringOrNull("server"),
            connectionType = data.optStringOrNull("connectionType")
        )
    }

    suspend fun stopBot(token: String) {
        request("/bots/stop", method = "POST", token = token)
    }

    suspend fun reconnectBot(token: String) {
        request("/bots/reconnect", method = "POST", token = token)
    }

    suspend fun fetchBotStatus(token: String): BotStatus {
        val data = request("/bots", token = token).optJSONObject("data") ?: JSONObject()
        return BotStatus(
            sessionId = data.optStringOrNull("sessionId"),
            status = data.optString("status", "offline"),
            connected = data.optBoolean("connected", false),
            isReconnecting = data.optBoolean("isReconnecting", false),
            reconnectAttempt = data.optInt("reconnectAttempt", 0),
            server = data.optStringOrNull("server"),
            startedAt = data.optLongOrNull("startedAt"),
            uptimeMs = data.optLongOrNull("uptimeMs"),
            lastConnectedAt = data.optLongOrNull("lastConnectedAt"),
            lastError = data.optStringOrNull("lastError"),
            lastDisconnectReason = data.optStringOrNull("lastDisconnectReason"),
            connectionType = data.optStringOrNull("connectionType"),
            accountId = data.optStringOrNull("accountId")
        )
    }

    suspend fun fetchHealth(): HealthStatus {
        val data = request("/health").optJSONObject("data") ?: JSONObject()
        return HealthStatus(
            status = data.optString("status", "ok"),
            uptimeSec = data.optLong("uptimeSec", 0L),
            bots = data.optInt("bots", 0),
            memoryMb = data.optInt("memoryMb", 0),
            maxBots = data.optInt("maxBots", 20)
        )
    }

    suspend fun pingHealth(): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        request("/health")
        System.currentTimeMillis() - start
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    optString(key).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null
