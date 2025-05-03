package io.github.octestx.krecall.exception

import io.github.octestx.krecall.plugins.basic.PluginBasicExt
import kotlin.reflect.KClass

class InvalidKeyPluginException(errorMessage: String, vararg pluginsStatus: Pair<KClass<out PluginBasicExt>, Collection<PluginBasicExt>>)
    : Exception(
        buildString {
            appendLine("InvalidKeyPluginException:")
            for (pluginStatus in pluginsStatus) {
                val (plugin, candidates) = pluginStatus
                append("${plugin::class.qualifiedName}: ")
                if (candidates.isEmpty()) {
                    appendLine("There are no candidates!")
                } else {
                    appendLine(candidates)
                }
            }
            appendLine("<---Message--->")
            append(errorMessage)
        }
    )