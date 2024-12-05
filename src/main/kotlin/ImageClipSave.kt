import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val clipboardImage = getClipboardImage()
    if (clipboardImage != null) {
        val savePath = "path/to/save/directory/image.png" // 指定保存目录和文件名
        saveImage(clipboardImage, savePath)
        println("Clipboard image saved to: $savePath")
    } else {
        println("No image found in the clipboard.")
    }
}

fun getClipboardImage(): BufferedImage? {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val transferable = clipboard.getContents(null)

    return if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
        try {
            transferable.getTransferData(DataFlavor.imageFlavor) as BufferedImage?
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    } else {
        null
    }
}

fun saveImage(image: BufferedImage, savePath: String) {
    try {
        val outputFile = File(savePath)
        val outputDir = outputFile.parentFile

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        ImageIO.write(image, "png", outputFile)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
