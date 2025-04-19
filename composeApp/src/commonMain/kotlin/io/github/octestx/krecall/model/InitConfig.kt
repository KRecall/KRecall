package io.github.octestx.krecall.model

import kotlinx.serialization.Serializable

@Serializable
data class InitConfig(
    val dataDirAbsPath: String? = null
)
