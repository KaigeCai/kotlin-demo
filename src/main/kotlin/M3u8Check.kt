import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.regex.Pattern

fun main() {
    val filePath = chooseFile("选择文件") ?: return // 文件路径

    val file = File(filePath)
    if (!file.exists()) {
        println("文件不存在: $filePath")
        return
    }

    val client = OkHttpClient()
    val urlPattern = Pattern.compile("https?://[\\w.-]+(:\\d+)?(/\\S*)?")
    val lines = file.readLines()

    val checkedLines = lines.map { line ->
        val trimmedLine = line.trim()
        when {
            trimmedLine.isEmpty() || trimmedLine.startsWith("#") -> {
                line // 空行或已注释行，保持原样
            }

            else -> {
                val matcher = urlPattern.matcher(trimmedLine)
                when {
                    matcher.find() -> {
                        val matchedLink = matcher.group()
                        if (isUrlValid(matchedLink) && isM3u8LinkValid(client, matchedLink)) line // 有效链接
                        else "# : $line" // 添加注释
                    }

                    else -> line
                }
            }
        }
    }

    file.writeText(checkedLines.joinToString(System.lineSeparator()))
    println("检测完成，结果已保存到: $filePath")
}

fun isM3u8LinkValid(client: OkHttpClient, link: String): Boolean {
    return try {
        val request = Request.Builder().url(link).build()
        client.newCall(request).execute().use { response ->
            response.code == 200
        }
    } catch (e: IOException) {
        println("检测链接失败: $link, 错误: ${e.message}")
        false
    }
}

fun isUrlValid(link: String): Boolean {
    return try {
        val url = URI(link).toURL()
        val port = url.port
        port == -1 || (port in 0..65535) // 验证端口号
    } catch (e: Exception) {
        println("无效的 URL: $link, 错误: ${e.message}")
        false
    }
}