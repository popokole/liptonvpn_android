package com.lipton.vpn

import com.lipton.vpn.data.model.IpInfo
import com.lipton.vpn.data.model.NewsItem
import com.lipton.vpn.data.model.ServerStatusList

/**
 * Живая статистика главной (обновляется раз в секунду, поэтому отдельно от UiState —
 * чтобы тики не перерисовывали остальные вкладки).
 */
data class StatsState(
    /** Начало текущей сессии (мс); 0 — не подключено. */
    val connectedAt: Long = 0L,
    /** Скорость, байт/с. */
    val downBps: Long = 0L,
    val upBps: Long = 0L,
    /** Скорость приёма за последние секунды (байт/с), для столбиков плитки «Скорость». */
    val speedHistory: List<Long> = emptyList(),
    /** Пинг до текущего сервера (TCP-рукопожатие), мс. */
    val pingMs: Long? = null,
    /** Замеры пинга за последний час (раз в 30 с). */
    val pingHistory: List<Long> = emptyList(),
    /** Трафик по дням «на этом телефоне»: день → [приём, отдача]. */
    val trafficDays: Map<String, LongArray> = emptyMap(),
    /** Как сайты видят адрес (через VPN, если подключено). */
    val ip: IpInfo? = null,
    val ipViaVpn: Boolean = false,
    val ipLoading: Boolean = false,
    /** У сети есть публичный IPv6 (без VPN он «открыт»). */
    val ipv6Available: Boolean = false,
)

/** Лента новостей и статус серверов для вкладки «Новости» и точки на таббаре. */
data class NewsState(
    val items: List<NewsItem> = emptyList(),
    val readIds: Set<String> = emptySet(),
    val status: ServerStatusList? = null,
    val statusAt: Long = 0L,
    val loading: Boolean = false,
    val loadedAt: Long = 0L,
    val error: String? = null,
)
