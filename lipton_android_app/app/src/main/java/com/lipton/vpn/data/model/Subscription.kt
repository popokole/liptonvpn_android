package com.lipton.vpn.data.model

import java.util.UUID

data class UserInfo(
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0,
    val expire: Long = 0,
)

data class Subscription(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Подписка",
    val url: String,
    val isTrial: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val servers: List<Server> = emptyList(),
    val userInfo: UserInfo = UserInfo(),
)

fun UserInfo.usedBytes(): Long = upload + download

fun UserInfo.usedPercent(): Float =
    if (total <= 0) 0f else (usedBytes().toFloat() / total.toFloat()).coerceIn(0f, 1f)

fun Long.toReadableBytes(): String = when {
    this >= 1_000_000_000L -> "%.1f GB".format(this / 1_000_000_000.0)
    this >= 1_000_000L     -> "%.1f MB".format(this / 1_000_000.0)
    this >= 1_000L         -> "%.1f KB".format(this / 1_000.0)
    else                   -> "$this B"
}
