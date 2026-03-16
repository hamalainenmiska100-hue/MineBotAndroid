package com.minebot.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

class SecurePrefs(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "minebot_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getToken(): String? = prefs.getString("token", null)
    fun setToken(token: String?) = prefs.edit().putString("token", token).apply()

    fun getTutorialSeen(): Boolean = prefs.getBoolean("tutorial_seen", false)
    fun setTutorialSeen(value: Boolean) = prefs.edit().putBoolean("tutorial_seen", value).apply()

    fun getSelectedServerId(): String? = prefs.getString("selected_server_id", null)
    fun setSelectedServerId(id: String?) = prefs.edit().putString("selected_server_id", id).apply()

    fun getServers(): List<ServerRecord> {
        val raw = prefs.getString("servers_json", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        ServerRecord(
                            id = obj.getString("id"),
                            ip = obj.getString("ip"),
                            port = obj.getInt("port")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun setServers(servers: List<ServerRecord>) {
        val arr = JSONArray()
        servers.forEach { server ->
            arr.put(
                JSONObject()
                    .put("id", server.id)
                    .put("ip", server.ip)
                    .put("port", server.port)
            )
        }
        prefs.edit().putString("servers_json", arr.toString()).apply()
    }
}
