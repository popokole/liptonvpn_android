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
    // Эффективный лимит устройств (нет поля — лимит неизвестен).
    @SerializedName("device_limit")       val deviceLimit: Int? = null,
    // Версия ссылки подписки: её же шлём в POST /me/subscription/relink как expected_version.
    @SerializedName("link_version")       val linkVersion: Int = 0,
    @SerializedName("link_updated_at")    val linkUpdatedAt: String? = null,
    @SerializedName("link_rotating")      val linkRotating: Boolean = false,
    @SerializedName("canceled")           val canceled: Boolean = false,
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
    // Поля B3 (пока могут не приходить — тогда null).
    @SerializedName("tariff_title")   val tariffTitle: String? = null,
    @SerializedName("period_days")    val periodDays: Int? = null,
    @SerializedName("method")         val method: String? = null,
    @SerializedName("card_last4")     val cardLast4: String? = null,
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
    // ── Редизайн (поля только добавлены; у старого бэкенда их нет — значения по умолчанию) ──
    // Гостевой доступ без регистрации: объект {enabled, minutes} и те же поля плоско.
    @SerializedName("guest_trial")         val guestTrial: GuestTrialConfig? = null,
    @SerializedName("trial_guest_enabled") val trialGuestEnabled: Boolean = false,
    @SerializedName("trial_guest_minutes") val trialGuestMinutes: Int = 0,
    // Пробный период после регистрации (дни).
    @SerializedName("account_trial_days")  val accountTrialDays: Int? = null,
    @SerializedName("support_bot")         val supportBot: String? = null,
    @SerializedName("support_bot_url")     val supportBotUrl: String? = null,
    @SerializedName("min_app_versions")    val minAppVersions: Map<String, String>? = null,
) {
    /** Гостевой доступ включён в админке (нет полей — выключен). */
    val guestEnabled: Boolean get() = guestTrial?.enabled ?: trialGuestEnabled

    /** Длительность гостевого доступа в минутах (по умолчанию 15). */
    val guestMinutes: Int get() = (guestTrial?.minutes?.takeIf { it > 0 } ?: trialGuestMinutes.takeIf { it > 0 }) ?: 15
}

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

data class GuestTrialConfig(
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("minutes") val minutes: Int = 0,
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
data class AiDialog(
    @SerializedName("messages") val messages: List<AiMessage> = emptyList(),
    @SerializedName("mode")     val mode: String? = null,   // auto | manual (отвечает оператор)
    @SerializedName("id")       val id: String? = null,
)

data class AiMessage(
    @SerializedName("role")    val role: String = "",   // user | assistant
    @SerializedName("content") val content: String = "",
    @SerializedName("at")      val at: String? = null,
    // Редизайн: id реплики — для «Помогло / Не помогло» (POST /support/ai/feedback).
    @SerializedName("id")      val id: String? = null,
)

// ─── Профиль, устройства, статус серверов (редизайн A4) ─────────────────────

// GET /me. Старые поля (avatar, card_last4, …) есть всегда; новые (tg_username,
// tg_photo_url, card_*, next_charge_*) — с бэкенда редизайна, до выкладки — null.
data class MeProfile(
    @SerializedName("email")                    val email: String? = null,
    @SerializedName("telegram_linked")          val telegramLinked: Boolean = false,
    @SerializedName("avatar")                   val avatar: String? = null,       // data:image/…;base64,…
    @SerializedName("has_card")                 val hasCard: Boolean = false,
    @SerializedName("card_last4")               val cardLast4: String? = null,
    @SerializedName("auto_renew")               val autoRenew: Boolean = false,
    @SerializedName("created_at")               val createdAt: String? = null,
    @SerializedName("tg_username")              val tgUsername: String? = null,   // без «@»
    @SerializedName("tg_photo_url")             val tgPhotoUrl: String? = null,
    @SerializedName("card_brand")               val cardBrand: String? = null,
    @SerializedName("card_exp")                 val cardExp: String? = null,
    @SerializedName("card_unlink_available_at") val cardUnlinkAvailableAt: String? = null,
    @SerializedName("next_charge_at")           val nextChargeAt: String? = null,
    @SerializedName("next_charge_kopeks")       val nextChargeKopeks: Long? = null,
    @SerializedName("next_charge_period_days")  val nextChargePeriodDays: Int? = null,
    @SerializedName("next_charge_tariff_title") val nextChargeTariffTitle: String? = null,
)

// GET /me/devices
data class DevicesResponse(
    @SerializedName("devices")      val devices: List<DeviceItem>? = null,
    @SerializedName("device_limit") val deviceLimit: Int? = null,
)

data class DeviceItem(
    @SerializedName("hwid")       val hwid: String = "",
    @SerializedName("platform")   val platform: String? = null,   // iOS, Android, Windows…
    @SerializedName("os_version") val osVersion: String? = null,
    @SerializedName("model")      val model: String? = null,
    @SerializedName("app")        val app: String? = null,        // приложение по User-Agent
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,  // последняя загрузка подписки
)

// GET /status/servers (публичная, кэш 60 с)
data class ServerStatusList(
    @SerializedName("servers")    val servers: List<ServerStatus>? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("stale")      val stale: Boolean = false,
)

data class ServerStatus(
    @SerializedName("name")    val name: String = "",
    @SerializedName("country") val country: String? = null,   // ISO: DE, NL…
    @SerializedName("status")  val status: String = "unknown", // up | down | unknown
)

// GET /ipcheck — как сайты видят исходящий адрес.
data class IpInfo(
    @SerializedName("ip")           val ip: String = "",
    @SerializedName("country")      val country: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    @SerializedName("city")         val city: String? = null,
    @SerializedName("org")          val org: String? = null,
    @SerializedName("flag")         val flag: String? = null,
)

// POST /promo/validate
data class PromoResult(
    @SerializedName("valid")       val valid: Boolean = false,
    @SerializedName("reason")      val reason: String? = null,
    @SerializedName("kind")        val kind: String? = null,
    @SerializedName("percent_off") val percentOff: Int? = null,
    @SerializedName("bonus_days")  val bonusDays: Int? = null,
)

// DELETE /payments/card
data class CardUnlinkResult(
    @SerializedName("status")  val status: String = "",
    @SerializedName("warning") val warning: String? = null,
)
