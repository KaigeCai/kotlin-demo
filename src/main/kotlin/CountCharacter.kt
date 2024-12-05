import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() {
    val selectedFilePath = chooseFile()
    if (selectedFilePath != null) {
        val text = readFile(selectedFilePath)

        println("请选择一个选项：")
        println("1. 统计中文字符数量（排除标点和空格）")
        println("2. 统计所有字符数量（包括标点和空格）")
        println("3. 统计指定单词出现的次数")
        val option = readlnOrNull()?.toInt()

        when (option) {
            1 -> {
                val chineseCharacterCount = countChineseCharacters(text)
                println("总中文字符数量（不包括标点和空格）：$chineseCharacterCount")
            }

            2 -> {
                val characterCount = text.length
                println("总字符数量（包括标点和空格）：$characterCount")
            }

            3 -> {
                println("请输入要统计出现次数的单词：")
                val word = readlnOrNull()
                if (word != null) {
                    val wordCount = countWordOccurrences(text, word)
                    println("${word}出现的次数：$wordCount")
                }
            }

            else -> println("选择无效。")
        }
    }
}

fun chooseFile(): String? {
    val fileDialog = FileDialog(null as Frame?, "选择文本文件", FileDialog.LOAD)
    fileDialog.isVisible = true

    val selectedFile = File(fileDialog.directory, fileDialog.file)
    return when {
        selectedFile.exists() -> selectedFile.absolutePath
        else -> null
    }
}

fun readFile(filePath: String): String {
    val file = File(filePath)
    return file.readText()
}

fun countChineseCharacters(text: String): Int {
    val regex = "[\\u4E00-\\u9FA5]+"
    val chineseCharacters = text.replace(Regex("[\\pP\\pS\\pZ]"), "")  // 移除标点符号
        .replace(" ", "")  // 移除空格
        .replace("\n", "")  // 移除换行

    val matches = Regex(regex).findAll(chineseCharacters)
    return matches.sumOf { it.value.length }
}

fun countWordOccurrences(text: String, word: String): Int {
    val regex = Regex(word)
    val matches = regex.findAll(text)
    return matches.count()
}