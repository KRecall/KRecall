package io.github.octestx.krecall.plugins.impl.storage

import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Perceptual hash (pHash) calculation for images, used to compare image similarity.
 *
 * @param size Hash dimension (default: 32), determines hash granularity
 * @param smallerSize Reduced dimension for DCT (default: 8), affects sensitivity
 */
class ImagePHash(val size: Int = 32, private val smallerSize: Int = 8) {
    private var hashArray: DoubleArray = DoubleArray(size)

    init {
        for (i in 1..<size) {
            hashArray[i] = 1.0
        }
        hashArray[0] = 1 / sqrt(2.0)
    }


    private fun distance(s1: String, s2: String): Int {
        var counter = 0
        for (k in s1.indices) {
            if (s1[k] != s2[k]) {
                counter++
            }
        }
        return counter
    }

    // Returns a 'binary string' (like. 001010111011100010) which is easy to do
    // a hamming distance on.
    @Throws(Exception::class)
    private fun getHash(`is`: InputStream): String {
        var img = ImageIO.read(`is`)

        /*
        * 1. Reduce size. Like Average Hash, pHash starts with a small image.
        * However, the image is larger than 8x8; 32x32 is a good size. This is
        * really done to simplify the DCT computation and not because it is
        * needed to reduce the high frequencies.
        */
        img = resize(img, size, size)

        /*
        * 2. Reduce color. The image is reduced to a grayscale just to further
        * simplify the number of computations.
        */
        img = grayscale(img)

        val vals = Array(size) { DoubleArray(size) }

        for (x in 0..<img.width) {
            for (y in 0..<img.height) {
                vals[x][y] = getBlue(img, x, y).toDouble()
            }
        }

        /*
        * 3. Compute the DCT. The DCT separates the image into a collection of
        * frequencies and scalars. While JPEG uses an 8x8 DCT, this algorithm
        * uses a 32x32 DCT.
        */
        val dctVals = applyDCT(vals)

        /*
        * 4. Reduce the DCT. This is the magic step. While the DCT is 32x32,
        * just keep the top-left 8x8. Those represent the lowest frequencies in
        * the picture.
        */
        /*
        * 5. Compute the average value. Like the Average Hash, compute the mean
        * DCT value (using only the 8x8 DCT low-frequency values and excluding
        * the first term since the DC coefficient can be significantly
        * different from the other values and will throw off the average).
        */
        var total = 0.0

        for (x in 0..<smallerSize) {
            for (y in 0..<smallerSize) {
                total += dctVals[x][y]
            }
        }
        total -= dctVals[0][0]

        val avg = total / ((smallerSize * smallerSize) - 1).toDouble()

        /*
        * 6. Further reduce the DCT. This is the magic step. Set the 64 hash
        * bits to 0 or 1 depending on whether each of the 64 DCT values is
        * above or below the average value. The result doesn't tell us the
        * actual low frequencies; it just tells us the very-rough relative
        * scale of the frequencies to the mean. The result will not vary as
        * long as the overall structure of the image remains the same; this can
        * survive gamma and color histogram adjustments without a problem.
        */
        var hash = ""

        for (x in 0..<smallerSize) {
            for (y in 0..<smallerSize) {
                if (x != 0 && y != 0) {
                    hash += (if (dctVals[x][y] > avg) "1" else "0")
                }
            }
        }

        return hash
    }

    private fun resize(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val resizedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = resizedImage.createGraphics()
        g.drawImage(image, 0, 0, width, height, null)
        g.dispose()
        return resizedImage
    }

    private val colorConvert: ColorConvertOp = ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null)

    private fun grayscale(img: BufferedImage): BufferedImage {
        colorConvert.filter(img, img)
        return img
    }

    private fun getBlue(img: BufferedImage, x: Int, y: Int): Int {
        return (img.getRGB(x, y)) and 0xff
    }

    // DCT function stolen from
    // http://stackoverflow.com/questions/4240490/problems-with-dct-and-idct-algorithm-in-java
    private fun applyDCT(f: Array<DoubleArray>): Array<DoubleArray> {
        val N = size

        val F = Array(N) { DoubleArray(N) }
        for (u in 0..<N) {
            for (v in 0..<N) {
                var sum = 0.0
                for (i in 0..<N) {
                    for (j in 0..<N) {
                        sum += (cos(((2 * i + 1) / (2.0 * N)) * u * Math.PI) * cos(((2 * j + 1) / (2.0 * N)) * v * Math.PI) * (f[i][j]))
                    }
                }
                sum *= ((hashArray[u] * hashArray[v]) / 4.0)
                F[u][v] = sum
            }
        }
        return F
    }

    /**
     * @param srcUrl
     * @param canUrl
     * @return    值越小相识度越高，10之内可以简单判断这两张图片内容一致
     * @throws Exception
     * @throws
     */
    @Throws(Exception::class)
    fun distance(srcUrl: URL, canUrl: URL): Int {
        val imgStr = this.getHash(srcUrl.openStream())
        val canStr = this.getHash(canUrl.openStream())
        return this.distance(imgStr, canStr)
    }

    /**
     * @param src
     * @param can:
     * @return 值越小相识度越高，10之内可以简单判断这两张图片内容一致
     * @throws Exception
     */
    @Throws(Exception::class)
    fun distance(src: ByteArray, can: ByteArray): Int {
        val imageSrcFile = this.getHash(ByteArrayInputStream(src))
        val imageCanFile = this.getHash(ByteArrayInputStream(can))
        return this.distance(imageSrcFile, imageCanFile)
    }

    fun toPercent(value: Int): Float {
        require(value >= 0) { "距离值不能为负数" }
        // 计算理论最大距离（smallerSize为类参数）
        val maxDistance = smallerSize * smallerSize

        return when {
            value == 0 -> 1.0f       // 完全匹配
            value >= maxDistance -> 0.0f  // 完全不相似
            else -> 1.0f - (value.toFloat() / maxDistance)  // 线性映射
        }.coerceIn(0f, 1f)  // 确保结果在[0,1]范围内
    }
}