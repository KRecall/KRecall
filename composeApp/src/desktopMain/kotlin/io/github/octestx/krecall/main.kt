package io.github.octestx.krecall

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.*
import io.github.octestx.basic.multiplatform.common.utils.OS
import io.github.octestx.basic.multiplatform.ui.SystemMessage
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.composeapp.generated.resources.icon
import io.github.octestx.krecall.ui.extremeErrorWindowInject
import io.klogging.noCoLogger
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.ProvidePreComposeLocals
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private val ologger = noCoLogger("Whole Recall for JVM Desktop")
@OptIn(ExperimentalResourceApi::class)
fun main() = application {
    extremeErrorWindowInject()
    // 创建系统托盘
    val trayState = rememberTrayState()

    //Other default is background running.
    var windowVisible by remember { mutableStateOf(
        OS.currentOS == OS.OperatingSystem.LINUX
    ) }
    Window(
        visible = windowVisible,
        onCloseRequest = {
            windowVisible = false
        },
        title = "KRecall"
    ) {
        val appMainPageModel = remember { AppMainPage.AppMainPageModel(trayState) }
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