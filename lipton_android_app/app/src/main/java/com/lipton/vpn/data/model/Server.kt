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
    val ping: Long? = null,
)

fun Server.displayName(): String =
    remark.replace(Regex("[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]\\s*"), "").trim()

fun Server.flagEmoji(): String {
    val match = Regex("([\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF])").find(remark)
    return match?.value ?: ""
}
