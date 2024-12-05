import java.awt.FileDialog
import java.awt.Frame
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val sourceIconPath: String = chooseSourceIcon() ?: return
    val targetIconPaths: List<String> = chooseTargetIcons() ?: return

    for (targetIconPath in targetIconPaths) {
        val sizes: List<String> = readImageSizesFromFile(targetIconPath)

        for (size: String in sizes) {
            val sizeValues = size.split("x").map { it.toInt() }
            val scaledImage = scaleImage(sourceIconPath, sizeValues[0], sizeValues[1])
            val targetImageFile = File(targetIconPath)
            overwriteImageWithScaled(targetImageFile, scaledImage)

            println("Generated icon for size $size and overwritten $targetIconPath")
        }
    }

    println("Icons generated and saved.")
}

fun chooseSourceIcon(): String? {
    val fileDialog = FileDialog(null as Frame?, "Choose Source Icon", FileDialog.LOAD)
    fileDialog.isVisible = true

    return if (fileDialog.file != null) File(fileDialog.directory, fileDialog.file).absolutePath else null
}

fun chooseTargetIcons(): List<String>? {
    val fileDialog = FileDialog(null as Frame?, "Choose Target Icons", FileDialog.LOAD)
    fileDialog.isMultipleMode = true
    fileDialog.isVisible = true

    return if (fileDialog.files.isNotEmpty()) {
        fileDialog.files.map { it.absolutePath }
    } else {
        null
    }
}

fun readImageSizesFromFile(imagePath: String): List<String> {
    val sizes = mutableListOf<String>()
    val image: BufferedImage = ImageIO.read(File(imagePath))
    sizes.add("${image.width}x${image.height}")
    return sizes
}

fun scaleImage(imagePath: String, width: Int, height: Int): BufferedImage {
    val sourceImage: BufferedImage = ImageIO.read(File(imagePath))
    val scaledImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2d: Graphics2D = scaledImage.createGraphics()
    g2d.drawImage(sourceImage, 0, 0, width, height, null)
    g2d.dispose()
    return scaledImage
}

fun overwriteImageWithScaled(imageFile: File, scaledImage: BufferedImage) {
    ImageIO.write(scaledImage, "png", imageFile)
}
