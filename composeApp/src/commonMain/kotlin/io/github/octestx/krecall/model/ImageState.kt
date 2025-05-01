package io.github.octestx.krecall.model

import androidx.compose.runtime.Immutable
import io.github.octestx.krecall.model.ImageState.*

/**
 * Represents the states of image loading process.
 *
 * @property Loading The image is being loaded (in progress state)
 * @property Success The image has loaded successfully, requires passing the image [ByteArray]
 * @property Error The image failed to load, requires passing the [Throwable] exception
 */
@Immutable
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

    data class Error(val cause: Throwable) : ImageState()
}