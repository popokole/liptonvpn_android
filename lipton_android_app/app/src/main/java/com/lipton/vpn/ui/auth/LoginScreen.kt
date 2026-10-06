package com.lipton.vpn.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.data.ApiClient
import com.lipton.vpn.ui.theme.Green
import com.lipton.vpn.ui.theme.LocalLiptonColors
import com.lipton.vpn.ui.theme.Red
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BOT_URL  = "https://t.me/liptonvpn_bot"
private const val SITE_URL = "https://liptonone.online/app/connect"

private enum class Method(val label: String) { CODE("Код с сайта"), EMAIL("Почта"), TG("Telegram") }

@Composable
fun LoginScreen(vm: MainViewModel) {
    val lc = LocalLiptonColors.current
    var method by remember { mutableStateOf(Method.CODE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lc.bgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 56.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Green, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("L", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.Black) }

        Spacer(Modifier.height(18.dp))
        Text("Вход в Lipton VPN", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Войдите в аккаунт — подписка подтянется автоматически.",
            fontSize = 13.sp, color = lc.textSecondary, textAlign = TextAlign.Center,
            lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.height(24.dp))

        // ─── Вкладки ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(lc.cardBg, RoundedCornerShape(12.dp))
                .padding(4.dp),
        ) {
            Method.values().forEach { m ->
                val on = method == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (on) Green else Color.Transparent, RoundedCornerShape(9.dp))
                        .clickable { method = m }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        m.label, fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) Color.Black else lc.textSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        when (method) {
            Method.CODE  -> CodeForm(vm, "site")
            Method.EMAIL -> EmailForm(vm)
            Method.TG    -> TgForm(vm)
        }

        Spacer(Modifier.height(20.dp))
        val ctx = LocalContext.current
        Text(
            "Нет аккаунта? Откройте бота Lipton VPN",
            fontSize = 12.sp, color = lc.textTertiary,
            modifier = Modifier.clickable {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BOT_URL)))
            },
        )
    }
}

// ─── Общие элементы ──────────────────────────────────────────────────────────

@Composable
private fun Hint(text: String) {
    val lc = LocalLiptonColors.current
    Text(text, fontSize = 12.5.sp, color = lc.textSecondary, lineHeight = 17.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
}

@Composable
private fun ErrLine(text: String?) {
    if (text.isNullOrBlank()) return
    Text(text, fontSize = 12.5.sp, color = Red,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
}

@Composable
private fun CodeField(value: String, onChange: (String) -> Unit, len: Int, placeholder: String) {
    val lc = LocalLiptonColors.current
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(len)) },
        placeholder = { Text(placeholder, color = lc.textTertiary, fontSize = 22.sp) },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 22.sp, color = lc.textPrimary, textAlign = TextAlign.Center,
            letterSpacing = 8.sp, fontWeight = FontWeight.Bold,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Green, unfocusedBorderColor = lc.cardBorder,
            focusedContainerColor = lc.cardBg, unfocusedContainerColor = lc.cardBg,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(50.dp),
    ) { Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

// ─── Код с сайта / из бота (4 цифры → device exchange) ───────────────────────

@Composable
private fun CodeForm(vm: MainViewModel, variant: String) {
    val lc = LocalLiptonColors.current
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Hint("Откройте сайт → Кабинет → Подключение → вкладка Windows → «Получить код» и введите 4 цифры.")
        CodeField(code, { code = it; err = null }, 4, "0000")
        ErrLine(err)
        PrimaryButton(if (busy) "Входим…" else "Войти", enabled = !busy && code.length == 4) {
            scope.launch {
                busy = true; err = null
                try { vm.authDeviceExchange(code); vm.completeLogin() }
                catch (e: ApiClient.ApiException) { err = e.message ?: "Неверный или истёкший код" }
                catch (e: Exception) { err = e.message }
                finally { busy = false }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Открыть сайт, чтобы получить код", fontSize = 12.sp, color = lc.textTertiary,
            modifier = Modifier.fillMaxWidth().clickable {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SITE_URL)))
            }, textAlign = TextAlign.Center)
    }
}

