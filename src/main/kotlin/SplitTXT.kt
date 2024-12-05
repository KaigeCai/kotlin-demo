import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.system.exitProcess

fun main() {
    println("请输入要分割的字数：")
    val chunkSize = readlnOrNull()?.toIntOrNull() ?: 3000
    val fileDialog = FileDialog(null as Frame?, "选择文本文件", FileDialog.LOAD)
    fileDialog.file = "*.txt"
    fileDialog.isVisible = true

    if (fileDialog.file == null) {
        println("操作被取消，未选择任何文件。")
        exitProcess(0)
    }

    val selectedFile = File(fileDialog.directory, fileDialog.file)
    val selectedFilePath = selectedFile.absolutePath
    println("选择的文件路径: $selectedFilePath")

    val content = selectedFile.readText()
    val outputDirectory = selectedFile.parentFile

    val chunks = splitTextIntoChunks(content, chunkSize)

    chunks.forEachIndexed { index, chunk ->
        val outputFileName = "temp${index + 1}.txt"
        val outputFile = File(outputDirectory, outputFileName)
        outputFile.writeText(chunk)
    }

    println("文本已分割并保存为${chunks.size}个文件")
    exitProcess(0)
}

fun splitTextIntoChunks(text: String, chunkSize: Int): List<String> {
    val chunks = mutableListOf<String>()
    var currentIndex = 0

    while (currentIndex < text.length) {
        val endIndex = if (currentIndex + chunkSize > text.length) {
            text.length
        } else {
            currentIndex + chunkSize
        }
        val chunk = text.substring(currentIndex, endIndex)
        chunks.add(chunk)
        currentIndex = endIndex
    }

    return chunks
}
