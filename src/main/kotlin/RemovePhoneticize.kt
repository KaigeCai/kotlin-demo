import java.io.*

fun main() {
    val inputFilePath = chooseFile("选择输入文本文件")
    if (inputFilePath == null) {
        println("未选择输入文本文件")
        return
    }

    val outputFilePath = chooseFile("选择保存处理后文本的位置")
    if (outputFilePath == null) {
        println("未选择保存位置")
        return
    }

    val inputText = readTextFromFile(inputFilePath)
    val processedText = removePinyinTones(inputText)
    saveTextToFile(processedText, outputFilePath)

    println("拼音音标已移除并保存到 $outputFilePath")
}

fun readTextFromFile(filePath: String): String {
    val file = File(filePath)
    val reader = BufferedReader(FileReader(file))
    val text = reader.readText()
    reader.close()
    return text
}

fun removePinyinTones(text: String): String {
    val pinyinWithTones = arrayOf(
        "ā",
        "á",
        "ǎ",
        "à",
        "ē",
        "é",
        "ě",
        "è",
        "ī",
        "í",
        "ǐ",
        "ì",
        "ō",
        "ó",
        "ǒ",
        "ò",
        "ū",
        "ú",
        "ǔ",
        "ù",
        "ü",
        "ǖ",
        "ǘ",
        "ǚ",
        "ǜ"
    )
    val pinyinWithoutTones = arrayOf(
        "a",
        "a",
        "a",
        "a",
        "e",
        "e",
        "e",
        "e",
        "i",
        "i",
        "i",
        "i",
        "o",
        "o",
        "o",
        "o",
        "u",
        "u",
        "u",
        "u",
        "u",
        "v",
        "v",
        "v",
        "v"
    )

    var resultText = text
    for (i in pinyinWithTones.indices) {
        resultText = resultText.replace(pinyinWithTones[i], pinyinWithoutTones[i])
    }

    return resultText
}

fun saveTextToFile(text: String, filePath: String) {
    val file = File(filePath)
    val writer = BufferedWriter(FileWriter(file))
    writer.write(text)
    writer.close()
}
