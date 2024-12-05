@file:Suppress("SpellCheckingInspection")

import java.io.File

fun generateRandomString(length: Int): String {
    val allowedChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    return (1..length).map { allowedChars.random() }.joinToString("")
}

fun renameFilesInFolder(folderPath: String) {
    val folder = File(folderPath)

    if (folder.exists() && folder.isDirectory) {
        val files = folder.listFiles()

        files?.forEach { file ->
            val fileExtension = file.extension
            val randomFileName = generateRandomString(9) // 调整为9位字符
            val newFileName = "$randomFileName.$fileExtension"
            val newFile = File(folderPath, newFileName)

            if (file.renameTo(newFile)) {
                println("文件 ${file.name} 重命名为 $newFileName 成功！")
            } else {
                println("文件 ${file.name} 重命名失败。")
            }
        }
    } else {
        println("指定的路径不是一个有效的文件夹。")
    }
}

fun main() {
    val folderPath = chooseDirectory("选择文件夹路径")
    if (folderPath != null) {
        println("选择的文件夹路径: $folderPath")
        renameFilesInFolder(folderPath)
    } else {
        println("未选择文件夹路径，程序退出。")
    }
}
