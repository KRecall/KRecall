package com.benjaminwan.ocrlibrary

class OcrEngine(libPath: String) {
    init {
        try {
//            System.loadLibrary("RapidOcrOnnx")
            System.load(libPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object{
        var padding: Int = 50
        var boxScoreThresh: Float = 0.5f
        var boxThresh: Float = 0.3f
        var unClipRatio: Float = 1.6f
        var doAngle: Boolean = false
        var mostAngle: Boolean = false
        var maxSideLen = 1024
    }

    fun detect(input: String, maxSideLen: Int) = detect(
        input, padding, maxSideLen,
        boxScoreThresh, boxThresh,
        unClipRatio, doAngle, mostAngle
    )

    external fun setNumThread(numThread: Int): Boolean

    external fun initLogger(
        isConsole: Boolean,
        isPartImg: Boolean,
        isResultImg: Boolean
    )

    external fun enableResultText(imagePath: String)

    external fun initModels(
        modelsDir: String,
        detName: String,
        clsName: String,
        recName: String,
        keysName: String
    ): Boolean

    external fun getVersion(): String

    external fun detect(
        input: String, padding: Int, maxSideLen: Int,
        boxScoreThresh: Float, boxThresh: Float,
        unClipRatio: Float, doAngle: Boolean, mostAngle: Boolean
    ): OcrResult

}