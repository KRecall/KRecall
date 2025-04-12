package io.github.octestx.krecall.plugins.impl.storage

import nu.pattern.OpenCV
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

object ImageUtils {
    init {
        OpenCV.loadLocally()
    }

    fun calculateImageSimilarityCV(img1Bytes: ByteArray, img2Bytes: ByteArray): Double {
        val img1 = Imgcodecs.imdecode(MatOfByte(*img1Bytes), Imgcodecs.IMREAD_COLOR)
        val img2 = Imgcodecs.imdecode(MatOfByte(*img2Bytes), Imgcodecs.IMREAD_COLOR)

        // 尺寸统一化
        val targetSize = Size(256.0, 256.0)
        Imgproc.resize(img1, img1, targetSize)
        Imgproc.resize(img2, img2, targetSize)

        // 灰度化
        val gray1 = Mat()
        val gray2 = Mat()
        Imgproc.cvtColor(img1, gray1, Imgproc.COLOR_BGR2GRAY)
        Imgproc.cvtColor(img2, gray2, Imgproc.COLOR_BGR2GRAY)

        // 直方图对比
        val hist1 = Mat()
        val hist2 = Mat()
        val histSize = MatOfInt(256)
        val ranges = MatOfFloat(0f, 256f)
        Imgproc.calcHist(listOf(gray1), MatOfInt(0), Mat(), hist1, histSize, ranges)
        Imgproc.calcHist(listOf(gray2), MatOfInt(0), Mat(), hist2, histSize, ranges)
        Core.normalize(hist1, hist1)
        Core.normalize(hist2, hist2)

        // 计算相似度（使用相关系数法）
        return Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CORREL).coerceIn(0.0, 1.0)
    }
}


