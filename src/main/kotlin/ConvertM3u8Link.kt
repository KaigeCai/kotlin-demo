import java.io.File

fun convertToM3U(title: String, link: String): String {
    return "#EXTINF:-1,$title,\n$link"
}

fun convertFileToM3U(filePath: String, outputFilePath: String) {
    val outputFile = File(outputFilePath)
    outputFile.bufferedWriter().use { writer ->
        File(filePath).forEachLine { line ->
            val (title, link) = line.split(',')
            val m3uEntry = convertToM3U(title.trim(), link.trim())
            writer.write(m3uEntry)
            writer.newLine()
        }
    }
}

fun main() {
    val inputFilePath = chooseFile("Choose Input File")
    if (inputFilePath != null) {
        val outputFilePath = "output.m3u" // 替换为你的输出文件路径
        convertFileToM3U(inputFilePath, outputFilePath)
        println("Conversion completed. Output saved to $outputFilePath")
    } else {
        println("File selection canceled.")
    }
}
