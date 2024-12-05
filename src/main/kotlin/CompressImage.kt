import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.plugins.jpeg.JPEGImageWriteParam

fun main() {
    val inputDirectory = chooseDirectory("选择输入图片文件夹")
    if (inputDirectory.isNullOrBlank()) {
        println("未选择输入图片文件夹")
        return
    }

    val compressionQuality = 0.8f

    val inputDir = File(inputDirectory)
    if (!inputDir.exists() || !inputDir.isDirectory) {
        println("输入图片文件夹不存在或不是一个文件夹")
        return
    }

    val imageFiles = inputDir.listFiles { file -> file.isFile && file.extension in listOf("jpg", "jpeg", "png") }
    if (imageFiles.isNullOrEmpty()) {
        println("没有找到图片文件")
        return
    }

    for (imageFile in imageFiles) {
        val outputFilePath = "${inputDirectory}/${imageFile.name}"
        compressImage(imageFile.absolutePath, outputFilePath, compressionQuality)
        println("压缩完成：${outputFilePath}")
    }
}

fun compressImage(inputPath: String, outputPath: String, quality: Float) {
    val inputFile = File(inputPath)
    val image = ImageIO.read(inputFile)

    val outputFormat = inputFile.extension
    val outputFile = File(outputPath)

    val imageType =
        if (outputFormat.equals("jpg", ignoreCase = true) || outputFormat.equals("jpeg", ignoreCase = true)) {
            BufferedImage.TYPE_INT_RGB
        } else {
            BufferedImage.TYPE_INT_ARGB
        }

    val compressedImage = BufferedImage(image.width, image.height, imageType)
    val g2d = compressedImage.createGraphics()
    g2d.drawImage(image, 0, 0, null)
    g2d.dispose()

    val iter: ImageWriter = ImageIO.getImageWritersByFormatName(outputFormat).next()
    val param: ImageWriteParam = JPEGImageWriteParam(null)
    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
    param.compressionQuality = quality

    val outputStream = outputFile.outputStream()
    val imageOutputStream = ImageIO.createImageOutputStream(outputStream)
    iter.output = imageOutputStream
    iter.write(null, javax.imageio.IIOImage(compressedImage, null, null), param)
    imageOutputStream.close()
    outputStream.close()
}
