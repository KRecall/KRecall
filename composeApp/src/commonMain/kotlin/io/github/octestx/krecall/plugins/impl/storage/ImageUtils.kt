package io.github.octestx.krecall.plugins.impl.storage

import nu.pattern.OpenCV
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

//object ImageComparator {
//
//    init {
//        OpenCV.loadLocally()
//    }
//
//    /**
//     * 计算两图的 SSIM 相似度
//     * @param scale 缩放比例 (0-1)，用于优化性能
//     * @return Result<Double> SSIM 值范围 [0,1]，1 表示完全相同
//     */
//    fun calculateSSIM(
//        img1: Mat,
//        img2: Mat,
//    ): Result<Double> = runCatching {
//        // 尺寸校验
//        if (!sameSizeAndType(img1, img2)) return Result.success(0.0)
//
//        // 转换为灰度图计算 SSIM
//        val gray1 = convertToGray(img1)
//        val gray2 = convertToGray(img2)
//
//        computeSSIM(gray1, gray2)
//    }
//
//    fun loadAndPrepareImages(
//        file1: File,
//        file2: File,
//        scale: Float = 0.2f
//    ): Pair<Mat, Mat> {
//        require(scale in 0f..1f) { "缩放比例必须在 0~1 之间" }
//        val mat1 = loadImage(file1).let { if (scale != 1f) resizeMat(it, scale) else it }
//        val mat2 = loadImage(file2).let { if (scale != 1f) resizeMat(it, scale) else it }
//        return mat1 to mat2
//    }
//
//    /**
//     * 从字节数组加载并预处理图片
//     * @param data1 第一张图片的字节数组
//     * @param data2 第二张图片的字节数组
//     * @param scale 缩放比例 (0-1)
//     * @throws IllegalArgumentException 图片数据无效或解码失败
//     */
//    fun loadAndPrepareImagesFromBytes(
//        data1: ByteArray,
//        data2: ByteArray,
//        scale: Float = 0.2f
//    ): Pair<Mat, Mat> {
//        require(scale in 0f..1f) { "缩放比例必须在 0~1 之间" }
//
//        val mat1 = loadImageFromBytes(data1).let {
//            if (scale != 1f) resizeMat(it, scale) else it
//        }
//        val mat2 = loadImageFromBytes(data2).let {
//            if (scale != 1f) resizeMat(it, scale) else it
//        }
//
//        return mat1 to mat2
//    }
//
//    /**
//     * 从字节数组加载图片
//     * @throws IllegalArgumentException 图片数据无效或解码失败
//     */
//    private fun loadImageFromBytes(data: ByteArray): Mat {
//        require(data.isNotEmpty()) { "图片数据不能为空" }
//
//        try {
//            val matOfByte = MatOfByte(*data)
//            val mat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR)
//            matOfByte.release()
//
//            require(!mat.empty()) { "无法解码图片数据" }
////            return ensureBgrFormat(mat)
//            return mat
//        } catch (e: Exception) {
//            throw IllegalArgumentException("图片数据解析失败", e)
//        }
//    }
//
////    /**
////     * 确保图片为 BGR 三通道格式
////     */
////    private fun ensureBgrFormat(mat: Mat): Mat {
////        return when (mat.channels()) {
////            1 -> convertGrayToBgr(mat)
////            3 -> mat
////            4 -> removeAlphaChannel(mat)
////            else -> throw IllegalArgumentException("不支持的通道数: ${mat.channels()}")
////        }
////    }
//
//
//    private fun loadImage(file: File): Mat {
//        require(file.exists()) { "文件不存在: ${file.path}" }
//        val mat = Imgcodecs.imread(file.path, Imgcodecs.IMREAD_COLOR)
//        require(!mat.empty()) { "无法解析图片: ${file.path}" }
//        return mat
//    }
//
//    private fun resizeMat(src: Mat, scale: Float): Mat {
//        val size = Size(
//            (src.cols() * scale).toInt().coerceAtLeast(1).toDouble(),
//            (src.rows() * scale).toInt().coerceAtLeast(1).toDouble()
//        )
//        val dst = Mat()
//        Imgproc.resize(src, dst, size, 0.0, 0.0, Imgproc.INTER_AREA)
//        return dst
//    }
//
//    private fun sameSizeAndType(mat1: Mat, mat2: Mat): Boolean {
//        return mat1.size() == mat2.size() && mat1.type() == mat2.type()
//    }
//
//    private fun convertToGray(mat: Mat): Mat {
//        val gray = Mat()
//        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
//        return gray
//    }
//
//    private fun computeSSIM(mat1: Mat, mat2: Mat): Double {
//        val C1 = (0.01 * 255).pow(2)
//        val C2 = (0.03 * 255).pow(2)
//
//        // 1. 计算均值
//        val mu1 = Mat().apply { Imgproc.GaussianBlur(mat1, this, Size(11.0, 11.0), 1.5) }
//        val mu2 = Mat().apply { Imgproc.GaussianBlur(mat2, this, Size(11.0, 11.0), 1.5) }
//
//        // 2. 计算均值平方
//        val mu1Sq = Mat().apply { Core.multiply(mu1, mu1, this) }
//        val mu2Sq = Mat().apply { Core.multiply(mu2, mu2, this) }
//        val mu1mu2 = Mat().apply { Core.multiply(mu1, mu2, this) }
//
//        // 3. 计算图像平方与乘积
//        val mat1Sq = Mat().apply { Core.multiply(mat1, mat1, this) }
//        val mat2Sq = Mat().apply { Core.multiply(mat2, mat2, this) }
//        val mat1Mat2 = Mat().apply { Core.multiply(mat1, mat2, this) }
//
//        // 4. 计算方差和协方差
//        val sigma1Sq = Mat().apply {
//            Imgproc.GaussianBlur(mat1Sq, this, Size(11.0, 11.0), 1.5)
//            Core.subtract(this, mu1Sq, this)
//        }
//        val sigma2Sq = Mat().apply {
//            Imgproc.GaussianBlur(mat2Sq, this, Size(11.0, 11.0), 1.5)
//            Core.subtract(this, mu2Sq, this)
//        }
//        val sigma12 = Mat().apply {
//            Imgproc.GaussianBlur(mat1Mat2, this, Size(11.0, 11.0), 1.5)
//            Core.subtract(this, mu1mu2, this)
//        }
//
//        // 5. 计算 SSIM 分子和分母
//        val numerator = Mat().apply {
//            Core.multiply(
//                mu1mu2.apply { Core.multiply(this, Scalar(2.0), this) }.apply { Core.add(this, Scalar(C1), this) },
//                sigma12.apply { Core.multiply(this, Scalar(2.0), this) }.apply { Core.add(this, Scalar(C2), this) },
//                this
//            )
//        }
//
//        val denominator = Mat().apply {
//            Core.multiply(
//                mu1Sq.apply { Core.add(this, mu2Sq, this) }.apply { Core.add(this, Scalar(C1), this) },
//                sigma1Sq.apply { Core.add(this, sigma2Sq, this) }.apply { Core.add(this, Scalar(C2), this) },
//                this
//            )
//        }
//
//        // 6. 计算 SSIM 映射图
//        val ssimMap = Mat()
//        Core.divide(numerator, denominator, ssimMap)
//
//        // 7. 返回平均值
//        return Core.mean(ssimMap).`val`[0]
//    }
//}

