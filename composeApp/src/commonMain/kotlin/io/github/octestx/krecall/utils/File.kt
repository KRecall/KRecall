package io.github.octestx.krecall.utils

import java.io.File

fun File.toKPath() = kotlinx.io.files.Path(path = this.absolutePath)