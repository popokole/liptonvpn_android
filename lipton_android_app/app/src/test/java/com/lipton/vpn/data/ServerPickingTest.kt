package com.lipton.vpn.data

import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.ServerStatus
import com.lipton.vpn.data.model.ServerStatusList
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ServerPickingTest {

    private fun srv(id: String, remark: String, address: String = "1.1.1.1", ping: Long? = null, sni: String = "a") =
        Server(id = id, protocol = "vless", address = address, port = 443, remark = remark, sni = sni, ping = ping)

    @Test fun `по умолчанию Авто-баланс, а не первый`() {
        val list = listOf(srv("de", "🇩🇪 Германия", ping = 150), srv("auto", "🇩🇪 Авто-баланс", ping = 200), srv("nl", "🇳🇱 Нидерланды", ping = 120))
        assertEquals("auto", list.defaultServer()?.id)
    }

    @Test fun `без Авто-баланса самый быстрый, без пинга первый`() {
        assertEquals("nl", listOf(srv("de", "Германия", ping = 150), srv("nl", "Нидерланды", ping = 120)).defaultServer()?.id)
        assertEquals("a", listOf(srv("a", "A"), srv("b", "B")).defaultServer()?.id)
        assertNull(emptyList<Server>().defaultServer())
    }

    @Test fun `Обход глушилок не считается Авто-балансом`() {
        val list = listOf(srv("bp", "Обход Глушилок авто"), srv("de", "Германия"))
        assertEquals("bp", list.defaultServer()?.id) // нет ни Авто-баланса, ни пинга — первый
        assertEquals("de", listOf(srv("bp", "Обход авто", ping = 300), srv("de", "Германия", ping = 100)).defaultServer()?.id)
    }

    @Test fun `название для сверки без флага и подписи`() {
        assertEquals("германия", serverTitle("🇩🇪 Германия · Франкфурт"))
        assertEquals("польша №2", serverTitle("🇵🇱 Польша №2"))
        assertEquals("германия берлин", serverTitle(" 🇩🇪 Германия Берлин"))
    }

    @Test fun `офлайн-серверы прячем, выбранный и неизвестные оставляем`() {
        val list = listOf(srv("de", "🇩🇪 Германия"), srv("pl", "🇵🇱 Польша №2"), srv("nl", "🇳🇱 Нидерланды · Амстердам"), srv("bp", "Обход Глушилок"))
        val status = ServerStatusList(servers = listOf(
            ServerStatus("Германия", status = "up"), ServerStatus("Польша №2", status = "down"),
            ServerStatus("Нидерланды", status = "down"), ServerStatus("Обход Глушилок", status = "unknown"),
        ))
        assertEquals(listOf("de", "bp"), hideOffline(list, status, null).map { it.id })
        assertEquals(listOf("de", "pl", "bp"), hideOffline(list, status, "pl").map { it.id })
        assertEquals(4, hideOffline(list, null, null).size)
        assertEquals(4, hideOffline(list, status.copy(stale = true), null).size)
        val allDown = ServerStatusList(servers = list.map { ServerStatus(serverTitle(it.remark), status = "down") })
        assertEquals(4, hideOffline(list, allDown, null).size)
    }

    @Test fun `пинг — один адрес меряется один раз, лучший из замеров`() = runBlocking {
        val calls = AtomicInteger()
        val seq = mutableListOf(438L, 124L, 150L)
        val list = listOf(srv("de", "Германия"), srv("fr", "Германия №3"), srv("de2", "Германия №2"))
        val out = measureServers(list) { calls.incrementAndGet(); synchronized(seq) { seq.removeAt(0) } }
        assertEquals(mapOf("de" to 124L, "fr" to 124L, "de2" to 124L), out)
        assertEquals(3, calls.get())
    }

    @Test fun `пинг — сбои пропускаются, все неудачные дают null`() = runBlocking {
        val out = measureServers(listOf(srv("x", "X"))) { throw RuntimeException("timeout") }
        assertNull(out["x"])
    }

    @Test fun `пинг — не больше заданного числа адресов одновременно`() = runBlocking {
        val active = AtomicInteger(); var peak = 0
        val list = (0 until 8).map { srv("s$it", "S$it", address = "10.0.0.$it") }
        measureServers(list, samples = 1, concurrency = 3) {
            val now = active.incrementAndGet(); synchronized(this@ServerPickingTest) { if (now > peak) peak = now }
            delay(20); active.decrementAndGet(); 50L
        }
        assertEquals(3, peak)
    }
}
