package com.minebot.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object ApiClient {

    private const val BASE = "https://afkbotb.fly.dev"

    suspend fun getHealth(): String = withContext(Dispatchers.IO) {
        URL("$BASE/health").readText()
    }

}
