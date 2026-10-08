package com.lipton.vpn.data.model

import java.util.UUID

data class Server(
    val id: String = UUID.randomUUID().toString(),
    val protocol: String,        // vless | vmess | trojan
    val address: String,
    val port: Int,
    val uuid: String = "",       // vless / vmess
    val password: String = "",   // trojan
    val remark: String,
    val network: String = "tcp",
    val security: String = "none",
    val flow: String = "",
    val sni: String = "",
    val pbk: String = "",        // VLESS Reality public key
    val sid: String = "",        // VLESS Reality short id
    val fp: String = "chrome",
    val path: String = "/",
    val host: String = "",
    val alpn: String = "",
    val serviceName: String = "",
    val alterId: Int = 0,        // VMess
    val cipher: String = "auto", // VMess
    val mode: String? = null,       // xhttp: auto | stream-up | packet-up
    val headerType: String? = null, // tcp: http (obfuscation)
    val ping: Long? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

// Флаг страны — пара символов Regional Indicator (U+1F1E6…U+1F1FF). Регулярки
// сопоставляют по кодовым точкам, поэтому диапазон задан кодовыми точками, а не
// половинками суррогатной пары (с ними флаг не находился и не вырезался из названия).
private val FLAG_REGEX = Regex("[\\x{1F1E6}-\\x{1F1FF}]{2}")
private val FLAG_PREFIX_REGEX = Regex("[\\x{1F1E6}-\\x{1F1FF}]{2}\\s*")

fun Server.displayName(): String =
    remark.replace(FLAG_PREFIX_REGEX, "").trim()

fun Server.flagEmoji(): String = FLAG_REGEX.find(remark)?.value ?: ""
