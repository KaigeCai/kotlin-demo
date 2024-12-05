import java.io.*

fun main() {
    val inputFile = chooseFile("选择输入文本文件")
    if (inputFile == null) {
        println("未选择输入文本文件")
        return
    }

    val outputPath = chooseDirectory("选择输出文件夹")
    if (outputPath == null) {
        println("未选择输出文件夹")
        return
    }

    val outputFilePath = File(outputPath, "output.txt").absolutePath
    mergeLines(inputFile, outputFilePath)

    println("文本合并完成，并保存在 $outputFilePath")
}

fun mergeLines(inputFilePath: String, outputFilePath: String) {
    val inputFile = File(inputFilePath)
    val outputFile = File(outputFilePath)

    val reader = BufferedReader(FileReader(inputFile))
    val writer = BufferedWriter(FileWriter(outputFile))

    var previousLineWasEmpty = false
    var line: String?

    while (reader.readLine().also { line = it } != null) {
        if (line!!.isNotBlank()) {
            writer.write(line)
            writer.write(" ")
            previousLineWasEmpty = false
        } else {
            // 遇到空行自动换行
            if (!previousLineWasEmpty) {
                writer.newLine()
                previousLineWasEmpty = true
            }
        }
    }

    reader.close()
    writer.close()
}
