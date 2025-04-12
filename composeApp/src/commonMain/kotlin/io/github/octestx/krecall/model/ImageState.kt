package io.github.octestx.krecall.model

sealed class ImageState {
    data object Loading : ImageState()
    data class Success(val bytes: ByteArray) : ImageState() {
        override fun equals(other: Any?): Boolean {
            return this === other
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }

    data object Error : ImageState()
}