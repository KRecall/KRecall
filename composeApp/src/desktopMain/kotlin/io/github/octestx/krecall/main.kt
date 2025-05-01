package io.github.octestx.krecall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import io.github.octestx.basic.multiplatform.common.utils.OS
import io.github.octestx.basic.multiplatform.common.utils.ojson
import io.github.octestx.basic.multiplatform.ui.SystemMessage
import io.github.octestx.basic.multiplatform.ui.ui.global.Language
import io.github.octestx.basic.multiplatform.ui.ui.global.LanguageRepository
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.composeapp.generated.resources.icon
import io.github.octestx.krecall.model.InitConfig
import io.github.octestx.krecall.ui.callExtremeErrorWindow
import io.github.octestx.krecall.ui.extremeErrorWindowInject
import io.klogging.noCoLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.ProvidePreComposeLocals
import org.jetbrains.compose.resources.painterResource
import java.io.File

private val ologger = noCoLogger("Whole Recall for JVM Desktop")
fun main() = application {
    extremeErrorWindowInject()
    // 创建系统托盘
    val trayState = rememberTrayState()
    var step by remember { mutableStateOf(0) }
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
    //Other default is background running.
    var windowVisible by remember { mutableStateOf(
        OS.currentOS == OS.OperatingSystem.LINUX
    ) }
    LaunchedEffect(Unit) {
        if (initConfig.dataDirAbsPath == null) {
            step = 1
        } else {
            step = 4
        }
    }
    Window(
        visible = windowVisible,
        onCloseRequest = {
            windowVisible = false
        },
        title = "KRecall"
    ) {
        when (step) {
            0 -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            1 -> {
                SelectLanguage() { step = 2 }
            }
            2 -> {
                ViewKRecallUpdatedInfo()
            }
            3 -> {
                InitConfig(initConfig, { initConfig = it }, initConfigFile, trayState, { step = 4 })
            }
            4 -> {
                var showProgress by remember { mutableStateOf(true) }
                AnimatedVisibility(visible = showProgress) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                LaunchedEffect(Unit) {
                    val workDir = File(initConfig.dataDirAbsPath, "KRecall")
                    val result = Core.init(trayState, workDir)
                    result.onFailure {
                        showProgress = false
                        callExtremeErrorWindow(it)
                    }
                    result.onSuccess { step = 5 }
                }
            }
            5 -> {
                val appMainPageModel = remember { AppMainPage.AppMainPageModel() }
                val appMainPage = remember { AppMainPage(appMainPageModel) }
                SystemMessage.sendNotification(
                    Notification(
                        "KRecall后台运行",
                        "点击托盘显示主界面"
                    )
                )
                ProvidePreComposeLocals {
                    PreComposeApp {
                        appMainPage.Main(Unit)
                    }
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
}
@Composable
private fun SelectLanguage(nextStep: () -> Unit) {
    Column {
        Text("请选择语言")
        Button(onClick = {
            LanguageRepository.switchLanguage(Language.English)
            nextStep()
        }) {
            Text("English")
        }
        Button(onClick = {
            LanguageRepository.switchLanguage(Language.ChineseSimplified)
            nextStep()
        }) {
            Text("简体中文")
        }
    }
}
@Composable
private fun InitConfig(
    initConfig: InitConfig,
    changeInitConfig: (InitConfig) -> Unit,
    initConfigFile: File,
    trayState: TrayState,
    nextStep: () -> Unit
) {
    if (initConfig.dataDirAbsPath == null) {
        val scope = rememberCoroutineScope()
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
                    changeInitConfig(config)
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
    } else {
        LaunchedEffect(Unit) {
            nextStep()
        }
    }
}