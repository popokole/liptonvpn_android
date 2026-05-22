package com.lipton.vpn.data

import com.google.gson.GsonBuilder
import com.lipton.vpn.data.model.Server

object XrayConfigGenerator {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun generate(
        server: Server,
        socksPort: Int = 10808,
        httpPort: Int = 10809,
        bypassRu: Boolean = true,
        bypassDomains: List<String> = emptyList(),
    ): String {
        val outbound = when (server.protocol) {
            "vless"  -> vlessOutbound(server)
            "vmess"  -> vmessOutbound(server)
            "trojan" -> trojanOutbound(server)
            else     -> error("Неизвестный протокол: ${server.protocol}")
        }

        val rules = mutableListOf<Map<String, Any>>()

        if (bypassDomains.isNotEmpty()) {
            rules += mapOf(
                "type" to "field",
                "domain" to bypassDomains.map { "domain:${it.removePrefix("domain:")}" },
                "outboundTag" to "direct",
            )
        }

        if (bypassRu) {
            rules += mapOf(
                "type" to "field",
                "domain" to listOf(
                    "geosite:category-ru",
                    "domain:vk.com", "domain:vkontakte.ru",
                    "domain:gosuslugi.ru", "domain:mos.ru",
                    "domain:yandex.ru", "domain:yandex.net",
                    "domain:mail.ru", "domain:ok.ru",
                    "domain:sberbank.ru", "domain:sbrf.ru",
                    "domain:tinkoff.ru", "domain:raiffeisen.ru",
                    "domain:gazprombank.ru", "domain:vtb.ru",
                    "domain:mvd.ru", "domain:kremlin.ru",
                    "domain:government.ru", "domain:rbc.ru",
                    "domain:1tv.ru", "domain:ntv.ru", "domain:rt.com",
                    "domain:2ip.ru", "domain:2ip.io",
                ),
                "outboundTag" to "direct",
            )
            rules += mapOf(
                "type" to "field",
                "ip" to listOf("geoip:ru", "geoip:private"),
                "outboundTag" to "direct",
            )
        } else {
            rules += mapOf(
                "type" to "field",
                "ip" to listOf("geoip:private"),
                "outboundTag" to "direct",
            )
        }

        rules += mapOf(
            "type" to "field",
            "network" to "tcp,udp",
            "outboundTag" to "proxy",
        )

        val config = mapOf(
            "log" to mapOf("loglevel" to "warning"),
            "inbounds" to listOf(
                mapOf(
                    "tag" to "socks",
                    "port" to socksPort,
                    "listen" to "127.0.0.1",
                    "protocol" to "socks",
                    "settings" to mapOf("auth" to "noauth", "udp" to true, "ip" to "127.0.0.1"),
                    "sniffing" to mapOf("enabled" to true, "destOverride" to listOf("http", "tls", "quic")),
                ),
                mapOf(
                    "tag" to "http",
                    "port" to httpPort,
                    "listen" to "127.0.0.1",
                    "protocol" to "http",
                    "settings" to mapOf("timeout" to 300, "allowTransparent" to false),
                    "sniffing" to mapOf("enabled" to true, "destOverride" to listOf("http", "tls", "quic")),
                ),
            ),
            "outbounds" to listOf(
                outbound,
                mapOf("tag" to "direct", "protocol" to "freedom", "settings" to emptyMap<String, Any>()),
                mapOf("tag" to "block", "protocol" to "blackhole",
                    "settings" to mapOf("response" to mapOf("type" to "http"))),
            ),
            "routing" to mapOf(
                "domainStrategy" to "IPIfNonMatch",
                "rules" to rules,
            ),
            "dns" to mapOf("servers" to listOf("8.8.8.8", "8.8.4.4", "1.1.1.1")),
        )

        return gson.toJson(config)
    }

    // ─── Outbounds ────────────────────────────────────────────────────────────

    private fun vlessOutbound(s: Server) = mapOf(
        "tag" to "proxy",
        "protocol" to "vless",
        "settings" to mapOf(
            "vnext" to listOf(mapOf(
                "address" to s.address,
                "port" to s.port,
                "users" to listOf(mapOf(
                    "id" to s.uuid,
                    "encryption" to "none",
                    "flow" to s.flow,
                )),
            )),
        ),
        "streamSettings" to streamSettings(s),
        "mux" to mapOf("enabled" to false),
    )

    private fun vmessOutbound(s: Server) = mapOf(
        "tag" to "proxy",
        "protocol" to "vmess",
        "settings" to mapOf(
            "vnext" to listOf(mapOf(
                "address" to s.address,
                "port" to s.port,
                "users" to listOf(mapOf(
                    "id" to s.uuid,
                    "alterId" to s.alterId,
                    "security" to s.cipher,
                )),
            )),
        ),
        "streamSettings" to streamSettings(s),
        "mux" to mapOf("enabled" to false),
    )

    private fun trojanOutbound(s: Server) = mapOf(
        "tag" to "proxy",
        "protocol" to "trojan",
        "settings" to mapOf(
            "servers" to listOf(mapOf(
                "address" to s.address,
                "port" to s.port,
                "password" to s.password,
            )),
        ),
        "streamSettings" to streamSettings(s),
        "mux" to mapOf("enabled" to false),
    )

    private fun streamSettings(s: Server): Map<String, Any> {
        val ss = mutableMapOf<String, Any>("network" to s.network.ifBlank { "tcp" })

        when (s.security) {
            "reality" -> {
                ss["security"] = "reality"
                ss["realitySettings"] = mapOf(
                    "fingerprint" to s.fp,
                    "serverName" to s.sni,
                    "publicKey" to s.pbk,
                    "shortId" to s.sid,
                    "spiderX" to s.path,
                )
            }
            "tls" -> {
                ss["security"] = "tls"
                ss["tlsSettings"] = mapOf(
                    "serverName" to s.sni,
                    "allowInsecure" to false,
                    "fingerprint" to s.fp,
                    "alpn" to if (s.alpn.isNotBlank()) s.alpn.split(",") else emptyList<String>(),
                )
            }
            else -> ss["security"] = "none"
        }

        when (s.network) {
            "ws" -> ss["wsSettings"] = mapOf(
                "path" to s.path,
                "headers" to if (s.host.isNotBlank()) mapOf("Host" to s.host) else emptyMap<String, String>(),
            )
            "grpc" -> ss["grpcSettings"] = mapOf("serviceName" to s.serviceName)
            "h2"   -> ss["httpSettings"] = mapOf("path" to s.path, "host" to if (s.host.isNotBlank()) listOf(s.host) else emptyList<String>())
            "httpupgrade" -> ss["httpupgradeSettings"] = mapOf("path" to s.path, "host" to s.host.ifBlank { s.sni })
            "xhttp", "splithttp" -> {
                val settingsKey = if (s.network == "xhttp") "xhttpSettings" else "splithttpSettings"
                val xhttpMap = mutableMapOf<String, Any>("path" to s.path)
                if (s.host.isNotBlank()) xhttpMap["host"] = s.host
                if (s.mode.isNotBlank()) xhttpMap["mode"] = s.mode
                ss[settingsKey] = xhttpMap
            }
            "tcp" -> if (s.headerType == "http") {
                val headers = if (s.host.isNotBlank())
                    mapOf("Host" to listOf(s.host))
                else emptyMap<String, List<String>>()
                ss["tcpSettings"] = mapOf(
                    "header" to mapOf(
                        "type" to "http",
                        "request" to mapOf("path" to listOf(s.path), "headers" to headers),
                    ),
                )
            }
        }

        return ss
    }
}