object ImageUtils {
    init {
        OpenCV.loadLocally()
    }
    fun compareHistograms(img1: Mat, img2: Mat): Double {
        // 转换为 HSV 颜色空间（更符合人类视觉）
        val hsv1 = Mat()
        val hsv2 = Mat()
        Imgproc.cvtColor(img1, hsv1, Imgproc.COLOR_BGR2HSV)
        Imgproc.cvtColor(img2, hsv2, Imgproc.COLOR_BGR2HSV)

        // 计算直方图
        val histSize = MatOfInt(50, 60) // H 通道 50 bins，S 通道 60 bins
        val channels = MatOfInt(0, 1)
        val ranges = MatOfFloat(0f, 180f, 0f, 256f) // H 范围 [0,180), S 范围 [0,256)
        val hist1 = Mat()
        val hist2 = Mat()
        Imgproc.calcHist(listOf(hsv1), channels, Mat(), hist1, histSize, ranges)
        Imgproc.calcHist(listOf(hsv2), channels, Mat(), hist2, histSize, ranges)

        // 归一化直方图
        Core.normalize(hist1, hist1, 0.0, 1.0, Core.NORM_MINMAX)
        Core.normalize(hist2, hist2, 0.0, 1.0, Core.NORM_MINMAX)

        // 使用巴氏距离比较直方图（值越小越相似）
        return Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_BHATTACHARYYA)
    }

    fun bytesToMat(imgBytes: ByteArray): Mat {
        val matOfByte = MatOfByte(*imgBytes)
        val mat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR)
        return mat
    }
}