package com.lipton.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

private const val RELEASES_URL = "https://api.github.com/repos/popokole/liptonvpn_android/releases/latest"

data class UpdateInfo(
    val versionName:  String,
    val downloadUrl:  String,
    val releaseUrl:   String,
)

object UpdateChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "LiptonVPN-Android/$currentVersion")
            .build()
        val body = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
            resp.body?.string() ?: throw Exception("Empty body")
        }
        val json       = JSONObject(body)
        val tag        = json.getString("tag_name").trimStart('v', 'V')
        val releaseUrl = json.getString("html_url")
        val assets     = json.getJSONArray("assets")
        val apkUrl     = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk") }
            ?.getString("browser_download_url")
            ?: releaseUrl

        if (isNewer(tag, currentVersion)) UpdateInfo(tag, apkUrl, releaseUrl) else null
    }

    suspend fun downloadApk(
        downloadUrl: String,
        destFile: File,
        onProgress: (Int) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val dlClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(downloadUrl).build()
            dlClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                val total = body.contentLength()
                destFile.parentFile?.mkdirs()
                var downloaded = 0L
                destFile.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(8192)
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                            downloaded += read
                            if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                        }
                    }
                }
                true
            }
        }.getOrDefault(false)
    }

    private fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
        val r = parts(remote)
        val l = parts(local)
        val len = maxOf(r.size, l.size)
        for (i in 0 until len) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}
