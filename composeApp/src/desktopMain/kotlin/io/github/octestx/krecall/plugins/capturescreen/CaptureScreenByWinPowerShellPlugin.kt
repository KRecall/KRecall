package io.github.octestx.krecall.plugins.capturescreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import io.github.octestx.basic.multiplatform.common.utils.OS
import io.github.octestx.krecall.plugins.basic.AbsCaptureScreenPlugin
import io.github.octestx.krecall.plugins.basic.PluginContext
import io.github.octestx.krecall.plugins.basic.PluginMetadata
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
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import javax.imageio.ImageIO

class CaptureScreenByWinPowerShellPlugin(metadata: PluginMetadata): AbsCaptureScreenPlugin(metadata) {
    companion object {
        val metadata = PluginMetadata(
            pluginId = "CaptureScreenByWinPowerShellPlugin",
            supportPlatform = setOf(OS.OperatingSystem.WIN, OS.OperatingSystem.MACOS),
            supportUI = true,
            mainClass = "io.github.octestx.krecall.plugins.capturescreen.CaptureScreenByWinPowerShellPlugin"
        )
    }
    private val ologger = noCoLogger<CaptureScreenByWinPowerShellPlugin>()
    override suspend fun supportOutputToStream(): Boolean = true

    override suspend fun getScreen(outputStream: OutputStream): WindowInfo = withContext(Dispatchers.IO) {
        val capture = captureScreen()
        ImageIO.write(capture, "png", outputStream)
        getCurrentWindowInfo()
    }

    private lateinit var robot: Robot

    override suspend fun getScreen(outputFileBitItNotExits: File): WindowInfo = withContext(Dispatchers.IO) {
        val capture = captureScreen()
        ImageIO.write(capture, "png", outputFileBitItNotExits)
        getCurrentWindowInfo()
    }

    private suspend fun captureScreen(): BufferedImage = withContext(Dispatchers.IO) {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val rect = java.awt.Rectangle(screenSize)
        robot.createScreenCapture(rect)
    }

    private fun getCurrentWindowInfo(): WindowInfo {
        return WindowInfo(1, "占位Id", "占位Title")
        // 执行 PowerShell 获取活动窗口的进程 ID 和标题
        val STR = "$"
        val command = """
Add-Type @"
using System;
using System.Text;
using System.Runtime.InteropServices;

public class User32 {
    [DllImport("user32.dll")]
    public static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int count);

    [DllImport("user32.dll")]
    public static extern int GetWindowThreadProcessId(IntPtr hWnd, out int processId);
}
"@

${STR}hwnd = [User32]::GetForegroundWindow()

# 获取进程ID
${STR}processId = 0
[User32]::GetWindowThreadProcessId(${STR}hwnd, [ref] ${STR}processId) | Out-Null

# 获取窗口标题
${STR}sb = New-Object System.Text.StringBuilder 256
${STR}length = [User32]::GetWindowText(${STR}hwnd, ${STR}sb, ${STR}sb.Capacity)
${STR}title = if (${STR}length -gt 0) { ${STR}sb.ToString() } else { "Unknown" }

# 输出结果
"${STR}processId|${STR}title"
    """.trimIndent()

        val process = ProcessBuilder("powershell.exe", "-Command", command).start()
        val output = process.inputStream.bufferedReader().use {
            it.readText()
        }

        return output.split("|").let {
            WindowInfo(1, appId = it[0], windowTitle = it[1])
        }
    }

    override fun load() {
        ologger.info { "Loaded" }
    }

    override fun selected() {
        robot = Robot()
    }
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
        val data = getScreen(f)
        if (!f.exists()) {
            throw FileNotFoundException("testFile not found")
        }
        ologger.info("Test: ${f.absolutePath} [$data]")
        return f
    }


    override suspend fun tryInitInner(context: PluginContext): InitResult {
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