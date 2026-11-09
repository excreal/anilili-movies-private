package com.anilili.data.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.intOrNull

private val jwtJson = Json { ignoreUnknownKeys = true }


internal fun isJwtExpired(token: String, nowEpochSeconds: Long = System.currentTimeMillis() / 1_000): Boolean {
    val expiration = jwtLongClaim(token, "exp") ?: return false
    return expiration <= nowEpochSeconds + TOKEN_EXPIRY_SKEW_SECONDS
}


private fun jwtLongClaim(token: String, name: String): Long? = runCatching {
    val payload = token.split('.').getOrNull(1) ?: return@runCatching null
    val decoded = com.anilili.util.Base64Compat.decode(payload)
    val value = jwtJson.parseToJsonElement(decoded.toString(Charsets.UTF_8)).jsonObject[name]?.jsonPrimitive
    value?.longOrNull ?: value?.intOrNull?.toLong()
}.getOrNull()


private const val TOKEN_EXPIRY_SKEW_SECONDS = 60L
