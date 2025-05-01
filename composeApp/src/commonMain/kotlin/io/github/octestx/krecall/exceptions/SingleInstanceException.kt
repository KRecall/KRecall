package io.github.octestx.krecall.exceptions

/**
 * Common unsaved configuration exception type.
 * When detecting unsaved configuration changes,
 * should display user alert via sendExceptionToast
 * and log this exception through io.klogging library.
 */
class SingleInstanceException: Exception("Configuration not saved")