package io.github.octestx.krecall.plugins

import io.github.octestx.basic.multiplatform.common.utils.OS
import io.github.octestx.basic.multiplatform.common.utils.ojson
import io.github.octestx.krecall.plugins.basic.PluginBasic
import io.github.octestx.krecall.plugins.basic.PluginMetadata
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

object JarPluginManager {
    private val ologger = noCoLogger<JarPluginManager>()
    private val _plugins = mutableMapOf<String, PluginContainer>()
    val plugins: Map<String, PluginContainer> = _plugins
    private val classLoaders = mutableMapOf<File, ClassLoader>()

    data class PluginContainer(
        val metadata: PluginMetadata,
        val classLoader: ClassLoader,
        val instanceCreator: () -> PluginBasic
    )

    suspend fun loadPluginsFromDir(jarDir: File) {
        ologger.info { "Load jar dir: $jarDir" }
        jarDir.takeIf { it.exists() }?.walk()
            ?.filter { it.extension == "jar" }
            ?.forEach { loadJar(it) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadJar(jarFile: File) = withContext(Dispatchers.IO) {
        ologger.info { "loadJar: $jarFile" }
        try {
            val classLoader = getOrCreateClassLoader(jarFile)
            JarFile(jarFile).use { jar ->
                jar.entries().toList()
                    .filter { it.name.startsWith("META-INF/plugins/") && it.name.endsWith(".json") }
                    .forEach { entry ->
                        try {
                            val metadata: PluginMetadata = jar.getInputStream(entry).use { it ->
                                val text = it.readBytes().decodeToString()
                                ologger.info { text }
                                val element = ojson.parseToJsonElement(text)
                                PluginMetadata(
                                    element.jsonObject["plugin_id"]!!.jsonPrimitive.content,
                                    element.jsonObject["support_platform"]!!.jsonArray.map {
                                        when (it.jsonPrimitive.content) {
                                            "WIN" -> OS.OperatingSystem.WIN
                                            "LINUX" -> OS.OperatingSystem.LINUX
                                            "MACOS" -> OS.OperatingSystem.MACOS
                                            else -> OS.OperatingSystem.OTHER
                                        }
                                    }.toSet(),
                                    element.jsonObject["support_ui"]!!.jsonPrimitive.boolean,
                                    element.jsonObject["main_class"]!!.jsonPrimitive.content
                                )
//                                ojson.decodeFromStream<PluginMetadata>(it)
                            }
                            ologger.info { "found metadata: $metadata from $jarFile" }
                            processPlugin(metadata, jarFile, classLoader)
                        } catch (e: Exception) {
                            ologger.error(e) {
                                "Failed to load plugin from ${entry.name}: ${e.message}"
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            ologger.error("Failed to load JAR ${jarFile.name}: ${e.message}")
        }
    }

    private fun getOrCreateClassLoader(jarFile: File): ClassLoader {
        return classLoaders.getOrPut(jarFile) {
            URLClassLoader(
                arrayOf(jarFile.toURI().toURL()),
                this::class.java.classLoader
            )
        }
    }

    private suspend fun processPlugin(
        metadata: PluginMetadata,
        jarFile: File,
        classLoader: ClassLoader,
    ) {
        if (OS.currentOS !in metadata.supportPlatform) {
            ologger.info("Skipping plugin ${metadata.pluginId} (incompatible platform)")
            return
        }

        if (_plugins.containsKey(metadata.pluginId)) {
            ologger.warn { "Plugin ${metadata.pluginId} already loaded" }
            return
        }

        try {
            _plugins[metadata.pluginId] = PluginContainer(
                metadata = metadata,
                classLoader = classLoader,
                instanceCreator = {
                    val instance = classLoader.loadClass(metadata.mainClass)
                        .declaredConstructors.first { it.parameterTypes.contentEquals(arrayOf(PluginMetadata::class.java)) }
                        .apply { isAccessible = true }
                        .newInstance(metadata) as PluginBasic
                    instance
                }
            )
            ologger.info("Successfully loaded plugin: ${metadata.pluginId}")
        } catch (e: Exception) {
            ologger.error(e) {
                "Failed to instantiate plugin ${metadata.pluginId}: ${e.message}"
            }
        }
    }

//    fun unloadPlugin(pluginId: String) {
//        plugins[pluginId]?.let {
//            try {
//                it.instance.unselected()
//                it.instance.unload()
//                logger.info("Unloaded plugin: $pluginId")
//            } catch (e: Exception) {
//                logger.error("Error unloading plugin $pluginId: ${e.message}")
//            }
//            plugins.remove(pluginId)
//        }
//    }

//    fun getPlugin(pluginId: String): PluginBasic? = plugins[pluginId]?.instance
}