//object ImageUtils {
//    init {
//        //TODO Maybe need user install opencv after use KRecall
//        OpenCV.loadLocally()
//    }
//    // 主要修改点一：增强预处理和颜色空间处理
//    fun calculateImageSimilarityCV(img1Bytes: ByteArray, img2Bytes: ByteArray): Double {
//        val img1 = Imgcodecs.imdecode(MatOfByte(*img1Bytes), Imgcodecs.IMREAD_COLOR).apply {
//            // 增强对比度
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_BGR2YUV)
//            Core.normalize(this, this, 50.0, 250.0, Core.NORM_MINMAX)
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_YUV2BGR)
//        }
//
//        val img2 = Imgcodecs.imdecode(MatOfByte(*img2Bytes), Imgcodecs.IMREAD_COLOR).apply {
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_BGR2YUV)
//            Core.normalize(this, this, 50.0, 250.0, Core.NORM_MINMAX)
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_YUV2BGR)
//        }
//
//        // 调整预处理参数
//        val targetSize = Size(1024.0, 1024.0)
//        Imgproc.resize(img1, img1, targetSize, 0.0, 0.0, Imgproc.INTER_CUBIC) // 改用立方插值
//        Imgproc.resize(img2, img2, targetSize, 0.0, 0.0, Imgproc.INTER_CUBIC)
//
//        // 混合相似度计算方法（新增边缘相似度）
//        val edgeSimilarity = calculateEdgeSimilarity(img1, img2)
//        val ssim = calculateMS_SSIM(img1, img2) // 改为多尺度SSIM
//        val featureSimilarity = calculateImprovedFeatureSimilarity(img1, img2)
//
//        // 调整权重分配
//        return (edgeSimilarity * 0.3) + (ssim * 0.5) + (featureSimilarity * 0.2)
//    }
//
//    // 主要修改点二：多尺度SSIM实现
//    private fun calculateMS_SSIM(img1: Mat, img2: Mat): Double {
//        val scales = listOf(1.0, 0.5, 0.25)
//        var finalSSIM = 0.0
//
//        scales.forEach { scale ->
//            val resizedImg1 = Mat()
//            val resizedImg2 = Mat()
//            Imgproc.resize(img1, resizedImg1, Size(img1.width()*scale, img1.height()*scale))
//            Imgproc.resize(img2, resizedImg2, Size(img2.width()*scale, img2.height()*scale))
//
//            finalSSIM += calculateSingleScaleSSIM(resizedImg1, resizedImg2) * when(scale) {
//                1.0 -> 0.5
//                0.5 -> 0.3
//                else -> 0.2
//            }
//        }
//
//        return finalSSIM.coerceIn(0.0..1.0)
//    }
//
//    // 主要修改点三：改进的特征匹配算法
//    private fun calculateImprovedFeatureSimilarity(img1: Mat, img2: Mat): Double {
//        val detector = AKAZE.create()
//        val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
//
//        val (keypoints1, descriptors1) = detectFeatures(img1, detector)
//        val (keypoints2, descriptors2) = detectFeatures(img2, detector)
//
//        if (descriptors1.empty() || descriptors2.empty()) return 0.0
//
//        // 双向匹配
//        val matches1to2 = MatOfDMatch()
//        val matches2to1 = MatOfDMatch()
//        matcher.match(descriptors1, descriptors2, matches1to2)
//        matcher.match(descriptors2, descriptors1, matches2to1)
//
//        // 交叉验证
//        val goodMatches = matches1to2.toList().filter { m1 ->
//            matches2to1.toList().any { m2 -> m1.queryIdx == m2.trainIdx && m1.trainIdx == m2.queryIdx }
//        }
//
//        // 几何一致性验证
//        val srcPoints = goodMatches.map { keypoints1[it.queryIdx].pt }
//        val dstPoints = goodMatches.map { keypoints2[it.trainIdx].pt }
//        val homography = Calib3d.findHomography(MatOfPoint2f(*srcPoints.toTypedArray()),
//            MatOfPoint2f(*dstPoints.toTypedArray()),
//            Calib3d.RANSAC, 5.0)
//
//        val inliers = homography?.let { h ->
//            srcPoints.zip(dstPoints).count { (src, dst) ->
//                // 修改后
//                val srcPointMat = MatOfPoint2f(src)
//                val predictedPoints = MatOfPoint2f()
//                Core.perspectiveTransform(srcPointMat, predictedPoints, h)
//                val predicted = predictedPoints.toArray()[0]
//                abs(predicted.x - dst.x) < 5 && abs(predicted.y - dst.y) < 5
//            }
//        } ?: 0
//
//        val denominator = max(keypoints1.size, keypoints2.size).coerceAtLeast(1)
//        return (inliers.toDouble() / denominator).coerceIn(0.0..1.0)
//    }
//
//    // 新增边缘相似度计算
//    private fun calculateEdgeSimilarity(img1: Mat, img2: Mat): Double {
//        fun getEdgeMap(src: Mat): Mat {
//            val gray = Mat()
//            val edges = Mat()
//            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
//            Imgproc.Canny(gray, edges, 50.0, 150.0)
//            return edges
//        }
//
//        val edges1 = getEdgeMap(img1)
//        val edges2 = getEdgeMap(img2)
//
//        val andResult = Mat()
//        Core.bitwise_and(edges1, edges2, andResult)
//
//        val unionResult = Mat()
//        Core.bitwise_or(edges1, edges2, unionResult)
//
//        val intersection = Core.countNonZero(andResult).toDouble()
//        val union = Core.countNonZero(unionResult).toDouble()
//
//        return if (union == 0.0) 0.0 else intersection / union
//    }
//    // 特征检测统一方法
//    private fun detectFeatures(img: Mat, detector: AKAZE): Pair<List<KeyPoint>, Mat> {
//        val keypoints = MatOfKeyPoint()
//        val descriptors = Mat()
//        detector.detectAndCompute(img, Mat(), keypoints, descriptors)
//        return Pair(keypoints.toList(), descriptors)
//    }
//
//    // 单尺度SSIM实现（基于之前修复的SSIM计算）
//    private fun calculateSingleScaleSSIM(img1: Mat, img2: Mat): Double {
//        // 转换为灰度
//        val gray1 = Mat()
//        val gray2 = Mat()
//        Imgproc.cvtColor(img1, gray1, Imgproc.COLOR_BGR2GRAY)
//        Imgproc.cvtColor(img2, gray2, Imgproc.COLOR_BGR2GRAY)
//        gray1.convertTo(gray1, CvType.CV_32F)
//        gray2.convertTo(gray2, CvType.CV_32F)
//
//        // 计算均值
//        val mu1 = Mat()
//        val mu2 = Mat()
//        Imgproc.GaussianBlur(gray1, mu1, Size(11.0, 11.0), 1.5)
//        Imgproc.GaussianBlur(gray2, mu2, Size(11.0, 11.0), 1.5)
//
//        // 计算方差和协方差
//        val mu1_sq = Mat()
//        Core.multiply(mu1, mu1, mu1_sq)
//        val mu2_sq = Mat()
//        Core.multiply(mu2, mu2, mu2_sq)
//        val mu1_mu2 = Mat()
//        Core.multiply(mu1, mu2, mu1_mu2)
//
//        val sigma1_sq = Mat()
//        val sq1Src = Mat()
//        Core.multiply(gray1, gray1, sq1Src)
//        Imgproc.GaussianBlur(sq1Src, sigma1_sq, Size(11.0, 11.0), 1.5)
//        Core.subtract(sigma1_sq, mu1_sq, sigma1_sq)
//
//        val sigma2_sq = Mat()
//        val sq2Src = Mat()
//        Core.multiply(gray2, gray2, sq2Src)
//        Imgproc.GaussianBlur(sq2Src, sigma2_sq, Size(11.0, 11.0), 1.5)
//        Core.subtract(sigma2_sq, mu2_sq, sigma2_sq)
//
//        val sigma12 = Mat()
//        val sigma12Src = Mat()
//        Core.multiply(gray1, gray2, sigma12Src)
//        Imgproc.GaussianBlur(sigma12Src, sigma12, Size(11.0, 11.0), 1.5)
//        Core.subtract(sigma12, mu1_mu2, sigma12)
//
//        // SSIM公式计算
//        val (c1, c2) = 6.5025 to 58.5225
//        val ssimMap = Mat()
//        Core.multiply(sigma12, Scalar(2.0), sigma12)
//        Core.add(sigma12, Scalar(c2), sigma12)
//
//        val temp1 = Mat()
//        Core.add(mu1_sq, mu2_sq, temp1)
//        Core.add(temp1, Scalar(c1), temp1)
//
//        val temp2 = Mat()
//        Core.add(sigma1_sq, sigma2_sq, temp2)
//        Core.add(temp2, Scalar(c2), temp2)
//
//        Core.multiply(temp1, temp2, temp1)
//        Core.divide(sigma12, temp1, ssimMap)
//
//        return Core.mean(ssimMap).`val`[0].coerceIn(0.0..1.0)
//    }
//
//}