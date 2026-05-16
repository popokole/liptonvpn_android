package com.lipton.vpn.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {

    fun mapNetworkError(e: Exception): String = when (e) {
        is SocketTimeoutException -> "Сервер не отвечает. Проверьте подключение к интернету."
        is UnknownHostException   -> "Не удалось определить адрес сервера. Проверьте DNS."
        is IOException            -> "Ошибка соединения. Проверьте интернет."
        else                      -> e.message?.let { mapRawMessage(it) } ?: "Неизвестная ошибка"
    }

    fun mapSubscriptionError(e: Exception): String = when {
        e.message?.contains("404") == true -> "Подписка не найдена. Проверьте ссылку."
        e.message?.contains("403") == true -> "Доступ запрещён. Подписка недействительна."
        e.message?.contains("429") == true -> "Слишком много запросов. Подождите немного."
        e.message?.contains("500") == true || e.message?.contains("502") == true -> "Ошибка сервера. Попробуйте позже."
        e.message?.contains("не найдено серверов") == true -> "Подписка пустая. Обратитесь в поддержку."
        e.message?.contains("уже добавлена") == true -> e.message!!
        e.message?.contains("уже есть") == true -> e.message!!
        else -> mapNetworkError(e)
    }

    fun mapRawMessage(raw: String): String {
        val lower = raw.lowercase()
        return when {
            "i/o timeout" in lower || "timed out" in lower -> "Превышено время ожидания"
            "no route to host" in lower || "network unreachable" in lower || "network is unreachable" in lower -> "Нет подключения к интернету"
            "connection refused" in lower -> "Сервер отклонил соединение"
            "no such host" in lower || "unknown host" in lower -> "Адрес сервера не найден"
            "ssl" in lower || "tls" in lower || "certificate" in lower -> "Ошибка шифрования (SSL)"
            "permission" in lower -> "Нет разрешения"
            "context deadline exceeded" in lower -> "Превышено время ожидания"
            else -> raw.take(120)
        }
    }
}
