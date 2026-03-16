package com.minebot.app

import org.json.JSONObject

fun JSONObject.optStringOrNull(key: String): String? =
    optString(key).takeIf { it.isNotBlank() && it != "null" }

fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null
