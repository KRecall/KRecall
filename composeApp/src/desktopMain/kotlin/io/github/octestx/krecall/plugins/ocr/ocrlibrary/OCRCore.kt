package io.github.octestx.krecall.plugins.ocr.ocrlibrary

import com.benjaminwan.ocrlibrary.OcrEngine
import com.benjaminwan.ocrlibrary.OcrResult
import io.klogging.noCoLogger
import java.io.File

class OCRCore(libPath: String, modelsDir: File) {
    private val ologger = noCoLogger<OCRCore>()
    var isConsole = false
    var isPartImg = true
    var isResultImg = true
    //------- init models/dir image/path -------
    private val detName = "det_infer.onnx"
    private val clsName = "cls_infer.onnx"
    private val recName = "rec_infer.onnx"
    private val keysName = "ppocr_keys.txt"

    lateinit var ocrEngine: OcrEngine
    init {
        val jniLibDir = System.getProperty("java.library.path")
        println("java.library.path=$jniLibDir")
        println("modelsDir=$modelsDir, detName=$detName, clsName=$clsName, recName=$recName, keysName=$keysName")

        //------- get jni version -------
        ocrEngine = OcrEngine(libPath)
        val version = ocrEngine.getVersion()
        println("version=$version")

        //------- init Logger -------
        ocrEngine.initLogger(
            isConsole = isConsole,//jni启用命令行输出
            isPartImg = isPartImg,
            isResultImg = isResultImg
        )
//TODO ?????????
//        ocrEngine.enableResultText(imagePath)
        //------- init Models -------
        val initModelsRet = ocrEngine.initModels(modelsDir.absolutePath, detName, clsName, recName, keysName)
        if (!initModelsRet) {
            ologger.error { "Error in models initialization, please check the models/keys path![modelsDir=$modelsDir, detName=$detName, clsName=$clsName, recName=$recName, keysName=$keysName]" }
            throw IllegalStateException("models initialization")
        }

//        //------- set param -------
//        println("padding($padding) boxScoreThresh($boxScoreThresh) boxThresh($boxThresh) unClipRatio($unClipRatio) doAngle($doAngle) mostAngle($mostAngle)")
//        ocrEngine.padding = padding //图像外接白框，用于提升识别率，文字框没有正确框住所有文字时，增加此值。
//        ocrEngine.boxScoreThresh = boxScoreThresh //文字框置信度门限，文字框没有正确框住所有文字时，减小此值
//        ocrEngine.boxThresh = boxThresh //请自行试验
//        ocrEngine.unClipRatio = unClipRatio //单个文字框大小倍率，越大时单个文字框越大
//        ocrEngine.doAngle = doAngle //启用(1)/禁用(0) 文字方向检测，只有图片倒置的情况下(旋转90~270度的图片)，才需要启用文字方向检测
//        ocrEngine.mostAngle = mostAngle //启用(1)/禁用(0) 角度投票(整张图片以最大可能文字方向来识别)，当禁用文字方向检测时，此项也不起作用
    }

    fun detect(
        image: File,
        maxSideLen: Int = OcrEngine.maxSideLen,
        padding: Int = OcrEngine.padding,
        boxScoreThresh: Float = OcrEngine.boxScoreThresh,
        boxThresh: Float = OcrEngine.boxThresh,
        unClipRatio: Float = OcrEngine.unClipRatio,
        doAngle: Boolean = OcrEngine.doAngle,
        mostAngle: Boolean = OcrEngine.mostAngle,
    ): OcrResult {
        //------- start detect -------
//        val ocrResult =
//            ocrEngine.detect(imagePath, maxSideLen = maxSideLen) //按图像长边进行总体缩放，放大增加识别耗时但精度更高，缩小减小耗时但精度降低，maxSideLen=0代表不缩放
        //使用native方法，可以让OcrEngine成为单例
        val ocrResult =
            ocrEngine.detect(image.absolutePath, padding, maxSideLen, boxScoreThresh, boxThresh, unClipRatio, doAngle, mostAngle)
        //------- print result -------
        return ocrResult
    }
}