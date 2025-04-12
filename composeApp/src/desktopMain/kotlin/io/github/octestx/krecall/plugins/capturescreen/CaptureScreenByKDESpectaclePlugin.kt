package io.github.octestx.krecall.plugins.capturescreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import io.github.kotlin.fibonacci.utils.OS
import io.github.octestx.krecall.plugins.basic.AbsCaptureScreenPlugin
import io.github.octestx.krecall.plugins.basic.WindowInfo
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream

class CaptureScreenByKDESpectaclePlugin: AbsCaptureScreenPlugin(pluginId = "CaptureScreenByKDESpectaclePlugin") {
    override val supportPlatform: Set<OS.OperatingSystem> = setOf(OS.OperatingSystem.LINUX)
    override val supportUI: Boolean = true
    private val ologger = noCoLogger<CaptureScreenByKDESpectaclePlugin>()
    override suspend fun supportOutputToStream(): Boolean = false

    override suspend fun getScreen(outputStream: OutputStream): WindowInfo {
        throw UnsupportedOperationException()
    }

    override suspend fun getScreen(outputFileBitItNotExits: File): WindowInfo {
        withContext(Dispatchers.IO) {
            ologger.info { "getScreen: $outputFileBitItNotExits" }
            val processBuilder = ProcessBuilder("/usr/bin/spectacle", "-f", "-b", "-n", "-o", outputFileBitItNotExits.absolutePath)
            processBuilder.redirectErrorStream(false)
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val err = RuntimeException("Command[${processBuilder.command()}] failed with exit code $exitCode")
                ologger.error(err) { "Exception: "+err.message }
                throw err
            }
        }
        return WindowInfo(
            0,
            "DEFAULT__Spectacle",
            "DEFAULT__Spectacle",
        )
    }

    override fun load() {
        ologger.info { "Loaded" }
    }

    override fun selected() {}
    override fun unselected() {}

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        var painter: Painter? by remember { mutableStateOf(null) }
        Column {
            Button(onClick = {
                scope.launch {
                    val f = test()
                    val img = f.inputStream().readAllBytes().decodeToImageBitmap()
                    painter = BitmapPainter(img)
                    ologger.info("Test: ${f.absolutePath}")
                }
            }) {
                Text("Test")
            }
            painter?.let { Image(it, contentDescription = null) }
        }
    }

    private suspend fun test(): File {
        val f = File(pluginDir, "test.png")
        getScreen(f)
        if (!f.exists()) {
            throw FileNotFoundException("testFile not found")
        }
        ologger.info("Test: ${f.absolutePath}")
        return f
    }


    override suspend fun tryInitInner(): InitResult {
        ologger.info { "TryInit" }
        val e = runBlocking {
            try {
                test()
                null
            } catch (e: Exception) {
                e
            }
        }
        if (e == null) {
            _initialized.value = true
            return InitResult.Success
        } else {
            return InitResult.Failed(e)
        }
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized
}