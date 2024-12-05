import java.io.File

fun listFilesInDirectory(directoryPath: String) {
    val directory = File(directoryPath)

    if (directory.exists() && directory.isDirectory) {
        directory.listFiles()?.forEach { file ->
            println("'${file.name}',")
        }
    } else {
        println("Directory does not exist or is not a directory")
    }
}

fun main() {
    val title = "Select a File (will list files in the same directory)"
    chooseFile(title)?.let { filePath ->
        val file = File(filePath)
        val directoryPath = file.parent ?: return
        listFilesInDirectory(directoryPath)
    } ?: println("No file was selected.")
}
