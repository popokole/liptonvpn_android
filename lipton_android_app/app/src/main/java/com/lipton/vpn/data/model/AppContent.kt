package com.lipton.vpn.data.model

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
//  Ответы ручек редизайна (волна 2): гостевой доступ, 15 минут в день,
//  баннеры, уведомления, база знаний. Если ручка ещё не
//  отвечает, клиент тихо скрывает соответствующий блок.
// ─────────────────────────────────────────────────────────────────────────────

// POST /guest/trial → 200
data class GuestTrialResponse(
    @SerializedName("subscription_url") val subscriptionUrl: String? = null,
    @SerializedName("expires_at")       val expiresAt: String? = null,
    @SerializedName("server_name")      val serverName: String? = null,
)

// POST /me/daily-trial → 200
data class DailyTrialResponse(
    @SerializedName("expires_at")       val expiresAt: String? = null,
    @SerializedName("subscription_url") val subscriptionUrl: String? = null,
)

// GET /app/banners?platform=android&version=X.Y.Z
data class BannersResponse(@SerializedName("banners") val banners: List<AppBanner>? = null)

/**
 * Баннер или экран из админки. kind: banner (в слоте над главной) | screen
 * (полноэкранный диалог) | update (обновление; dismissible=false — обязательное).
 * style: info | promo | warning.
 */
data class AppBanner(
    @SerializedName("id")          val id: String = "",
    @SerializedName("kind")        val kind: String = "banner",
    @SerializedName("style")       val style: String = "info",
    @SerializedName("title")       val title: String = "",
    @SerializedName("text")        val text: String? = null,
    @SerializedName("cta_text")    val ctaText: String? = null,
    @SerializedName("cta_url")     val ctaUrl: String? = null,
    @SerializedName("dismissible") val dismissible: Boolean = true,
    @SerializedName("priority")    val priority: Int = 0,
    @SerializedName("starts_at")   val startsAt: String? = null,
    @SerializedName("ends_at")     val endsAt: String? = null,
)

// GET / PUT /me/notifications
data class NotificationPrefs(
    @SerializedName("payment_reminders") val paymentReminders: Boolean = true,
    @SerializedName("news")              val news: Boolean = true,
    @SerializedName("telegram_messages") val telegramMessages: Boolean = true,
)

// GET /faq
data class FaqList(@SerializedName("items") val items: List<FaqEntry>? = null)

data class FaqEntry(
    @SerializedName("id")       val id: String = "",
    @SerializedName("question") val question: String = "",
    @SerializedName("answer")   val answer: String = "",
)

// GET /content/articles?category= → [{ slug, title, category, minutes, excerpt }]
data class ArticleSummary(
    @SerializedName("slug")     val slug: String = "",
    @SerializedName("title")    val title: String = "",
    @SerializedName("category") val category: String? = null,
    @SerializedName("minutes")  val minutes: Int? = null,
    @SerializedName("excerpt")  val excerpt: String? = null,
)

// GET /content/articles/:slug → { …, html }
data class ArticleDetail(
    @SerializedName("slug")     val slug: String = "",
    @SerializedName("title")    val title: String = "",
    @SerializedName("category") val category: String? = null,
    @SerializedName("minutes")  val minutes: Int? = null,
    @SerializedName("excerpt")  val excerpt: String? = null,
    @SerializedName("html")     val html: String? = null,
)
