package io.github.octestx.krecall.utils

import io.github.octestx.basic.multiplatform.common.exceptions.ExceptionSerializer
import io.github.octestx.basic.multiplatform.common.utils.ojson
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val exceptionSerializableOjson = Json(ojson) {
    serializersModule = SerializersModule {
        contextual(Exception::class, ExceptionSerializer)
    }
}