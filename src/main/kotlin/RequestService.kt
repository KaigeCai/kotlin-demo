@file:Suppress("HttpUrlsUsage")

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

fun main() {
    @Throws(IOException::class)
    fun get(url: String): String {
        val client = OkHttpClient()
        val request: Request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            return "${response.code}\n${response.body}"
        }
    }

    val url = "http://ott.js.chinamobile.com/PLTV/3/224/3221227659/index.m3u8"
    println(get(url))
}