// ─── Почта (OTP, 6 цифр) ─────────────────────────────────────────────────────

@Composable
private fun EmailForm(vm: MainViewModel) {
    val lc = LocalLiptonColors.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf("email") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        if (step == "email") {
            Hint("Пришлём код на почту, привязанную к аккаунту.")
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; err = null },
                placeholder = { Text("you@example.com", color = lc.textTertiary) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = lc.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green, unfocusedBorderColor = lc.cardBorder,
                    focusedContainerColor = lc.cardBg, unfocusedContainerColor = lc.cardBg,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrLine(err)
            PrimaryButton(if (busy) "Отправляем…" else "Получить код", enabled = !busy && email.contains("@")) {
                scope.launch {
                    busy = true; err = null
                    val e = email.trim().lowercase()
                    try { vm.authEmailRequest(e); email = e; step = "code" }
                    catch (ex: ApiClient.ApiException) { err = ex.message ?: "Не удалось отправить код" }
                    catch (ex: Exception) { err = ex.message }
                    finally { busy = false }
                }
            }
        } else {
            Hint("Код отправлен на $email")
            CodeField(code, { code = it; err = null }, 6, "000000")
            ErrLine(err)
            PrimaryButton(if (busy) "Входим…" else "Войти", enabled = !busy && code.length == 6) {
                scope.launch {
                    busy = true; err = null
                    try { vm.authEmailVerify(email, code); vm.completeLogin() }
                    catch (ex: ApiClient.ApiException) { err = ex.message ?: "Неверный код" }
                    catch (ex: Exception) { err = ex.message }
                    finally { busy = false }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Изменить почту", fontSize = 12.sp, color = lc.textTertiary,
                modifier = Modifier.fillMaxWidth().clickable { step = "email"; code = ""; err = null },
                textAlign = TextAlign.Center)
        }
    }
}

// ─── Telegram: открыть бота → авто-вход по поллингу ──────────────────────────

@Composable
private fun TgForm(vm: MainViewModel) {
    val lc = LocalLiptonColors.current
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var linkToken by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var waiting by remember { mutableStateOf(false) }

    // Поллинг: пока waiting=true и есть linkToken — опрашиваем бэкенд.
    LaunchedEffect(waiting, linkToken) {
        val tok = linkToken
        if (!waiting || tok == null) return@LaunchedEffect
        val deadline = System.currentTimeMillis() + 5 * 60_000
        while (waiting && System.currentTimeMillis() < deadline) {
            try {
                if (vm.authTgPoll(tok)) { vm.completeLogin(); return@LaunchedEffect }
            } catch (e: ApiClient.ApiException) {
                err = e.message ?: "Сессия входа истекла"; waiting = false; return@LaunchedEffect
            } catch (_: Exception) { /* сеть — повторим */ }
            delay(2500)
        }
        waiting = false
    }

    Column(Modifier.fillMaxWidth()) {
        Hint("Нажмите кнопку — откроется бот Lipton VPN. Подтвердите вход, и приложение войдёт автоматически.")
        Button(
            onClick = {
                scope.launch {
                    busy = true; err = null
                    try {
                        val init = vm.authTgInit()
                        linkToken = init.linkToken
                        val link = init.link ?: BOT_URL
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        waiting = true
                    } catch (e: ApiClient.ApiException) { err = e.message ?: "Не удалось начать вход" }
                    catch (e: Exception) { err = e.message }
                    finally { busy = false }
                }
            },
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text(if (busy) "Открываем…" else "Открыть бота в Telegram", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }

        if (waiting) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Green, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Ждём подтверждения в Telegram…", fontSize = 12.5.sp, color = lc.textSecondary)
            }
        }
        ErrLine(err)
    }
}
