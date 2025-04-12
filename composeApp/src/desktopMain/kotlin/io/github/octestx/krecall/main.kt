package io.github.octestx.krecall

import androidx.compose.runtime.*
import androidx.compose.ui.window.*
import io.github.kotlin.fibonacci.SystemMessage
import io.github.kotlin.fibonacci.utils.OS
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.composeapp.generated.resources.icon
import io.klogging.NoCoLogger
import io.klogging.noCoLogger
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.ProvidePreComposeLocals
import org.jetbrains.compose.resources.painterResource

private lateinit var ologger: NoCoLogger
fun main() = application {
    // 创建系统托盘
    val trayState = rememberTrayState()
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        Core.init(trayState)
        ologger = noCoLogger("Whole Recall")
        loading = false
    }
    var windowVisible by remember { mutableStateOf(
        if (OS.getCurrentOS() == OS.OperatingSystem.WIN) false
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