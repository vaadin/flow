package com.vaadin.gradle.worker

import org.gradle.api.logging.Logger

internal class LoggerAdapter(private val logger: Logger) {
    fun logDebug(p0: CharSequence?) {
        logger.debug(asMessage(p0))
    }

    fun logDebug(p0: CharSequence?, p1: Throwable?) {
        logger.debug(asMessage(p0), asThrowable(p1))
    }

    fun logInfo(p0: CharSequence?) {
        logger.info(asMessage(p0))
    }

    fun logWarn(p0: CharSequence?) {
        logger.warn(asMessage(p0))
    }

    fun logWarn(p0: CharSequence?, p1: Throwable?) {
        logger.warn(asMessage(p0), asThrowable(p1))
    }

    fun logError(p0: CharSequence?) {
        logger.error(asMessage(p0))
    }

    fun logError(p0: CharSequence?, p1: Throwable?) {
        logger.error(asMessage(p0), asThrowable(p1))
    }

    private fun asMessage(p0: CharSequence?): String {
        return p0!!.toString()
    }

    private fun asThrowable(p1: Throwable?): Throwable {
        return p1!!
    }
}
