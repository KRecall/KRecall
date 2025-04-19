package io.github.octestx.krecall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import io.github.kotlin.fibonacci.SystemMessage
import io.github.kotlin.fibonacci.utils.OS
import io.github.kotlin.fibonacci.utils.ojson
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.composeapp.generated.resources.icon
import io.github.octestx.krecall.model.InitConfig
import io.klogging.noCoLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.ProvidePreComposeLocals
import org.jetbrains.compose.resources.painterResource
import java.io.File

private val ologger = noCoLogger("Whole Recall for JVM Desktop")
fun main() = application {
    // 创建系统托盘
    val trayState = rememberTrayState()
    var loading by remember { mutableStateOf(true) }
    val initConfigFile = remember {
        File(File(System.getProperty("user.dir")), "KRecall-INIT.json").apply {
            parentFile.mkdirs()
            createNewFile()
        }
    }
    var initConfig by remember {
        mutableStateOf(
            try {
                ojson.decodeFromString<InitConfig>(initConfigFile.readText())
            } catch (e: Throwable) {
                val config = InitConfig()
                val text = ojson.encodeToString(config)
                initConfigFile.writeText(text)
                config
            }
        )
    }
    if (initConfig.dataDirAbsPath == null) {
        Window(onCloseRequest = { exitApplication() }) {
            val scope = rememberCoroutineScope()
            MaterialTheme {
                Column {
                    Text("请先设置数据目录[第一次使用只要输入空文件夹目录.已有数据用户请输入数据目录,不是名称为KRecall的目录，而是它的父目录]！")
                    var path by remember { mutableStateOf("") }
                    TextField(path, { path = it})
                    var showApplyError by remember { mutableStateOf("") }
                    AnimatedVisibility(showApplyError.isNotEmpty()) {
                        Text(text = showApplyError, modifier = Modifier.padding(45.dp).background(MaterialTheme.colorScheme.errorContainer), color = MaterialTheme.colorScheme.error, fontStyle = MaterialTheme.typography.bodyMedium.fontStyle)
                    }
                    Button(onClick = {
                        val workDir = File(path)
                        if (workDir.exists()) {
                            val config = initConfig.copy(dataDirAbsPath = workDir.absolutePath)
                            val text = ojson.encodeToString(config)
                            initConfigFile.writeText(text)
                            initConfig = config
                        } else {
                            scope.launch {
                                showApplyError = "目录无效"
                                delay(3000)
                                showApplyError = ""
                            }
                        }
                    }) {
                        Text("Apply")
                    }
                }
            }
        }
    } else {
        LaunchedEffect(Unit) {
            val workDir = File(initConfig.dataDirAbsPath, "KRecall")
            Core.init(trayState, workDir)
            loading = false
        }
    }
    var windowVisible by remember { mutableStateOf(
        if (OS.currentOS == OS.OperatingSystem.WIN) false
        else true
    ) }//Default is background running.
    if (loading.not()) {
        val appMainPageModel = remember { AppMainPage.AppMainPageModel() }
        val appMainPage = remember { AppMainPage(appMainPageModel) }
        Window(
            visible = windowVisible,
            onCloseRequest = {
                windowVisible = false
            },
            title = "KRecall",
        ) {
            ProvidePreComposeLocals {
                PreComposeApp {
                    appMainPage.Main(Unit)
                }
            }
        }
    }
    Tray(
        icon = painterResource(Res.drawable.icon),
        menu = {
            Item("Show", onClick = { windowVisible = true })
            Separator()
            Item("Exit", onClick = ::exitApplication)
        },
        onAction = {
            windowVisible = windowVisible.not()
        },
        tooltip = "KRecall",
        state = trayState
    )
    LaunchedEffect(loading) {
        if (loading.not()) {
            SystemMessage.sendNotification(
                Notification(
                    "KRecall后台运行",
                    "点击托盘显示主界面"
                )
            )
        }
    }
}