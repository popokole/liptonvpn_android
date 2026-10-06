package com.lipton.vpn.data.model

import com.google.gson.annotations.SerializedName

// ─── Ответы бэкенда liptonone.online ────────────────────────────────────────
// Имена полей повторяют JSON API Go-бэка (см. internal/*). Gson-парсинг.

// POST /auth/* → пара токенов
data class TokenPair(
    @SerializedName("access_token")  val accessToken: String?  = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in")    val expiresIn: Long       = 900,
)

// POST /auth/telegram/init
data class TgInit(
    @SerializedName("link")       val link: String?      = null,
    @SerializedName("link_token") val linkToken: String? = null,
    @SerializedName("ttl")        val ttl: Long          = 0,
)

// GET /me/subscription
data class MeSubscription(
    @SerializedName("status")             val status: String  = "none",
    @SerializedName("tariff_code")        val tariffCode: String? = null,
    @SerializedName("current_period_end") val currentPeriodEnd: String? = null,
    @SerializedName("subscription_url")   val subscriptionUrl: String?  = null,
    @SerializedName("devices_used")       val devicesUsed: Int? = null,
    // Действующий «временный тариф» (например, «Обход глушилок» поверх «Базового»), иначе null.
    @SerializedName("overlay")            val overlay: SubOverlay? = null,
)

data class SubOverlay(
    @SerializedName("tariff_title")        val tariffTitle: String? = null,
    @SerializedName("until")               val until: String? = null,
    @SerializedName("revert_tariff_title") val revertTariffTitle: String? = null,
)

// ─── Смена тарифа (/me/subscription/change*) ────────────────────────────────

// GET /me/subscription/change/options
data class ChangeOptions(
    @SerializedName("available")        val available: Boolean = false,
    @SerializedName("reason")           val reason: String? = null,
    @SerializedName("discount_percent") val discountPercent: Int = 0,
    @SerializedName("current")          val current: ChangeCurrent? = null,
    @SerializedName("options")          val options: List<ChangeOption>? = null,
)

data class ChangeCurrent(
    @SerializedName("tariff_id")    val tariffId: String = "",
    @SerializedName("tariff_code")  val tariffCode: String = "",
    @SerializedName("tariff_title") val tariffTitle: String = "",
    @SerializedName("period_days")  val periodDays: Int = 0,
    @SerializedName("period_end")   val periodEnd: String? = null,
)

// Вариант смены. mode = "change" (зачёт остатка, скидка на доплату, излишек днями)
// или "overlay" (временный тариф дороже на срок короче: полная цена, потом
// подписка возвращается к revert_tariff_title).
data class ChangeOption(
    @SerializedName("mode")                val mode: String? = null,
    @SerializedName("tariff_id")           val tariffId: String = "",
    @SerializedName("tariff_code")         val tariffCode: String = "",
    @SerializedName("tariff_title")        val tariffTitle: String = "",
    @SerializedName("period_days")         val periodDays: Int = 0,
    @SerializedName("price_kopeks")        val priceKopeks: Long = 0,
    @SerializedName("credit_kopeks")       val creditKopeks: Long = 0,
    @SerializedName("surcharge_kopeks")    val surchargeKopeks: Long = 0,
    @SerializedName("discount_kopeks")     val discountKopeks: Long = 0,
    @SerializedName("extra_days")          val extraDays: Int = 0,
    @SerializedName("new_period_end")      val newPeriodEnd: String? = null,
    @SerializedName("will_charge_card")    val willChargeCard: Boolean = false,
    @SerializedName("card_last4")          val cardLast4: String? = null,
    @SerializedName("overlay_until")       val overlayUntil: String? = null,
    @SerializedName("revert_tariff_title") val revertTariffTitle: String? = null,
) {
    val isOverlay: Boolean get() = mode == "overlay"
}

// POST /me/subscription/change
data class ChangeResult(
    @SerializedName("status")         val status: String = "",   // changed | charged | payment_required | pending | failed
    @SerializedName("payment_url")    val paymentUrl: String? = null,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("amount_kopeks")  val amountKopeks: Long = 0,
    @SerializedName("new_period_end") val newPeriodEnd: String? = null,
)

// GET /me/transactions
data class TxList(@SerializedName("transactions") val transactions: List<TxItem> = emptyList())

data class TxItem(
    @SerializedName("id")             val id: String = "",
    @SerializedName("kind")           val kind: String = "",
    @SerializedName("amount_kopeks")  val amountKopeks: Long = 0,
    @SerializedName("status")         val status: String = "",
    @SerializedName("failure_reason") val failureReason: String? = null,
    @SerializedName("created_at")     val createdAt: String? = null,
)

// POST /payments/checkout
data class CheckoutResult(
    @SerializedName("transaction_id")   val transactionId: String = "",
    @SerializedName("status")           val status: String = "",
    @SerializedName("confirmation_url") val confirmationUrl: String? = null,
    @SerializedName("amount_kopeks")    val amountKopeks: Long = 0,
)

// GET /payments/status/:id
data class PaymentStatus(
    @SerializedName("status")         val status: String = "",
    @SerializedName("failure_reason") val failureReason: String? = null,
)

// GET /config
data class AppConfig(
    @SerializedName("tariffs")      val tariffs: List<Tariff> = emptyList(),
    @SerializedName("trial_days")   val trialDays: Long = 0,
    @SerializedName("free_sub_url") val freeSubUrl: String? = null,
)

data class Tariff(
    @SerializedName("code")         val code: String = "",
    @SerializedName("title")        val title: String = "",
    @SerializedName("period_days")  val periodDays: Int = 0,
    @SerializedName("price_kopeks") val priceKopeks: Long = 0,
    @SerializedName("periods")      val periods: List<TariffPeriod> = emptyList(),
)

data class TariffPeriod(
    @SerializedName("id")           val id: String = "",
    @SerializedName("days")         val days: Int = 0,
    @SerializedName("price_kopeks") val priceKopeks: Long = 0,
)

// GET /news
data class NewsList(@SerializedName("items") val items: List<NewsItem> = emptyList())

data class NewsItem(
    @SerializedName("id")           val id: String = "",
    @SerializedName("title")        val title: String = "",
    @SerializedName("body")         val body: String = "",
    @SerializedName("source_name")  val sourceName: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
)

// POST /support/ai
data class AiReply(
    @SerializedName("reply")    val reply: String = "",
    @SerializedName("escalate") val escalate: Boolean = false,
    @SerializedName("mode")     val mode: String = "auto",
)

// GET /support/ai/dialog
data class AiDialog(@SerializedName("messages") val messages: List<AiMessage> = emptyList())

data class AiMessage(
    @SerializedName("role")    val role: String = "",   // user | assistant
    @SerializedName("content") val content: String = "",
    @SerializedName("at")      val at: String? = null,
)
